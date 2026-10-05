package skillloop.threads;

import java.util.LinkedList;
import java.util.Queue;

import skillloop.services.NotificationService;
import skillloop.users.Student;

/**
 * BACKGROUND THREAD 2 - delivers notifications without blocking the UI.
 *
 * A message is put into the outbox by whichever part of the app produces it, and
 * this worker sends them one by one. In a real product the sending step would be
 * an e-mail or SMS call, which is slow - exactly the kind of work that belongs
 * on a background thread.
 */
public class NotificationThread implements Runnable {

    /** One item of the outbox. */
    private static class Message {
        final Student to;
        final String text;

        Message(Student to, String text) {
            this.to = to;
            this.text = text;
        }
    }

    private final Queue<Message> outbox = new LinkedList<>();
    private final NotificationService notificationService;
    private volatile boolean running = true;
    private int deliveredCount = 0;

    public NotificationThread(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * SYNCHRONIZED: the main thread adds messages while this thread removes them,
     * so both methods must lock the same object (this NotificationThread).
     */
    public synchronized void queueMessage(Student student, String text) {
        outbox.offer(new Message(student, text));
    }

    private synchronized Message takeMessage() {
        return outbox.poll();
    }

    public synchronized int pendingCount() {
        return outbox.size();
    }

    @Override
    public void run() {
        System.out.println("[NotificationThread] started as '" + Thread.currentThread().getName() + "'");
        while (running) {
            Message message = takeMessage();
            if (message == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
                continue;
            }
            notificationService.notify(message.to, message.text);
            deliveredCount++;
        }
        System.out.println("[NotificationThread] stopped after " + deliveredCount + " message(s)");
    }

    public void stop() {
        this.running = false;
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }
}
