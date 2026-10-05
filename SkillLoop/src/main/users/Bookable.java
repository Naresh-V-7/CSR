package skillloop.users;

import java.util.List;

/**
 * INTERFACE: "anyone who can take part in a session booking".
 *
 * Student implements Teachable, Learnable AND Bookable at the same time.
 * That is how Java gives us MULTIPLE INHERITANCE (of type): one class can have
 * only one parent class, but it may implement any number of interfaces.
 */
public interface Bookable {

    /** True when the wallet has enough credits for this cost. */
    boolean canAfford(int credits);

    /** Remember that this student holds a seat in a session. */
    void addBookedSession(String sessionId);

    /** Remember that this student is waiting for a seat in a session. */
    void addWaitingSession(String sessionId);

    /** Sessions where the student already has a confirmed seat. */
    List<String> getBookedSessionIds();

    /** Sessions where the student is on the waiting list. */
    List<String> getWaitingSessionIds();
}
