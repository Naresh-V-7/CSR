package skillloop.collections;

import java.util.LinkedList;
import java.util.Queue;

import skillloop.sessions.SkillRequest;

/**
 * The queue of pending "I want to learn X" requests.
 *
 * Why a Queue?
 *   Requests must be answered in the order they arrived - FIFO (first in, first
 *   out) - so nobody who asked first is served last. Queue is the interface that
 *   expresses exactly this, and LinkedList is its simplest implementation.
 *
 * The background MatchingThread takes requests from this queue one by one.
 */
public class RequestQueue {

    /** Queue (interface) implemented by LinkedList (class). */
    private final Queue<SkillRequest> pendingRequests = new LinkedList<>();

    /** offer() adds at the tail of the queue. */
    public synchronized void submit(SkillRequest request) {
        pendingRequests.offer(request);
    }

    /** poll() removes and returns the head - null when the queue is empty. */
    public synchronized SkillRequest next() {
        return pendingRequests.poll();
    }

    /** peek() looks at the head without removing it. */
    public synchronized SkillRequest peek() {
        return pendingRequests.peek();
    }

    public synchronized boolean isEmpty() {
        return pendingRequests.isEmpty();
    }

    public synchronized int size() {
        return pendingRequests.size();
    }

    public Queue<SkillRequest> getPendingRequests() {
        return pendingRequests;
    }
}
