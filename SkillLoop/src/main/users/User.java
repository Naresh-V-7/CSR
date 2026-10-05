package skillloop.users;

/**
 * ABSTRACT CLASS - the common parent of Student and Admin.
 *
 * Why abstract?
 *   A "User" on its own is not a real thing in SkillLoop: every real user is
 *   either a Student or an Admin. Making the class abstract means nobody can
 *   write  new User(...)  by mistake, but both children still inherit the id,
 *   name, email and login handling written here exactly once.
 *
 * Concepts shown here: abstract class, abstract method, inheritance,
 * static members, constructors, "this", protected fields.
 */
public abstract class User {

    /** Application-wide constants (static final = one copy, cannot change). */
    public static final String APP_NAME = "SkillLoop";
    public static final String APP_VERSION = "1.0";

    /** static counter used to generate unique ids - shared by ALL users. */
    private static int idCounter = 100;

    /** static count of how many users are registered right now. */
    private static int totalUsers = 0;

    /** protected: visible to the subclasses Student and Admin. */
    protected final int id;
    protected String name;
    protected String email;
    protected String password;

    /**
     * Constructor of the parent class.
     * Student and Admin call it with super(...) - that is CONSTRUCTOR INHERITANCE.
     */
    protected User(String name, String email, String password) {
        this.id = generateId();      // static method used inside a constructor
        this.name = name;            // "this.name" = field, "name" = parameter
        this.email = email;
        this.password = password;
        totalUsers++;
    }

    /** synchronized so two threads registering at the same time cannot get the same id. */
    private static synchronized int generateId() {
        idCounter++;
        return idCounter;
    }

    /**
     * ABSTRACT METHOD: no body here.
     * Every subclass MUST provide its own version. This is the method we use in
     * Main.java to demonstrate runtime polymorphism.
     */
    public abstract void showDashboard();

    /**
     * Another abstract method: each type of user describes its own role.
     */
    public abstract String getRole();

    /** Ordinary inherited method - both Student and Admin use it unchanged. */
    public boolean login(String emailAttempt, String passwordAttempt) {
        return this.email.equalsIgnoreCase(emailAttempt)
                && this.password.equals(passwordAttempt);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public static int getTotalUsers() {
        return totalUsers;
    }

    /** Only used by the unit tests so each test starts from a clean state. */
    public static void resetCounters() {
        idCounter = 100;
        totalUsers = 0;
    }

    /** Overriding Object.toString(); Student overrides it again with more detail. */
    @Override
    public String toString() {
        return "[" + id + "] " + name + " (" + getRole() + ")";
    }
}
