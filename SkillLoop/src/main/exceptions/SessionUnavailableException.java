package skillloop.exceptions;

/**
 * Thrown when a session cannot be booked: it is full, cancelled, already
 * completed, or the student is trying to book their own session.
 *
 * CHECKED exception, because "the session is full" is an expected outcome that
 * the booking screen must handle (it puts the student on the waiting list).
 */
public class SessionUnavailableException extends SkillLoopException {

    private final String sessionId;

    public SessionUnavailableException(String sessionId, String reason) {
        super("Session " + sessionId + " is unavailable: " + reason);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
