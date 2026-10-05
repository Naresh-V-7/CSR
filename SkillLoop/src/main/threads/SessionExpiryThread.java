package skillloop.threads;

import skillloop.collections.Repository;
import skillloop.sessions.Session;
import skillloop.sessions.SessionStatus;

/**
 * BACKGROUND THREAD 3 - housekeeping.
 *
 * Every few seconds it walks through the sessions and marks the ones whose date
 * has passed as EXPIRED, so they stop appearing in the booking list. Nobody has
 * to click anything for this to happen - that is the point of a background
 * thread.
 */
public class SessionExpiryThread implements Runnable {

    private final Repository<Session> sessionRepository;
    private final String today;                 // "2026-03-11" style date
    private final long checkIntervalMillis;

    private volatile boolean running = true;
    private int expiredCount = 0;

    public SessionExpiryThread(Repository<Session> sessionRepository, String today) {
        this(sessionRepository, today, 1000);
    }

    public SessionExpiryThread(Repository<Session> sessionRepository, String today,
                               long checkIntervalMillis) {
        this.sessionRepository = sessionRepository;
        this.today = today;
        this.checkIntervalMillis = checkIntervalMillis;
    }

    @Override
    public void run() {
        System.out.println("[SessionExpiryThread] started as '" + Thread.currentThread().getName() + "'");
        while (running) {
            expirePastSessions();
            try {
                Thread.sleep(checkIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        System.out.println("[SessionExpiryThread] stopped, " + expiredCount + " session(s) expired");
    }

    /** One sweep - also called directly by the tests, without any thread. */
    public int expirePastSessions() {
        int expiredNow = 0;
        for (Session session : sessionRepository.getAll()) {
            boolean stillOpen = session.getStatus() == SessionStatus.SCHEDULED
                    || session.getStatus() == SessionStatus.FULL;
            if (stillOpen && session.getScheduledDate().compareTo(today) < 0) {
                session.markExpired();
                expiredNow++;
                expiredCount++;
                System.out.println("   [expiry] " + session.getSessionId()
                        + " expired (was scheduled on " + session.getScheduledDate() + ")");
            }
        }
        return expiredNow;
    }

    public void stop() {
        this.running = false;
    }

    public int getExpiredCount() {
        return expiredCount;
    }
}
