package skillloop.testing;

import java.util.ArrayList;
import java.util.List;

import skillloop.services.NotificationService;
import skillloop.users.Student;

/**
 * MOCKING - written by hand, with no mocking library.
 *
 * The real ConsoleNotificationService prints on the screen (in a real product it
 * would send an e-mail). A unit test must not depend on that, and it should be
 * able to CHECK that the notification was sent.
 *
 * This fake implements the same NotificationService interface but only records
 * the messages in a list, so a test can assert:
 *
 *      assertEquals(2, mock.getSentCount());
 *      assertTrue(mock.wasNotified(student, "Seat confirmed"));
 *
 * BookingService accepts it without any change because it depends on the
 * INTERFACE, not on the console class - that is dependency injection paying off.
 */
public class MockNotificationService implements NotificationService {

    /** One recorded call to notify(). */
    public static class SentMessage {
        public final Student to;
        public final String text;

        SentMessage(Student to, String text) {
            this.to = to;
            this.text = text;
        }
    }

    private final List<SentMessage> sentMessages = new ArrayList<>();

    @Override
    public void notify(Student student, String message) {
        sentMessages.add(new SentMessage(student, message));   // recorded, not printed
    }

    @Override
    public int getSentCount() {
        return sentMessages.size();
    }

    public List<SentMessage> getSentMessages() {
        return sentMessages;
    }

    /** True when this student received a message containing the given text. */
    public boolean wasNotified(Student student, String partOfMessage) {
        for (SentMessage message : sentMessages) {
            if (message.to.getId() == student.getId()
                    && message.text.contains(partOfMessage)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        sentMessages.clear();
    }
}
