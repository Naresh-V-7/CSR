package skillloop.sessions;

/**
 * The life-cycle of a learning session.
 * An enum is used instead of plain strings so a typo like "COMPLETD" cannot
 * compile.
 */
public enum SessionStatus {
    SCHEDULED,    // open for booking
    FULL,         // every seat taken, new students go to the waiting list
    COMPLETED,    // teaching finished, credits transferred, review possible
    CANCELLED,    // called off by the teacher
    EXPIRED       // its date passed without being completed (found by a thread)
}
