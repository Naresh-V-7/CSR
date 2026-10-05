package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import skillloop.collections.Repository;
import skillloop.exceptions.InsufficientCreditsException;
import skillloop.exceptions.SessionUnavailableException;
import skillloop.services.BookingService;
import skillloop.sessions.Session;
import skillloop.sessions.SessionStatus;
import skillloop.skills.Skill;
import skillloop.users.Student;
import skillloop.users.User;

/**
 * JUnit 5 tests for BookingService.
 *
 * These tests show DEPENDENCY INJECTION and MOCKING together:
 * the service under test is built with a repository created inside the test and
 * with a MockNotificationService instead of the real console notifier.
 */
class BookingServiceTest {

    private Repository<Session> sessionRepository;
    private MockNotificationService mockNotifier;      // the MOCK
    private BookingService bookingService;

    private Student teacher;
    private Student learner;
    private Session session;

    @BeforeEach
    void setUp() {
        User.resetCounters();
        Session.resetCounter();

        sessionRepository = new Repository<>();
        mockNotifier = new MockNotificationService();

        // DEPENDENCY INJECTION: both parts are handed to the service
        bookingService = new BookingService(sessionRepository, mockNotifier);

        teacher = new Student("Ravi", "ravi@c.edu", "p", "CSE");
        teacher.addTeachingSkill(new Skill("Java", "Programming", 4));

        learner = new Student("Arjun", "arjun@c.edu", "p", "ECE");
        learner.addLearningSkill(new Skill("Java"));

        session = new Session(new Skill("Java"), teacher, 1, 1, "2026-12-01");
        sessionRepository.add(session);
    }

    @Test
    @DisplayName("A booking succeeds, takes the seat and charges the wallet")
    void successfulBooking() throws Exception {
        bookingService.bookSession(learner, session);

        assertTrue(session.hasStudent(learner));
        assertEquals(10, learner.getWallet().getBalance());     // 20 - 10
        assertEquals(0, session.getFreeSeats());
        assertEquals(SessionStatus.FULL, session.getStatus());
        assertTrue(learner.getBookedSessionIds().contains(session.getSessionId()));
    }

    @Test
    @DisplayName("MOCKING: the booking notifies both the learner and the teacher")
    void bookingSendsNotifications() throws Exception {
        bookingService.bookSession(learner, session);

        assertEquals(2, mockNotifier.getSentCount());
        assertTrue(mockNotifier.wasNotified(learner, "Seat confirmed"));
        assertTrue(mockNotifier.wasNotified(teacher, "booked your Java session"));
    }

    @Test
    @DisplayName("Not enough credits -> InsufficientCreditsException, no seat taken")
    void bookingWithoutEnoughCredits() {
        Session expensive = new Session(new Skill("Java"), teacher, 5, 3, "2026-12-01");
        sessionRepository.add(expensive);                       // costs 30, learner has 20

        InsufficientCreditsException thrown = assertThrows(InsufficientCreditsException.class,
                () -> bookingService.bookSession(learner, expensive));

        assertEquals(10, thrown.getShortfall());
        assertFalse(expensive.hasStudent(learner));
        assertEquals(20, learner.getWallet().getBalance());
        assertEquals(0, mockNotifier.getSentCount(), "a failed booking notifies nobody");
    }

    @Test
    @DisplayName("A full session throws SessionUnavailableException")
    void fullSessionCannotBeBooked() throws Exception {
        bookingService.bookSession(learner, session);           // the only seat

        Student second = new Student("Meera", "meera@c.edu", "p", "IT");
        SessionUnavailableException thrown = assertThrows(SessionUnavailableException.class,
                () -> bookingService.bookSession(second, session));

        assertTrue(thrown.getMessage().contains("all seats are taken"));
    }

    @Test
    @DisplayName("The waiting list receives the student when the session is full")
    void secondStudentGoesToTheWaitingList() throws Exception {
        bookingService.bookSession(learner, session);

        Student second = new Student("Meera", "meera@c.edu", "p", "IT");
        String outcome = bookingService.bookOrJoinWaitingList(second, session);

        assertTrue(outcome.startsWith("WAITING LIST"));
        assertEquals(1, session.getWaitingList().size());
        assertEquals(1, session.getWaitingList().positionOf(second));
        assertEquals(20, second.getWallet().getBalance(), "a waiting student pays nothing");
    }

    @Test
    @DisplayName("Cancelling refunds the learner and promotes the first waiting student")
    void cancellationPromotesFromTheWaitingList() throws Exception {
        bookingService.bookSession(learner, session);
        Student second = new Student("Meera", "meera@c.edu", "p", "IT");
        bookingService.bookOrJoinWaitingList(second, session);

        bookingService.cancelBooking(learner, session);

        assertEquals(20, learner.getWallet().getBalance(), "credits refunded");
        assertTrue(session.hasStudent(second), "the waiting student got the free seat");
        assertEquals(10, second.getWallet().getBalance(), "and paid for it");
    }

    @Test
    @DisplayName("You cannot book your own session")
    void teacherCannotBookOwnSession() {
        assertThrows(SessionUnavailableException.class,
                () -> bookingService.bookSession(teacher, session));
    }

    @Test
    @DisplayName("Completing a session pays the teacher")
    void completingASessionPaysTheTeacher() throws Exception {
        bookingService.bookSession(learner, session);
        bookingService.completeSession(session);

        assertEquals(SessionStatus.COMPLETED, session.getStatus());
        assertEquals(30, teacher.getWallet().getBalance());     // 20 + 10 earned
        assertEquals(10, teacher.getWallet().getTotalEarned());
    }

    @Test
    @DisplayName("SYNCHRONIZATION: two threads, one seat -> only one booking")
    void onlyOneThreadGetsTheLastSeat() throws Exception {
        Student first = new Student("Kiran", "kiran@c.edu", "p", "CSE");
        Student second = new Student("Divya", "divya@c.edu", "p", "IT");

        Runnable book1 = () -> bookingService.bookOrJoinWaitingList(first, session);
        Runnable book2 = () -> bookingService.bookOrJoinWaitingList(second, session);

        Thread t1 = new Thread(book1);
        Thread t2 = new Thread(book2);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(1, session.getBookedStudents().size(),
                "the synchronized reserveSeat() must never over-book");
        assertEquals(1, session.getWaitingList().size(),
                "the other student is on the waiting list");
    }
}
