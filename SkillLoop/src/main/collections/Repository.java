package skillloop.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * GENERIC CLASS: a simple in-memory store that works for ANY type.
 *
 *      Repository<Student> studentRepo = new Repository<>();
 *      Repository<Skill>   skillRepo   = new Repository<>();
 *      Repository<Session> sessionRepo = new Repository<>();
 *
 * Why generics?
 *   Without <T> we would need StudentRepository, SkillRepository and
 *   SessionRepository containing exactly the same code, and we would have to
 *   cast every object we read back (ArrayList of Object). With <T> the compiler
 *   checks the type for us: studentRepo.add(someSkill) does not even compile.
 *
 * "T" is a type parameter - it is replaced by the real type when the object is
 * created.
 */
public class Repository<T> {

    /** ArrayList: fast random access and easy growth - perfect for a list of records. */
    private final List<T> items = new ArrayList<>();

    /** Generic method parameter: T is whatever type this repository stores. */
    public void add(T item) {
        items.add(item);
    }

    public T get(int index) {
        return items.get(index);
    }

    public boolean remove(T item) {
        return items.remove(item);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<T> getAll() {
        return items;
    }

    public void clear() {
        items.clear();
    }

    /**
     * GENERIC + LAMBDA: find every item that satisfies a condition.
     * The condition arrives as a lambda, e.g.
     *      repo.findBy(student -> student.teaches("Java"))
     */
    public List<T> findBy(Predicate<T> condition) {
        List<T> matches = new ArrayList<>();
        for (T item : items) {
            if (condition.test(item)) {
                matches.add(item);
            }
        }
        return matches;
    }

    /**
     * ITERATOR: removing while looping with a normal for-each throws
     * ConcurrentModificationException. Iterator.remove() is the safe way.
     */
    public int removeWhere(Predicate<T> condition) {
        int removed = 0;
        Iterator<T> iterator = items.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (condition.test(item)) {
                iterator.remove();       // safe removal during traversal
                removed++;
            }
        }
        return removed;
    }

    /**
     * GENERIC METHOD (its own type parameter <E>, independent of the class).
     * Used by the services to print any list in a numbered format.
     */
    public static <E> void printAll(String title, List<E> list) {
        System.out.println("--- " + title + " (" + list.size() + ") ---");
        int number = 1;
        for (E element : list) {
            System.out.println(" " + number + ". " + element);
            number++;
        }
        if (list.isEmpty()) {
            System.out.println(" (nothing to show)");
        }
    }
}
