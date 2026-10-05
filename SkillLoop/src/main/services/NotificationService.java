package skillloop.services;

import skillloop.users.Student;

/**
 * INTERFACE for sending notifications.
 *
 * BookingService depends on this interface, never on a concrete class.
 * That is what makes MOCKING possible: in the unit tests we inject a fake
 * implementation (MockNotificationService) that only records the messages
 * instead of printing or e-mailing them.
 */
public interface NotificationService {

    /** Send a message to one student. */
    void notify(Student student, String message);

    /** How many messages were sent so far (handy for the tests). */
    int getSentCount();
}
