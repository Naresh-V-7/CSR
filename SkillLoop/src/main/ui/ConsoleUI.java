package skillloop.ui;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import skillloop.SkillLoopApp;
import skillloop.credits.CreditTransaction;
import skillloop.exceptions.InvalidSkillLevelException;
import skillloop.exceptions.SessionUnavailableException;
import skillloop.exceptions.SkillNotFoundException;
import skillloop.matching.SkillCycle;
import skillloop.matching.SkillMatch;
import skillloop.sessions.Review;
import skillloop.sessions.Session;
import skillloop.sessions.SessionStatus;
import skillloop.sessions.SkillRequest;
import skillloop.skills.Skill;
import skillloop.users.Student;
import skillloop.users.User;

/**
 * The text based user interface (the top layer of the architecture).
 *
 *      ConsoleUI  ->  Services  ->  Model  ->  Repository / FileStorage
 *
 * The UI contains NO business rules: every action it offers is one call into a
 * service. That is why the same services can be driven by the demo, by the unit
 * tests, or later by a GUI, without any change.
 */
public class ConsoleUI {

    private final SkillLoopApp app;
    private final Scanner scanner;

    /** The student who is logged in right now (null = nobody). */
    private Student currentStudent;

    public ConsoleUI(SkillLoopApp app, Scanner scanner) {
        this.app = app;
        this.scanner = scanner;
    }

    // ==================================================================
    // Login / register
    // ==================================================================

    public void start() {
        boolean running = true;
        while (running) {
            if (currentStudent == null) {
                running = showWelcomeMenu();
            } else {
                running = showStudentMenu();
            }
        }
        System.out.println("Goodbye!");
    }

    private boolean showWelcomeMenu() {
        header("WELCOME TO " + User.APP_NAME + " v" + User.APP_VERSION);
        System.out.println(" 1. Login");
        System.out.println(" 2. Register");
        System.out.println(" 3. Admin dashboard");
        System.out.println(" 0. Exit");
        switch (ask("Choice")) {
            case "1": doLogin(); return true;
            case "2": doRegister(); return true;
            case "3": app.getAdmin().showDashboard(); return true;
            case "0": app.saveAll(); return false;
            default:  System.out.println("Please choose 0-3."); return true;
        }
    }

    private void doLogin() {
        String email = ask("E-mail");
        String password = ask("Password");
        Student student = app.getStudentService().login(email, password);
        if (student == null) {
            System.out.println("Wrong e-mail or password.");
        } else {
            currentStudent = student;
            System.out.println("Welcome back, " + student.getName() + "!");
        }
    }

    private void doRegister() {
        String name = ask("Name");
        String email = ask("E-mail");
        String password = ask("Password");
        String college = ask("College");
        currentStudent = app.getStudentService().register(name, email, password, college);
        System.out.println("Registered with id " + currentStudent.getId()
                + " and " + currentStudent.getWallet().getBalance() + " welcome credits.");
    }

    // ==================================================================
    // Student menu
    // ==================================================================

    private boolean showStudentMenu() {
        header("MAIN MENU - " + currentStudent.getName()
                + "  (" + currentStudent.getWallet().getBalance() + " credits)");
        System.out.println("  1. My dashboard");
        System.out.println("  2. Add a skill I can teach");
        System.out.println("  3. Add a skill I want to learn");
        System.out.println("  4. Find skills / search the catalogue");
        System.out.println("  5. Matching: teachers, direct exchanges, SKILL CYCLES");
        System.out.println("  6. Offer a session (as teacher)");
        System.out.println("  7. Browse and book sessions");
        System.out.println("  8. My waiting list positions");
        System.out.println("  9. My Skill Wallet");
        System.out.println(" 10. Complete a session I taught");
        System.out.println(" 11. Write a review");
        System.out.println(" 12. Request a skill (goes into the background queue)");
        System.out.println(" 13. Save everything to files");
        System.out.println(" 14. Logout");
        System.out.println("  0. Save and exit");

        switch (ask("Choice")) {
            case "1":  currentStudent.showDashboard(); return true;
            case "2":  addTeachingSkill(); return true;
            case "3":  addLearningSkill(); return true;
            case "4":  findSkills(); return true;
            case "5":  showMatching(); return true;
            case "6":  offerSession(); return true;
            case "7":  browseAndBook(); return true;
            case "8":  showWaitingLists(); return true;
            case "9":  showWallet(); return true;
            case "10": completeSession(); return true;
            case "11": writeReview(); return true;
            case "12": requestSkill(); return true;
            case "13": app.saveAll(); return true;
            case "14": currentStudent = null; return true;
            case "0":  app.saveAll(); return false;
            default:   System.out.println("Please choose 0-14."); return true;
        }
    }

    // ------------------------------------------------------------------

    private void addTeachingSkill() {
        String name = ask("Skill name");
        String category = ask("Category (Programming / Soft Skills / ...)");
        int level = askInt("Your level (1-5)");
        try {
            Skill skill = new Skill(name, category, level);
            int before = currentStudent.getTeachingSkills().size();
            currentStudent.addTeachingSkill(skill);
            app.getSkillService().addSkill(skill);
            if (currentStudent.getTeachingSkills().size() == before) {
                System.out.println("You already teach that skill (the Set blocked the duplicate).");
            } else {
                System.out.println("Added: " + skill);
            }
        } catch (InvalidSkillLevelException e) {
            // unchecked exception, caught here so the menu does not crash
            System.out.println("Rejected: " + e.getMessage());
        }
    }

    private void addLearningSkill() {
        String name = ask("Skill you want to learn");
        currentStudent.addLearningSkill(new Skill(name));
        System.out.println("Added to your wish list.");
    }

    private void findSkills() {
        header("FIND SKILLS");
        System.out.println("Catalogue (TreeSet, alphabetical): "
                + app.getSkillService().getSortedSkillNames());
        String keyword = ask("Search keyword (blank = skip)");
        if (!keyword.isEmpty()) {
            List<Skill> found = app.getSkillService().searchByKeyword(keyword);
            if (found.isEmpty()) {
                System.out.println("Nothing matched '" + keyword + "'.");
            } else {
                for (Skill skill : found) {
                    System.out.println("   " + skill);
                }
            }
            try {
                // the overloaded exact search
                System.out.println("Exact match: " + app.getSkillService().findSkill(keyword));
            } catch (SkillNotFoundException e) {
                System.out.println("(no exact match: " + e.getMessage() + ")");
            }
        }

        Map<String, List<Student>> teachers = app.getMatchingService().buildSkillToTeachersMap();
        System.out.println("Who teaches what:");
        for (Map.Entry<String, List<Student>> entry : teachers.entrySet()) {
            System.out.print("   " + entry.getKey() + " -> ");
            for (Student teacher : entry.getValue()) {
                System.out.print(teacher.getName() + " ");
            }
            System.out.println();
        }
    }

    private void showMatching() {
        header("MATCHING FOR " + currentStudent.getName());

        System.out.println("- Potential matches (best score first):");
        List<SkillMatch> matches = app.getMatchingService().findMatchesFor(currentStudent);
        if (matches.isEmpty()) {
            System.out.println("   (add a skill to your wish list first)");
        }
        for (SkillMatch match : matches) {
            System.out.println("   " + match);
        }

        System.out.println("- Direct exchanges (you teach them, they teach you):");
        List<Student> partners = app.getMatchingService().findDirectExchangePartners(currentStudent);
        if (partners.isEmpty()) {
            System.out.println("   (none yet)");
        }
        for (Student partner : partners) {
            System.out.println("   " + partner.getName());
        }

        System.out.println("- SKILL CYCLES you belong to:");
        List<SkillCycle> cycles = app.getSkillCycleMatcher().findCyclesFor(currentStudent);
        if (cycles.isEmpty()) {
            System.out.println("   (no cycle found yet)");
        }
        for (SkillCycle cycle : cycles) {
            System.out.println("   " + cycle);
        }

        System.out.println("- Rating leaderboard (TreeSet):");
        Set<Student> leaderboard = app.getMatchingService().buildRatingLeaderboard();
        for (Student student : leaderboard) {
            System.out.println("   " + student.getAverageRating() + "  " + student.getName());
        }
    }

    private void offerSession() {
        String skillName = ask("Skill you will teach");
        if (!currentStudent.teaches(skillName)) {
            System.out.println("Add it to your teaching skills first (menu option 2).");
            return;
        }
        int seats = askInt("Number of seats");
        int hours = askInt("Duration in hours");
        String date = ask("Date (yyyy-mm-dd)");
        Session session = app.createSession(currentStudent, skillName, seats, hours, date);
        System.out.println("Session created: " + session);
        System.out.println("Learners will pay " + session.getCost()
                + " credits, and you will earn the same amount per learner.");
    }

    private void browseAndBook() {
        header("AVAILABLE SESSIONS");
        List<Session> sessions = app.getSessionRepository().getAll();
        int shown = 0;
        for (Session session : sessions) {
            if (session.getStatus() != SessionStatus.COMPLETED
                    && session.getStatus() != SessionStatus.CANCELLED) {
                System.out.println("   " + session);
                shown++;
            }
        }
        if (shown == 0) {
            System.out.println("   (no sessions yet)");
            return;
        }
        String id = ask("Session id to book (blank = cancel)");
        if (id.isEmpty()) {
            return;
        }
        Session chosen = findSessionById(id);
        if (chosen == null) {
            System.out.println("No session with id " + id);
            return;
        }
        // one call - the service does the credit check, the seat and the waiting list
        System.out.println(app.getBookingService().bookOrJoinWaitingList(currentStudent, chosen));
    }

    private void showWaitingLists() {
        header("MY WAITING LISTS");
        if (currentStudent.getWaitingSessionIds().isEmpty()) {
            System.out.println("   You are not waiting for any session.");
            return;
        }
        for (String sessionId : currentStudent.getWaitingSessionIds()) {
            Session session = findSessionById(sessionId);
            if (session != null) {
                System.out.println("   " + sessionId + " (" + session.getSkill().getName()
                        + ") - your position: "
                        + session.getWaitingList().positionOf(currentStudent)
                        + " of " + session.getWaitingList().size());
            }
        }
    }

    private void showWallet() {
        header("SKILL WALLET - " + currentStudent.getName());
        System.out.println(" Current credits : " + currentStudent.getWallet().getBalance());
        System.out.println(" Credits earned  : " + currentStudent.getWallet().getTotalEarned());
        System.out.println(" Credits spent   : " + currentStudent.getWallet().getTotalSpent());
        System.out.println(" Transaction history:");
        List<CreditTransaction> history = currentStudent.getWallet().getHistory();
        if (history.isEmpty()) {
            System.out.println("   (no movements yet)");
        }
        for (CreditTransaction transaction : history) {
            System.out.println("   " + transaction);
        }
    }

    private void completeSession() {
        header("COMPLETE A SESSION I TAUGHT");
        for (Session session : app.getSessionRepository().getAll()) {
            if (session.getTeacher().getId() == currentStudent.getId()) {
                System.out.println("   " + session);
            }
        }
        String id = ask("Session id to complete (blank = cancel)");
        if (id.isEmpty()) {
            return;
        }
        Session session = findSessionById(id);
        if (session == null || session.getTeacher().getId() != currentStudent.getId()) {
            System.out.println("That is not one of your sessions.");
            return;
        }
        app.getBookingService().completeSession(session);
        System.out.println("Done. Your balance is now "
                + currentStudent.getWallet().getBalance() + " credits.");
    }

    private void writeReview() {
        header("WRITE A REVIEW");
        for (String sessionId : currentStudent.getBookedSessionIds()) {
            Session session = findSessionById(sessionId);
            if (session != null && session.getStatus() == SessionStatus.COMPLETED) {
                System.out.println("   " + session);
            }
        }
        String id = ask("Session id (blank = cancel)");
        if (id.isEmpty()) {
            return;
        }
        Session session = findSessionById(id);
        if (session == null) {
            System.out.println("No such session.");
            return;
        }
        int rating = askInt("Rating 1-5");
        String comment = ask("Comment");
        try {
            Review review = app.getReviewService().addReview(currentStudent, session, rating, comment);
            System.out.println("Thank you! " + review);
        } catch (SessionUnavailableException e) {
            System.out.println("Review rejected: " + e.getMessage());
        } finally {
            System.out.println("(review step finished)");
        }
    }

    private void requestSkill() {
        String skillName = ask("Which skill do you want to learn");
        SkillRequest request = new SkillRequest(currentStudent.getId(),
                currentStudent.getName(), skillName);
        app.getRequestQueue().submit(request);
        System.out.println("Request " + request.getRequestId()
                + " added to the queue (" + app.getRequestQueue().size()
                + " waiting). The background MatchingThread will answer it.");
    }

    // ==================================================================

    private Session findSessionById(String id) {
        for (Session session : app.getSessionRepository().getAll()) {
            if (session.getSessionId().equalsIgnoreCase(id)) {
                return session;
            }
        }
        return null;
    }

    private String ask(String prompt) {
        System.out.print(prompt + ": ");
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    private int askInt(String prompt) {
        while (true) {
            String text = ask(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                // NumberFormatException is an UNCHECKED exception from the JDK
                System.out.println("   '" + text + "' is not a number, try again.");
            }
        }
    }

    private void header(String text) {
        System.out.println();
        System.out.println("================================================================");
        System.out.println(" " + text);
        System.out.println("================================================================");
    }

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
    }
}
