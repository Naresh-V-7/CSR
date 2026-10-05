package skillloop;

import java.util.Scanner;

import skillloop.services.ConsoleNotificationService;
import skillloop.storage.FileStorage;
import skillloop.ui.ConsoleUI;
import skillloop.ui.DemoRunner;
import skillloop.users.Admin;
import skillloop.users.Student;
import skillloop.users.User;

/**
 * Entry point of SkillLoop.
 *
 *   Option 1 - runs the complete project demonstration (use this in the review)
 *   Option 2 - opens the interactive menu with the demo data already loaded
 *   Option 3 - opens the interactive menu with an empty platform
 *
 * The first thing main() does is the RUNTIME POLYMORPHISM demonstration, so it
 * is visible even before any menu appears.
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("Welcome to " + User.APP_NAME + " v" + User.APP_VERSION);

        // ==============================================================
        // RUNTIME POLYMORPHISM / DYNAMIC BINDING
        // One reference of the PARENT type, two different CHILD objects.
        // Java decides at RUNTIME which showDashboard() to call.
        // ==============================================================
        User user;

        user = new Student("Demo Student", "demo@college.edu", "demo123", "CSE");
        user.showDashboard();                 // -> Student.showDashboard()

        user = new Admin("Demo Admin", "admin@college.edu", "admin123");
        user.showDashboard();                 // -> Admin.showDashboard()

        // ==============================================================

        SkillLoopApp app = new SkillLoopApp(new ConsoleNotificationService(), new FileStorage("data"));
        Scanner scanner = new Scanner(System.in);

        System.out.println();
        System.out.println("What would you like to do?");
        System.out.println("  1. Run the full project demo (recommended for the review)");
        System.out.println("  2. Demo data + interactive menu");
        System.out.println("  3. Empty platform + interactive menu");
        System.out.print("Choice: ");

        String choice = scanner.hasNextLine() ? scanner.nextLine().trim() : "1";

        DemoRunner demo = new DemoRunner(app);
        switch (choice) {
            case "2":
                demo.runFullDemo();
                ConsoleUI uiWithData = new ConsoleUI(app, scanner);
                uiWithData.setCurrentStudent(demo.getStudentA());
                uiWithData.start();
                break;
            case "3":
                new ConsoleUI(app, scanner).start();
                break;
            case "1":
            default:
                demo.runFullDemo();
                break;
        }

        scanner.close();
    }
}
