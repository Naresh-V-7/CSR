package skillloop.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import skillloop.collections.Repository;
import skillloop.users.Student;

/**
 * Registration, login and lookup of students.
 *
 * MAP:
 *   students are kept twice on purpose -
 *     - in a Repository<Student> (an ArrayList) when we need the whole list, and
 *     - in a HashMap<Integer, Student> for instant lookup by id.
 *   Searching the list would be O(n); the map answers "who is student 103?"
 *   in one step, which is what every screen of the UI needs.
 */
public class StudentService {

    private final Repository<Student> studentRepository;

    /** MAP: student id -> Student object. */
    private final Map<Integer, Student> studentsById = new HashMap<>();

    /** MAP: e-mail -> Student object, used by the login screen. */
    private final Map<String, Student> studentsByEmail = new HashMap<>();

    /** DEPENDENCY INJECTION. */
    public StudentService(Repository<Student> studentRepository) {
        this.studentRepository = studentRepository;
    }

    /** Create and store a new student. */
    public Student register(String name, String email, String password, String college) {
        Student student = new Student(name, email, password, college);
        save(student);
        return student;
    }

    /** Overloaded quick registration. */
    public Student register(String name, String email, String password) {
        return register(name, email, password, "Not specified");
    }

    /** Used both by register() and by the file loader. */
    public void save(Student student) {
        studentRepository.add(student);
        studentsById.put(student.getId(), student);
        studentsByEmail.put(student.getEmail().toLowerCase(), student);
    }

    /** Login: returns the student, or null when the credentials are wrong. */
    public Student login(String email, String password) {
        Student student = studentsByEmail.get(email.toLowerCase());
        if (student != null && student.login(email, password)) {
            return student;
        }
        return null;
    }

    /** MAP lookup - O(1) instead of scanning the whole list. */
    public Student findById(int id) {
        return studentsById.get(id);
    }

    public Student findByEmail(String email) {
        return studentsByEmail.get(email.toLowerCase());
    }

    /** LAMBDA: search students by (part of) their name. */
    public List<Student> searchByName(String partOfName) {
        return studentRepository.findBy(
                student -> student.getName().toLowerCase().contains(partOfName.toLowerCase()));
    }

    /**
     * ITERATOR: remove every student who never registered a single skill.
     * A for-each loop would throw ConcurrentModificationException here, which is
     * exactly why Iterator.remove() exists.
     */
    public int removeEmptyProfiles() {
        int removed = 0;
        Iterator<Student> iterator = studentRepository.getAll().iterator();
        while (iterator.hasNext()) {
            Student student = iterator.next();
            if (student.getTeachingSkills().isEmpty() && student.getLearningSkills().isEmpty()) {
                iterator.remove();
                studentsById.remove(student.getId());
                studentsByEmail.remove(student.getEmail().toLowerCase());
                removed++;
            }
        }
        return removed;
    }

    public List<Student> getAllStudents() {
        return studentRepository.getAll();
    }

    public Map<Integer, Student> getStudentsById() {
        return studentsById;
    }

    public Repository<Student> getStudentRepository() {
        return studentRepository;
    }

    public int getStudentCount() {
        return studentRepository.size();
    }
}
