package skillloop;

import java.io.IOException;

import skillloop.collections.Repository;
import skillloop.collections.RequestQueue;
import skillloop.matching.MatchingService;
import skillloop.matching.SkillCycleMatcher;
import skillloop.services.BookingService;
import skillloop.services.ConsoleNotificationService;
import skillloop.services.NotificationService;
import skillloop.services.ReviewService;
import skillloop.services.SkillService;
import skillloop.services.StudentService;
import skillloop.sessions.Review;
import skillloop.sessions.Session;
import skillloop.skills.Skill;
import skillloop.storage.FileStorage;
import skillloop.users.Admin;
import skillloop.users.Student;

/**
 * The "composition root" of SkillLoop: the single place where every object of
 * the application is created and wired together.
 *
 * This class is the clearest example of DEPENDENCY INJECTION in the project:
 *   - the repositories are created here,
 *   - every service receives what it needs through its CONSTRUCTOR,
 *   - no service ever creates its own dependencies with "new".
 *
 * Because of that, the unit tests can build the same services with different
 * (smaller, or fake) parts - see BookingServiceTest and MockNotificationService.
 *
 * Layers:   UI  ->  Services  ->  Model  ->  Repository / FileStorage
 */
public class SkillLoopApp {

    // --- Repository layer (generic class used four times) ------------------
    private final Repository<Student> studentRepository = new Repository<>();
    private final Repository<Skill> skillRepository = new Repository<>();
    private final Repository<Session> sessionRepository = new Repository<>();
    private final Repository<Review> reviewRepository = new Repository<>();

    /** Queue of pending "I want to learn X" requests. */
    private final RequestQueue requestQueue = new RequestQueue();

    // --- Service layer -----------------------------------------------------
    private final NotificationService notificationService;
    private final StudentService studentService;
    private final SkillService skillService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final MatchingService matchingService;
    private final SkillCycleMatcher skillCycleMatcher;

    // --- Storage layer -----------------------------------------------------
    private final FileStorage fileStorage;

    /** The platform administrator (used for the polymorphism demo). */
    private final Admin admin;

    public SkillLoopApp() {
        this(new ConsoleNotificationService(), new FileStorage("data"));
    }

    /**
     * The interesting constructor: BOTH the notification service and the storage
     * come from outside. Main.java passes the real ones; a test can pass a mock
     * notifier and a temporary folder.
     */
    public SkillLoopApp(NotificationService notificationService, FileStorage fileStorage) {
        this.notificationService = notificationService;
        this.fileStorage = fileStorage;

        // every service gets its dependencies through its constructor
        this.studentService = new StudentService(studentRepository);
        this.skillService = new SkillService(skillRepository);
        this.bookingService = new BookingService(sessionRepository, notificationService);
        this.reviewService = new ReviewService(reviewRepository, notificationService);
        this.matchingService = new MatchingService(studentRepository);
        this.skillCycleMatcher = new SkillCycleMatcher(studentRepository);

        this.admin = new Admin("Prof. Anand", "admin@skillloop.edu", "admin123", "Computer Science");
    }

    /** Create a session and register it in the repository. */
    public Session createSession(Student teacher, String skillName, int seats,
                                 int hours, String date) {
        Skill skill = new Skill(skillName);
        Session session = new Session(skill, teacher, seats, hours, date);
        sessionRepository.add(session);
        return session;
    }

    /** Save everything to the data folder. */
    public void saveAll() {
        try {
            fileStorage.saveAll(studentRepository.getAll(), skillRepository.getAll(),
                    sessionRepository.getAll(), reviewRepository.getAll());
            System.out.println("[storage] all data written to '" + fileStorage.getDataFolder() + "/'");
        } catch (IOException e) {
            System.out.println("[storage] could not save: " + e.getMessage());
        } finally {
            fileStorage.appendLog("saveAll() finished");
        }
    }

    // --- getters used by the UI and the tests ------------------------------

    public Repository<Student> getStudentRepository() {
        return studentRepository;
    }

    public Repository<Skill> getSkillRepository() {
        return skillRepository;
    }

    public Repository<Session> getSessionRepository() {
        return sessionRepository;
    }

    public Repository<Review> getReviewRepository() {
        return reviewRepository;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public StudentService getStudentService() {
        return studentService;
    }

    public SkillService getSkillService() {
        return skillService;
    }

    public BookingService getBookingService() {
        return bookingService;
    }

    public ReviewService getReviewService() {
        return reviewService;
    }

    public MatchingService getMatchingService() {
        return matchingService;
    }

    public SkillCycleMatcher getSkillCycleMatcher() {
        return skillCycleMatcher;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
    }

    public Admin getAdmin() {
        return admin;
    }
}
