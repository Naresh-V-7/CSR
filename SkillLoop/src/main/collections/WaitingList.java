package skillloop.collections;

import java.util.Iterator;
import java.util.LinkedList;

import skillloop.users.Student;

/**
 * The waiting list of ONE session.
 *
 * Why LinkedList?
 *   A waiting list only ever has items added at the END and removed from the
 *   FRONT ("the next student gets the free seat"). LinkedList does both in
 *   constant time, and it is the standard Java implementation of the Queue
 *   interface, which is exactly the behaviour we want here.
 */
public class WaitingList {

    private final LinkedList<Student> queue = new LinkedList<>();

    /** Add a student at the end of the waiting list. */
    public void join(Student student) {
        if (!queue.contains(student)) {
            queue.addLast(student);
        }
    }

    /** Give the seat to the student who has waited the longest (FIFO). */
    public Student callNext() {
        return queue.pollFirst();       // returns null when the list is empty
    }

    /** Position shown in the UI: 1 means "you are next". */
    public int positionOf(Student student) {
        int position = 1;
        for (Student waiting : queue) {
            if (waiting.getId() == student.getId()) {
                return position;
            }
            position++;
        }
        return -1;                       // not on this waiting list
    }

    /**
     * ITERATOR used for safe removal: a student who cancels must be taken out of
     * the middle of the list while we are traversing it.
     */
    public boolean leave(Student student) {
        Iterator<Student> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Student waiting = iterator.next();
            if (waiting.getId() == student.getId()) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public LinkedList<Student> getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        if (queue.isEmpty()) {
            return "(waiting list empty)";
        }
        StringBuilder text = new StringBuilder();
        int position = 1;
        for (Student student : queue) {
            text.append(position).append(") ").append(student.getName()).append("  ");
            position++;
        }
        return text.toString().trim();
    }
}
