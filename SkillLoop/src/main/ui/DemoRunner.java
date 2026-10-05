package skillloop.ui;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import skillloop.SkillLoopApp;
import skillloop.exceptions.InsufficientCreditsException;
import skillloop.exceptions.InvalidSkillLevelException;
import skillloop.exceptions.SessionUnavailableException;
import skillloop.exceptions.SkillNotFoundException;
import skillloop.matching.MatchingService;
import skillloop.matching.SkillCycle;
import skillloop.matching.SkillCycleMatcher;
import skillloop.matching.SkillMatch;
import skillloop.matching.SkillMatcher;
import skillloop.services.BookingService;
import skillloop.sessions.Session;
import skillloop.sessions.SkillRequest;
import skillloop.skills.Skill;
import skillloop.threads.MatchingThread;
import skillloop.threads.NotificationThread;
import skillloop.threads.SessionExpiryThread;
import skillloop.users.Admin;
import skillloop.users.Student;
import skillloop.users.User;

/**
 * THE PROJECT DEMONSTRATION.
 *
 * Running "java skillloop.Main" and choosing option 1 plays this script from
 * start to finish, in the exact order of the project review:
 *
 *   1. register three students and their skills
 *   2. runtime polymorphism (Student dashboard vs Admin dashboard)
 *   3. SKILL CYCLE MATCHING       A --Java--> B --Python--> C --C--> A
 *   4. booking, credits, completion and a review
 *   5. not enough credits -> exception
 *   6. two students, one seat -> synchronization
 *   7. background threads
 *   8. collections tour and file storage
 */
public class DemoRunner {

    private final SkillLoopApp app;

    private Student studentA;
    private Student studentB;
    private Student studentC;

    public DemoRunner(SkillLoopApp app) {
        this.app = app;
    }

    /** Runs the whole review demo. */
    public void runFullDemo() {
        banner("SKILLLOOP - PEER TO PEER SKILL EXCHANGE FOR STUDENTS");
        step1RegisterStudents();
        step2Polymorphism();
        step3SkillCycleMatching();
        step4DirectMatchesAndLambdas();
        step5BookingAndCredits();
        step6InsufficientCredits();
        step7Synchronization();
        step8BackgroundThreads();
        step9CollectionsTour();
        step10Exceptions();
        step11FileStorage();
        banner("DEMO FINISHED");
    }

    // ------------------------------------------------------------------
    // 1. Registration - constructors, varargs, Set
    // ------------------------------------------------------------------
    private void step1RegisterStudents() {
        title("STEP 1 - REGISTER STUDENTS (constructors, varargs, Set)");

        studentA = app.getStudentService().register("Ravi", "ravi@college.edu", "pass1", "CSE");
        studentB = app.getStudentService().register("Meera", "meera@college.edu", "pass2", "IT");
        studentC = app.getStudentService().register("Arjun", "arjun@college.edu", "pass3", "ECE");

        // VARARGS: several skills in a single call
        studentA.addSkillsInCategory("Programming", "Java");
        studentA.addWantedSkills("Python");

        studentB.addSkillsInCategory("Programming", "Python");
        studentB.addWantedSkills("C");

        studentC.addSkillsInCategory("Programming", "C");
        studentC.addWantedSkills("Java");

        // SET prevents duplicates - this second "Java" is silently ignored
        studentA.addTeachingSkill(new Skill("java", "Programming", 5));
        System.out.println("Ravi tried to add 'Java' twice; his teaching set has "
                + studentA.getTeachingSkills().size() + " skill (Set blocked the duplicate)");

        // the catalogue of skills offered on the platform
        app.getSkillService().addSkill(new Skill("Java", "Programming", 4));
        app.getSkillService().addSkill(new Skill("Python", "Programming", 3));
        app.getSkillService().addSkill(new Skill("C", "Programming", 5));
        app.getSkillService().addSkill(new Skill("Public Speaking", "Soft Skills", 4));

        for (Student student : app.getStudentService().getAllStudents()) {
            System.out.println("  registered -> " + student);
        }
        System.out.println("Total users created (static counter): " + User.getTotalUsers());
    }

    // ------------------------------------------------------------------
    // 2. Runtime polymorphism
    // ------------------------------------------------------------------
    private void step2Polymorphism() {
        title("STEP 2 - RUNTIME POLYMORPHISM (one reference, two behaviours)");

        User user;                         // parent reference

        user = studentA;                   // holds a Student object
        user.showDashboard();              // Student.showDashboard() runs

        user = app.getAdmin();             // now holds an Admin object
        user.showDashboard();              // Admin.showDashboard() runs

        System.out.println("Same line of code, two different dashboards -> dynamic binding.");

        // DOWNCASTING: an Admin-only method needs the real type back
        if (user instanceof Admin) {
            Admin admin = (Admin) user;
            admin.printPlatformStats(app.getStudentRepository().size(),
                    app.getSkillRepository().size(), app.getSessionRepository().size());
        }
    }

    // ------------------------------------------------------------------
    // 3. THE INNOVATIVE FEATURE
    // ------------------------------------------------------------------
    private void step3SkillCycleMatching() {
        title("STEP 3 - SKILL CYCLE MATCHING (the innovative feature)");

        System.out.println("Ravi  teaches Java   and wants Python");
        System.out.println("Meera teaches Python and wants C");
        System.out.println("Arjun teaches C      and wants Java");
        System.out.println();

        SkillCycleMatcher matcher = app.getSkillCycleMatcher();
        List<SkillCycle> cycles = matcher.findAllCycles();

        System.out.println("Cycles found by the depth-first search: " + cycles.size());
        for (SkillCycle cycle : cycles) {
            System.out.println("   " + cycle);
        }

        List<SkillCycle> multi = matcher.findMultiPersonCycles();
        if (!multi.isEmpty()) {
            System.out.println();
            System.out.println(">>> SKILL EXCHANGE CYCLE DETECTED:");
            System.out.println("    " + multi.get(0).describe());
            System.out.println("    Everybody teaches one person and learns from another.");
        }
    }

    // ------------------------------------------------------------------
    // 4. Lambda + functional interface
    // ------------------------------------------------------------------
    private void step4DirectMatchesAndLambdas() {
        title("STEP 4 - MATCHING WITH LAMBDAS AND A FUNCTIONAL INTERFACE");

        MatchingService matching = app.getMatchingService();

        // LAMBDA + STREAM
        System.out.println("Students who teach Java (stream + lambda):");
        matching.getStudentRepository().getAll().stream()
                .filter(student -> student.teaches("Java"))
                .forEach(student -> System.out.println("   " + student.getName()));

        // FUNCTIONAL INTERFACE used explicitly
        SkillMatcher teachesPython = student -> student.teaches("Python");
        SkillMatcher wantsC = SkillMatcher.wantsSkill("C");
        SkillMatcher both = teachesPython.and(wantsC);     // default method of the interface

        System.out.println("Students who teach Python AND want to learn C:");
        for (Student student : matching.filter(both)) {
            System.out.println("   " + student.getName());
        }

        System.out.println("Match list for Ravi (score uses the Math class):");
        for (SkillMatch match : matching.findMatchesFor(studentA)) {
            System.out.println("   " + match);
        }
    }

    // ------------------------------------------------------------------
    // 5. Booking, credits, completion, review
    // ------------------------------------------------------------------
    private void step5BookingAndCredits() {
        title("STEP 5 - BOOKING, SKILL CREDITS, COMPLETION AND REVIEW");

        Session javaSession = app.createSession(studentA, "Java", 2, 1, "2026-12-01");
        System.out.println("New session: " + javaSession);

        System.out.println("Arjun's wallet before booking: " + studentC.getWallet());
        System.out.println(app.getBookingService().bookOrJoinWaitingList(studentC, javaSession));
        System.out.println("Arjun's wallet after booking : " + studentC.getWallet());

        app.getBookingService().completeSession(javaSession);
        System.out.println("Ravi's wallet after teaching : " + studentA.getWallet());

        try {
            app.getReviewService().addReview(studentC, javaSession, 5, "Very clear explanation!");
            System.out.println("Ravi's rating is now " + studentA.getAverageRating() + " / 5");
        } catch (SessionUnavailableException e) {
            System.out.println("Review failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 6. Custom exception
    // ------------------------------------------------------------------
    private void step6InsufficientCredits() {
        title("STEP 6 - NOT ENOUGH CREDITS (custom checked exception)");

        Student broke = app.getStudentService().register("Nisha", "nisha@college.edu", "pass4", "CSE");
        broke.addWantedSkills("Python");

        Session python = app.createSession(studentB, "Python", 5, 3, "2026-12-05");
        System.out.println("Session cost: " + python.getCost()
                + " credits, Nisha has " + broke.getWallet().getBalance());

        try {
            app.getBookingService().bookSession(broke, python);
            System.out.println("Booked (this line should not be reached)");
        } catch (InsufficientCreditsException e) {
            System.out.println("Caught InsufficientCreditsException -> " + e.getMessage());
            System.out.println("She needs " + e.getShortfall() + " more credits.");
        } catch (SessionUnavailableException e) {
            System.out.println("Caught SessionUnavailableException -> " + e.getMessage());
        } finally {
            System.out.println("finally: the booking attempt is over (this always runs).");
        }
    }

    // ------------------------------------------------------------------
    // 7. SYNCHRONIZATION
    // ------------------------------------------------------------------
    private void step7Synchronization() {
        title("STEP 7 - TWO STUDENTS, ONE SEAT (synchronization)");

        Student first = app.getStudentService().register("Kiran", "kiran@college.edu", "p", "CSE");
        Student second = app.getStudentService().register("Divya", "divya@college.edu", "p", "IT");

        // ---------- (a) WITHOUT synchronization ----------
        Session unsafe = app.createSession(studentA, "Java", 1, 1, "2026-12-10");

        /*
         * A "start gate": both threads wait on the latch and are released at the
         * very same moment, so the race condition shows up on every run instead
         * of once in a while. It only makes the timing reliable for the demo -
         * the bug itself is in reserveSeatUnsafe(), not here.
         */
        CountDownLatch startGate = new CountDownLatch(1);
        Runnable unsafeBooking1 = () -> {
            awaitQuietly(startGate);
            unsafe.reserveSeatUnsafe(first);
        };
        Runnable unsafeBooking2 = () -> {
            awaitQuietly(startGate);
            unsafe.reserveSeatUnsafe(second);
        };

        Thread t1 = new Thread(unsafeBooking1, "booking-thread-1");
        Thread t2 = new Thread(unsafeBooking2, "booking-thread-2");
        t1.start();
        t2.start();
        sleepQuietly(50);          // give both threads time to reach the gate
        startGate.countDown();     // ... and release them together
        joinQuietly(t1, t2);

        int unsafeSeats = unsafe.getBookedStudents().size();
        System.out.println("(a) WITHOUT synchronized: seats taken = "
                + unsafeSeats + " out of " + unsafe.getMaxSeats()
                + (unsafeSeats > unsafe.getMaxSeats()
                   ? "  <-- the session is OVER-BOOKED, both students got the same seat!"
                   : "  (this time the threads did not overlap - run the demo again)"));

        // ---------- (b) WITH synchronization ----------
        Session safe = app.createSession(studentA, "Java", 1, 1, "2026-12-11");
        BookingService booking = app.getBookingService();

        Runnable safeBooking1 = () -> System.out.println("   "
                + first.getName() + ": " + booking.bookOrJoinWaitingList(first, safe));
        Runnable safeBooking2 = () -> System.out.println("   "
                + second.getName() + ": " + booking.bookOrJoinWaitingList(second, safe));

        Thread t3 = new Thread(safeBooking1, "booking-thread-3");
        Thread t4 = new Thread(safeBooking2, "booking-thread-4");
        t3.start();
        t4.start();
        joinQuietly(t3, t4);

        System.out.println("(b) WITH synchronized: seats taken = "
                + safe.getBookedStudents().size() + " out of " + safe.getMaxSeats()
                + ", waiting list -> " + safe.getWaitingList());
    }

    /** Wait at the start gate; InterruptedException is checked, so it is caught. */
    private void awaitQuietly(CountDownLatch gate) {
        try {
            gate.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void joinQuietly(Thread... threads) {      // VARARGS again
        for (Thread thread : threads) {
            try {
                thread.join();                          // wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------
    // 8. Background threads
    // ------------------------------------------------------------------
    private void step8BackgroundThreads() {
        title("STEP 8 - BACKGROUND THREADS (Runnable, start, sleep, join)");

        MatchingThread matchingWorker = new MatchingThread(app.getRequestQueue(), app.getMatchingService());
        NotificationThread notificationWorker = new NotificationThread(app.getNotificationService());
        SessionExpiryThread expiryWorker = new SessionExpiryThread(app.getSessionRepository(),
                "2026-12-03", 300);

        // an old session nobody completed - the expiry worker will clean it up
        app.createSession(studentB, "Python", 3, 1, "2026-01-15");

        Thread t1 = new Thread(matchingWorker, "matching-worker");
        Thread t2 = new Thread(notificationWorker, "notification-worker");
        Thread t3 = new Thread(expiryWorker, "expiry-worker");

        t1.start();
        t2.start();
        t3.start();
        System.out.println("Thread states: " + t1.getName() + "=" + t1.getState()
                + ", " + t2.getName() + "=" + t2.getState());

        // the students drop requests into the QUEUE
        app.getRequestQueue().submit(new SkillRequest(studentA.getId(), studentA.getName(), "Python"));
        app.getRequestQueue().submit(new SkillRequest(studentB.getId(), studentB.getName(), "C"));
        app.getRequestQueue().submit(new SkillRequest(studentC.getId(), studentC.getName(), "Java"));

        notificationWorker.queueMessage(studentA, "Your Java session was rated 5/5.");

        sleepQuietly(900);                 // let the workers do their job

        matchingWorker.stop();
        notificationWorker.stop();
        expiryWorker.stop();
        joinQuietly(t1, t2, t3);

        System.out.println("Requests processed in the background: " + matchingWorker.getProcessedCount());
        System.out.println("Notifications delivered            : " + notificationWorker.getDeliveredCount());
        System.out.println("Sessions expired automatically     : " + expiryWorker.getExpiredCount());
    }

    private void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------
    // 9. Collections
    // ------------------------------------------------------------------
    private void step9CollectionsTour() {
        title("STEP 9 - COLLECTIONS USED IN SKILLLOOP");

        System.out.println("ArrayList  (Repository<Student>) : "
                + app.getStudentRepository().size() + " students");

        System.out.println("Set        (Ravi's teaching set) : " + studentA.getTeachingSkills());

        Map<String, List<Student>> skillMap = app.getMatchingService().buildSkillToTeachersMap();
        System.out.println("TreeMap    (skill -> teachers, alphabetical):");
        for (Map.Entry<String, List<Student>> entry : skillMap.entrySet()) {
            StringBuilder names = new StringBuilder();
            for (Student teacher : entry.getValue()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(teacher.getName());
            }
            System.out.println("   " + entry.getKey() + " -> " + names);
        }

        Set<Student> leaderboard = app.getMatchingService().buildRatingLeaderboard();
        System.out.println("TreeSet    (students sorted by rating, best first):");
        for (Student student : leaderboard) {
            System.out.println("   " + student.getAverageRating() + "  " + student.getName());
        }

        System.out.println("TreeSet    (skill names, alphabetical): "
                + app.getSkillService().getSortedSkillNames());

        System.out.println("HashMap    (id -> student) lookup of id "
                + studentB.getId() + " -> "
                + app.getStudentService().findById(studentB.getId()).getName());

        int removed = app.getStudentService().removeEmptyProfiles();
        System.out.println("Iterator   (removed " + removed + " empty profile(s) safely)");
    }

    // ------------------------------------------------------------------
    // 10. Exceptions
    // ------------------------------------------------------------------
    private void step10Exceptions() {
        title("STEP 10 - EXCEPTION HANDLING SUMMARY");

        // checked exception
        try {
            app.getSkillService().findSkill("Kotlin");
        } catch (SkillNotFoundException e) {
            System.out.println("checked   -> SkillNotFoundException: " + e.getMessage());
        }

        // unchecked exception
        try {
            new Skill("Java", "Programming", 9);
        } catch (InvalidSkillLevelException e) {
            System.out.println("unchecked -> InvalidSkillLevelException: " + e.getMessage());
        }

        // overloaded search, three versions
        try {
            System.out.println("findSkill(\"Java\")                        -> "
                    + app.getSkillService().findSkill("Java"));
            System.out.println("findSkill(\"Java\", \"Programming\")          -> "
                    + app.getSkillService().findSkill("Java", "Programming"));
            System.out.println("findSkill(\"Java\", \"Programming\", 3)       -> "
                    + app.getSkillService().findSkill("Java", "Programming", 3));
        } catch (SkillNotFoundException e) {
            System.out.println("search failed: " + e.getMessage());
        } finally {
            System.out.println("finally: search finished.");
        }
    }

    // ------------------------------------------------------------------
    // 11. Files
    // ------------------------------------------------------------------
    private void step11FileStorage() {
        title("STEP 11 - FILE HANDLING");

        app.saveAll();
        try {
            List<String> lines = app.getFileStorage().readLines("students.txt");
            System.out.println("First lines of " + app.getFileStorage().getDataFolder() + "/students.txt:");
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                System.out.println("   " + lines.get(i));
            }
            System.out.println("transactions.txt has "
                    + app.getFileStorage().readLines("transactions.txt").size() + " credit records");
        } catch (Exception e) {
            System.out.println("Could not read back the files: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    private void banner(String text) {
        System.out.println();
        System.out.println("################################################################");
        System.out.println("#  " + text);
        System.out.println("################################################################");
    }

    private void title(String text) {
        System.out.println();
        System.out.println("----------------------------------------------------------------");
        System.out.println(" " + text);
        System.out.println("----------------------------------------------------------------");
    }

    public Student getStudentA() {
        return studentA;
    }

    public Student getStudentB() {
        return studentB;
    }

    public Student getStudentC() {
        return studentC;
    }
}
