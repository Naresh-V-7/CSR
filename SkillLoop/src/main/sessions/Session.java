package skillloop.sessions;

import java.util.ArrayList;
import java.util.List;

import skillloop.collections.WaitingList;
import skillloop.credits.SkillWallet;
import skillloop.skills.Skill;
import skillloop.users.Student;

/**
 * A learning session: "Ravi teaches Java, 1 hour, 1 seat, on 2026-03-11".
 *
 * This class also contains the SYNCHRONIZATION demo of the project: the seat
 * reservation is the piece of code two booking threads can reach at the same
 * moment.
 */
public class Session {

    private static int sessionCounter = 0;

    private final String sessionId;
    private final Skill skill;
    private final Student teacher;
    private final int maxSeats;
    private final int durationHours;
    private final String scheduledDate;      // kept as text so files stay readable

    private SessionStatus status;

    /** ArrayList: the students who already hold a seat. */
    private final List<Student> bookedStudents = new ArrayList<>();

    /** LinkedList-based waiting list used when all seats are taken. */
    private final WaitingList waitingList = new WaitingList();

    public Session(Skill skill, Student teacher, int maxSeats, int durationHours, String scheduledDate) {
        sessionCounter++;
        this.sessionId = "S" + sessionCounter;
        this.skill = skill;
        this.teacher = teacher;
        this.maxSeats = maxSeats;
        this.durationHours = durationHours;
        this.scheduledDate = scheduledDate;
        this.status = SessionStatus.SCHEDULED;
    }

    /** Overloaded constructor: the common case of a 1-hour session. */
    public Session(Skill skill, Student teacher, int maxSeats, String scheduledDate) {
        this(skill, teacher, maxSeats, 1, scheduledDate);
    }

    // ------------------------------------------------------------------
    // SYNCHRONIZATION - the heart of the concurrency demo
    // ------------------------------------------------------------------

    /**
     * Reserve one seat for a student.
     *
     * The method is SYNCHRONIZED, which means: only ONE thread at a time may be
     * inside it for a given Session object. Java locks the Session object at the
     * start of the call and releases the lock at the end.
     *
     * Without this keyword two students booking the last seat at the same moment
     * could both read "free seats = 1" and both be added -> the session would be
     * over-booked. With it, the first thread takes the seat and the second thread
     * sees zero seats and is told to join the waiting list.
     *
     * @return true when the seat was taken, false when the session is full
     */
    public synchronized boolean reserveSeat(Student student) {
        if (status != SessionStatus.SCHEDULED) {
            return false;
        }
        if (bookedStudents.size() >= maxSeats) {
            return false;                       // caller puts the student on the waiting list
        }
        bookedStudents.add(student);
        if (bookedStudents.size() >= maxSeats) {
            status = SessionStatus.FULL;
        }
        return true;
    }

    /**
     * FOR DEMONSTRATION ONLY - the same logic WITHOUT synchronization.
     *
     * The small sleep between "check" and "add" widens the gap where a second
     * thread can slip in, so the double-booking bug shows up every time we run
     * the demo. Nothing in the real application calls this method.
     */
    public boolean reserveSeatUnsafe(Student student) {
        if (bookedStudents.size() >= maxSeats) {
            return false;
        }
        try {
            Thread.sleep(500);                  // makes the race condition visible
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookedStudents.add(student);
        return true;
    }

    /** Also synchronized: it changes the same bookedStudents list. */
    public synchronized boolean cancelSeat(Student student) {
        boolean removed = bookedStudents.remove(student);
        if (removed && status == SessionStatus.FULL) {
            status = SessionStatus.SCHEDULED;
        }
        return removed;
    }

    // ------------------------------------------------------------------
    // Ordinary session behaviour
    // ------------------------------------------------------------------

    public synchronized boolean isFull() {
        return bookedStudents.size() >= maxSeats;
    }

    public synchronized int getFreeSeats() {
        return maxSeats - bookedStudents.size();
    }

    public boolean isBookable() {
        return status == SessionStatus.SCHEDULED && !isFull();
    }

    /** Cost in Skill Credits: 10 credits per hour. */
    public int getCost() {
        return SkillWallet.costOf(durationHours);
    }

    public void markCompleted() {
        this.status = SessionStatus.COMPLETED;
    }

    public void markCancelled() {
        this.status = SessionStatus.CANCELLED;
    }

    public void markExpired() {
        this.status = SessionStatus.EXPIRED;
    }

    public boolean hasStudent(Student student) {
        return bookedStudents.contains(student);
    }

    public String getSessionId() {
        return sessionId;
    }

    public Skill getSkill() {
        return skill;
    }

    public Student getTeacher() {
        return teacher;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public String getScheduledDate() {
        return scheduledDate;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public List<Student> getBookedStudents() {
        return bookedStudents;
    }

    public WaitingList getWaitingList() {
        return waitingList;
    }

    /** Only used by the tests so ids start from S1 again. */
    public static void resetCounter() {
        sessionCounter = 0;
    }

    @Override
    public String toString() {
        return sessionId + " | " + skill.getName() + " by " + teacher.getName()
                + " | " + scheduledDate + " | " + durationHours + "h"
                + " | seats " + bookedStudents.size() + "/" + maxSeats
                + " | cost " + getCost() + " credits"
                + " | " + status;
    }
}
