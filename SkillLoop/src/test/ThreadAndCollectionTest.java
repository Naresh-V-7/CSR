package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import skillloop.collections.Repository;
import skillloop.collections.RequestQueue;
import skillloop.collections.WaitingList;
import skillloop.matching.MatchingService;
import skillloop.sessions.Session;
import skillloop.sessions.SessionStatus;
import skillloop.sessions.SkillRequest;
import skillloop.skills.Skill;
import skillloop.threads.MatchingThread;
import skillloop.threads.SessionExpiryThread;
import skillloop.users.Student;
import skillloop.users.User;

/** JUnit 5 tests for the collections and the background workers. */
class ThreadAndCollectionTest {

    @Test
    @DisplayName("The waiting list keeps FIFO order and reports the position")
    void waitingListIsFifo() {
        User.resetCounters();
        WaitingList waitingList = new WaitingList();
        Student first = new Student("A", "a@c.edu", "p");
        Student second = new Student("B", "b@c.edu", "p");
        Student third = new Student("C", "c@c.edu", "p");

        waitingList.join(first);
        waitingList.join(second);
        waitingList.join(third);
        waitingList.join(first);                  // joining twice changes nothing

        assertEquals(3, waitingList.size());
        assertEquals(1, waitingList.positionOf(first));
        assertEquals(3, waitingList.positionOf(third));

        assertEquals("A", waitingList.callNext().getName());     // FIFO
        assertEquals(1, waitingList.positionOf(second));

        assertTrue(waitingList.leave(third));                    // Iterator.remove()
        assertEquals(1, waitingList.size());
    }

    @Test
    @DisplayName("The request queue is first-in first-out")
    void requestQueueIsFifo() {
        RequestQueue queue = new RequestQueue();
        queue.submit(new SkillRequest(1, "A", "Java"));
        queue.submit(new SkillRequest(2, "B", "Python"));

        assertEquals(2, queue.size());
        assertEquals("Java", queue.next().getSkillName());
        assertEquals("Python", queue.next().getSkillName());
        assertNull(queue.next(), "an empty queue returns null");
    }

    @Test
    @DisplayName("The generic Repository<T> stores, filters and removes safely")
    void genericRepositoryWorks() {
        User.resetCounters();
        Repository<Student> repository = new Repository<>();
        Student ravi = new Student("Ravi", "r@c.edu", "p");
        ravi.addSkills("Java");
        Student meera = new Student("Meera", "m@c.edu", "p");

        repository.add(ravi);
        repository.add(meera);

        assertEquals(2, repository.size());
        assertEquals(1, repository.findBy(student -> student.teaches("Java")).size());

        // Iterator.remove() inside removeWhere()
        assertEquals(1, repository.removeWhere(student -> student.getTeachingSkills().isEmpty()));
        assertEquals(1, repository.size());
    }

    @Test
    @DisplayName("The background MatchingThread answers the queued requests")
    void matchingThreadProcessesTheQueue() throws InterruptedException {
        User.resetCounters();
        Repository<Student> students = new Repository<>();
        Student teacher = new Student("Ravi", "r2@c.edu", "p");
        teacher.addSkills("Java");
        students.add(teacher);

        RequestQueue queue = new RequestQueue();
        queue.submit(new SkillRequest(2, "Arjun", "Java"));
        SkillRequest unanswered = new SkillRequest(3, "Nisha", "Kotlin");
        queue.submit(unanswered);

        MatchingThread worker = new MatchingThread(queue, new MatchingService(students));
        Thread thread = new Thread(worker, "test-matching-worker");
        thread.start();

        Thread.sleep(400);                 // let the worker do its job
        worker.stop();
        thread.join();

        assertEquals(2, worker.getProcessedCount());
        assertTrue(queue.isEmpty());
        assertTrue(unanswered.isProcessed());
        assertEquals("no teacher found yet", unanswered.getResult());
    }

    @Test
    @DisplayName("The expiry worker marks past sessions as EXPIRED")
    void expiryThreadExpiresOldSessions() {
        User.resetCounters();
        Session.resetCounter();
        Repository<Session> sessions = new Repository<>();
        Student teacher = new Student("Ravi", "r3@c.edu", "p");

        Session old = new Session(new Skill("Java"), teacher, 2, 1, "2026-01-01");
        Session future = new Session(new Skill("Java"), teacher, 2, 1, "2030-01-01");
        sessions.add(old);
        sessions.add(future);

        // the sweep can be called directly, without starting a thread
        int expired = new SessionExpiryThread(sessions, "2026-06-01").expirePastSessions();

        assertEquals(1, expired);
        assertEquals(SessionStatus.EXPIRED, old.getStatus());
        assertEquals(SessionStatus.SCHEDULED, future.getStatus());
    }
}
