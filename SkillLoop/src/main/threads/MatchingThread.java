package skillloop.threads;

import java.util.List;

import skillloop.collections.RequestQueue;
import skillloop.matching.MatchingService;
import skillloop.sessions.SkillRequest;
import skillloop.users.Student;

/**
 * BACKGROUND THREAD 1 - processes the queue of "I want to learn X" requests.
 *
 * Why a thread?
 *   Searching every student for every request takes time. If the main menu did
 *   it, the user would sit and wait. Instead the request is dropped into the
 *   RequestQueue and this worker answers it in the background while the student
 *   keeps using the app.
 *
 * RUNNABLE is used (not "extends Thread") because:
 *   - this class already has its own job, and Java allows only one parent class;
 *   - Runnable separates the TASK from the THREAD that runs it.
 */
public class MatchingThread implements Runnable {

    private final RequestQueue requestQueue;
    private final MatchingService matchingService;

    /** volatile: the flag is written by the main thread and read by this one. */
    private volatile boolean running = true;

    private int processedCount = 0;

    /** DEPENDENCY INJECTION of the queue and the matching service. */
    public MatchingThread(RequestQueue requestQueue, MatchingService matchingService) {
        this.requestQueue = requestQueue;
        this.matchingService = matchingService;
    }

    /** The task the thread performs - this is the single method of Runnable. */
    @Override
    public void run() {
        System.out.println("[MatchingThread] started as '" + Thread.currentThread().getName() + "'");
        while (running) {
            SkillRequest request = requestQueue.next();     // synchronized inside
            if (request == null) {
                sleepQuietly(100);                          // queue empty - wait a bit
                continue;
            }
            process(request);
        }
        System.out.println("[MatchingThread] stopped after " + processedCount + " request(s)");
    }

    private void process(SkillRequest request) {
        List<Student> teachers = matchingService.findTeachersOf(request.getSkillName());
        if (teachers.isEmpty()) {
            request.complete("no teacher found yet");
        } else {
            StringBuilder names = new StringBuilder();
            for (Student teacher : teachers) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(teacher.getName());
            }
            request.complete(teachers.size() + " teacher(s): " + names);
        }
        processedCount++;
        System.out.println("   [matching] " + request);
    }

    /** Ask the thread to finish its current loop and stop. */
    public void stop() {
        this.running = false;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    /**
     * Thread.sleep() throws InterruptedException (a CHECKED exception), so it has
     * to be wrapped in try/catch. Re-setting the interrupt flag is the polite way
     * to handle it.
     */
    private void sleepQuietly(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
