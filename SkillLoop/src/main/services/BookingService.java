package skillloop.services;

import java.util.List;

import skillloop.collections.Repository;
import skillloop.exceptions.InsufficientCreditsException;
import skillloop.exceptions.SessionUnavailableException;
import skillloop.sessions.Session;
import skillloop.sessions.SessionStatus;
import skillloop.users.Student;

/**
 * Handles everything about booking a seat in a session:
 * credit check, seat reservation, waiting list, cancellation and completion.
 *
 * DEPENDENCY INJECTION:
 *   BookingService receives the session repository and the notification service
 *   through its constructor. It never writes "new ConsoleNotificationService()",
 *   so the tests can hand it a mock instead.
 *
 * This class is also where most of the EXCEPTION HANDLING of the project lives.
 */
public class BookingService {

    private final Repository<Session> sessionRepository;
    private final NotificationService notificationService;

    /** Constructor injection of TWO dependencies. */
    public BookingService(Repository<Session> sessionRepository,
                          NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
    }

    /**
     * Book one seat.
     *
     * "throws" lists the two things that can legitimately go wrong, so every
     * caller is forced by the compiler to handle them.
     *
     * @throws SessionUnavailableException  session cancelled/completed, own session,
     *                                      already booked, or no free seat
     * @throws InsufficientCreditsException wallet does not have enough credits
     */
    public void bookSession(Student student, Session session)
            throws SessionUnavailableException, InsufficientCreditsException {

        // --- rule 1: the session must still be open -------------------------
        if (session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.EXPIRED) {
            throw new SessionUnavailableException(session.getSessionId(),
                    "its status is " + session.getStatus());
        }

        // --- rule 2: you cannot learn from yourself --------------------------
        if (session.getTeacher().getId() == student.getId()) {
            throw new SessionUnavailableException(session.getSessionId(),
                    "you are the teacher of this session");
        }

        // --- rule 3: no double booking ---------------------------------------
        if (session.hasStudent(student)) {
            throw new SessionUnavailableException(session.getSessionId(),
                    "you already hold a seat in this session");
        }

        // --- rule 4: enough credits ------------------------------------------
        int cost = session.getCost();
        if (!student.canAfford(cost)) {
            throw new InsufficientCreditsException(cost, student.getWallet().getBalance());
        }

        // --- rule 5: a free seat (synchronized inside Session) ----------------
        boolean gotSeat = session.reserveSeat(student);
        if (!gotSeat) {
            throw new SessionUnavailableException(session.getSessionId(), "all seats are taken");
        }

        // --- payment -----------------------------------------------------
        try {
            student.getWallet().spend(cost, "Booked " + session.getSkill().getName()
                    + " session " + session.getSessionId());
        } catch (InsufficientCreditsException e) {
            // Another thread may have emptied the wallet between rule 4 and here.
            // Undo the seat, then RETHROW so the caller still learns the truth.
            session.cancelSeat(student);
            throw e;                               // RETHROWING an exception
        }

        student.addBookedSession(session.getSessionId());
        notificationService.notify(student, "Seat confirmed for " + session.getSessionId()
                + " (" + session.getSkill().getName() + "), " + cost + " credits paid.");
        notificationService.notify(session.getTeacher(),
                student.getName() + " booked your " + session.getSkill().getName() + " session.");
    }

    /**
     * The version the UI actually calls: it books, and if the session is full it
     * puts the student on the waiting list instead.
     *
     * This method shows the complete try / catch / catch / finally structure.
     *
     * @return a message that can be printed straight to the screen
     */
    public String bookOrJoinWaitingList(Student student, Session session) {
        String outcome;
        try {
            bookSession(student, session);
            outcome = "BOOKED: seat confirmed in " + session.getSessionId();

        } catch (InsufficientCreditsException e) {
            // specific problem first: not enough credits
            outcome = "FAILED: " + e.getMessage()
                    + " Teach " + (int) Math.ceil(e.getShortfall() / 10.0)
                    + " more hour(s) to earn what you need.";

        } catch (SessionUnavailableException e) {
            // second specific problem: the session could not take the student
            if (session.isFull()) {
                joinWaitingList(student, session);
                outcome = "WAITING LIST: session full, your position is "
                        + session.getWaitingList().positionOf(student);
            } else {
                outcome = "FAILED: " + e.getMessage();
            }

        } finally {
            // FINALLY always runs - success, failure or exception - so it is the
            // right place for a log line or for closing resources.
            System.out.println("   [log] booking attempt finished for "
                    + student.getName() + " on " + session.getSessionId());
        }
        return outcome;
    }

    /** Put the student at the end of the LinkedList waiting list. */
    public void joinWaitingList(Student student, Session session) {
        session.getWaitingList().join(student);
        student.addWaitingSession(session.getSessionId());
        notificationService.notify(student, "You are number "
                + session.getWaitingList().positionOf(student)
                + " on the waiting list of " + session.getSessionId() + ".");
    }

    /**
     * Cancel a seat: refund the credits and give the seat to the first student
     * waiting in the queue (FIFO).
     */
    public void cancelBooking(Student student, Session session) throws SessionUnavailableException {
        if (!session.hasStudent(student)) {
            throw new SessionUnavailableException(session.getSessionId(),
                    "you do not hold a seat in this session");
        }
        session.cancelSeat(student);
        student.getBookedSessionIds().remove(session.getSessionId());
        student.getWallet().refund(session.getCost(),
                "Refund for cancelled booking " + session.getSessionId());
        notificationService.notify(student, "Booking cancelled, "
                + session.getCost() + " credits refunded.");

        promoteFromWaitingList(session);
    }

    /** Move the next waiting student into the free seat, if there is one. */
    public void promoteFromWaitingList(Session session) {
        if (session.getWaitingList().isEmpty() || session.isFull()) {
            return;
        }
        Student next = session.getWaitingList().callNext();
        try {
            bookSession(next, session);
            notificationService.notify(next, "A seat opened up - you are now booked in "
                    + session.getSessionId() + "!");
        } catch (InsufficientCreditsException | SessionUnavailableException e) {
            // MULTI-CATCH: both problems are handled in the same way here -
            // the student simply loses the turn and the next one is tried.
            notificationService.notify(next, "A seat opened up but the booking failed: "
                    + e.getMessage());
            promoteFromWaitingList(session);
        }
    }

    /**
     * The session took place: the teacher earns credits for every learner.
     * (The learners already paid when they booked.)
     */
    public void completeSession(Session session) {
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return;
        }
        int creditsPerLearner = session.getCost();
        List<Student> learners = session.getBookedStudents();

        for (Student learner : learners) {
            session.getTeacher().getWallet().earn(creditsPerLearner,
                    "Taught " + session.getSkill().getName() + " to " + learner.getName()
                            + " (" + session.getSessionId() + ")");
        }
        session.markCompleted();
        notificationService.notify(session.getTeacher(), "Session " + session.getSessionId()
                + " completed. You earned " + (creditsPerLearner * learners.size()) + " credits.");
        for (Student learner : learners) {
            notificationService.notify(learner, "Session " + session.getSessionId()
                    + " completed. You can now leave a review.");
        }
    }

    public Repository<Session> getSessionRepository() {
        return sessionRepository;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
