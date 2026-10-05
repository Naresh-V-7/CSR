package skillloop.services;

import skillloop.users.Student;

/**
 * The REAL notification service used by the running application: it prints the
 * message on the console (a college project does not need real e-mail/SMS).
 */
public class ConsoleNotificationService implements NotificationService {

    private int sentCount = 0;
    private final boolean silent;

    public ConsoleNotificationService() {
        this(false);
    }

    /** silent = true is used while loading demo data, to keep the screen clean. */
    public ConsoleNotificationService(boolean silent) {
        this.silent = silent;
    }

    @Override
    public void notify(Student student, String message) {
        sentCount++;
        if (!silent) {
            System.out.println("   [notification -> " + student.getName() + "] " + message);
        }
    }

    @Override
    public int getSentCount() {
        return sentCount;
    }
}
