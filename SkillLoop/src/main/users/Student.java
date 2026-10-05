package skillloop.users;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import skillloop.credits.SkillWallet;
import skillloop.skills.Skill;

/**
 * A student of SkillLoop: the main actor of the application.
 *
 * Concepts shown here:
 *   - INHERITANCE            : Student extends User
 *   - MULTIPLE INHERITANCE   : implements Teachable, Learnable, Bookable
 *   - METHOD OVERRIDING      : showDashboard(), getRole(), toString()
 *   - VARARGS                : addSkills("Java", "C", "Python")
 *   - ARRAYS                 : ratings[] and availabilitySlots[]
 *   - Math class             : average rating calculation
 *   - Set                    : prevents duplicate skills
 *   - Comparable             : lets a TreeSet sort students by rating
 */
public class Student extends User implements Teachable, Learnable, Bookable, Comparable<Student> {

    /** A student keeps at most this many ratings - the size of the ratings array. */
    public static final int MAX_RATINGS = 20;

    /** Fixed number of weekly availability slots (Mon..Sun mornings/evenings). */
    public static final int SLOT_COUNT = 7;

    private String college;

    /**
     * Set (LinkedHashSet) - chosen because a student must NOT be able to add the
     * same skill twice. Skill.equals() compares names, so "java" and "Java" are
     * treated as one skill. LinkedHashSet also keeps the insertion order, which
     * makes the dashboard predictable.
     */
    private final Set<Skill> teachingSkills = new LinkedHashSet<>();
    private final Set<Skill> learningSkills = new LinkedHashSet<>();

    /** ARRAY - a fixed-size store of the last MAX_RATINGS ratings received. */
    private final int[] ratings = new int[MAX_RATINGS];
    private int ratingCount = 0;

    /** ARRAY - one free/busy flag per day of the week. */
    private final String[] availabilitySlots = new String[SLOT_COUNT];

    private final SkillWallet wallet;

    /** ArrayList of session ids - a student can hold several seats. */
    private final List<String> bookedSessionIds = new ArrayList<>();
    private final List<String> waitingSessionIds = new ArrayList<>();

    /**
     * Main constructor. super(...) runs the User constructor first, so the id,
     * name, email and password are initialised by the parent class.
     */
    public Student(String name, String email, String password, String college) {
        super(name, email, password);
        this.college = college;
        this.wallet = new SkillWallet(this.id);   // every student owns one wallet
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.availabilitySlots[i] = "free";
        }
    }

    /** Constructor overloading - quick registration without a college name. */
    public Student(String name, String email, String password) {
        this(name, email, password, "Not specified");
    }

    // ------------------------------------------------------------------
    // Teachable
    // ------------------------------------------------------------------

    @Override
    public void addTeachingSkill(Skill skill) {
        teachingSkills.add(skill);       // Set silently ignores a duplicate
    }

    @Override
    public boolean teaches(String skillName) {
        for (Skill skill : teachingSkills) {
            if (skill.getName().equalsIgnoreCase(skillName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Skill> getTeachingSkills() {
        return teachingSkills;
    }

    // ------------------------------------------------------------------
    // Learnable
    // ------------------------------------------------------------------

    @Override
    public void addLearningSkill(Skill skill) {
        learningSkills.add(skill);
    }

    @Override
    public boolean wantsToLearn(String skillName) {
        for (Skill skill : learningSkills) {
            if (skill.getName().equalsIgnoreCase(skillName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Skill> getLearningSkills() {
        return learningSkills;
    }

    // ------------------------------------------------------------------
    // Bookable
    // ------------------------------------------------------------------

    @Override
    public boolean canAfford(int credits) {
        return wallet.canAfford(credits);
    }

    @Override
    public void addBookedSession(String sessionId) {
        bookedSessionIds.add(sessionId);
    }

    @Override
    public void addWaitingSession(String sessionId) {
        waitingSessionIds.add(sessionId);
    }

    @Override
    public List<String> getBookedSessionIds() {
        return bookedSessionIds;
    }

    @Override
    public List<String> getWaitingSessionIds() {
        return waitingSessionIds;
    }

    // ------------------------------------------------------------------
    // VARARGS
    // ------------------------------------------------------------------

    /**
     * VARARGS: "String... names" means "zero or more String arguments".
     *
     * Without varargs the UI would have to build an array or call addTeachingSkill()
     * in a loop. With varargs the demo can simply write:
     *      studentA.addSkills("Java", "C", "Python");
     *
     * Inside the method, "names" behaves exactly like a String[] array.
     */
    public void addSkills(String... names) {
        for (String name : names) {
            addTeachingSkill(new Skill(name));
        }
    }

    /**
     * Varargs version that also sets a category for every skill.
     *
     * Note the different NAME: two varargs methods called addSkills(String...)
     * and addSkills(String, String...) would be ambiguous for the compiler when
     * two arguments are passed, so overloading and varargs must be mixed with
     * care. This is a good point to mention in the viva.
     */
    public void addSkillsInCategory(String category, String... names) {
        for (String name : names) {
            addTeachingSkill(new Skill(name, category));
        }
    }

    /** Varargs version for the "skills I want to learn" list. */
    public void addWantedSkills(String... names) {
        for (String name : names) {
            addLearningSkill(new Skill(name));
        }
    }

    // ------------------------------------------------------------------
    // Ratings - ARRAY + Math
    // ------------------------------------------------------------------

    /** Store one rating (1..5) in the ratings array. */
    public void addRating(int rating) {
        if (ratingCount < MAX_RATINGS) {
            ratings[ratingCount] = rating;
            ratingCount++;
        } else {
            // array full: shift left by one, drop the oldest rating
            for (int i = 1; i < MAX_RATINGS; i++) {
                ratings[i - 1] = ratings[i];
            }
            ratings[MAX_RATINGS - 1] = rating;
        }
    }

    /**
     * Average rating, rounded to one decimal place.
     * Math.round() is used here because a rating like 4.333333 looks bad on the
     * dashboard; Math.max() protects the division against ratingCount == 0.
     */
    public double getAverageRating() {
        if (ratingCount == 0) {
            return 0.0;
        }
        int total = 0;
        for (int i = 0; i < ratingCount; i++) {
            total += ratings[i];
        }
        double average = (double) total / Math.max(1, ratingCount);
        return Math.round(average * 10.0) / 10.0;     // 4.333 -> 4.3
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int[] getRatings() {
        return ratings;
    }

    // ------------------------------------------------------------------
    // Availability - ARRAY
    // ------------------------------------------------------------------

    public void setAvailability(int dayIndex, String status) {
        if (dayIndex >= 0 && dayIndex < SLOT_COUNT) {
            availabilitySlots[dayIndex] = status;
        }
    }

    public String[] getAvailabilitySlots() {
        return availabilitySlots;
    }

    public SkillWallet getWallet() {
        return wallet;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    // ------------------------------------------------------------------
    // Overriding
    // ------------------------------------------------------------------

    @Override
    public String getRole() {
        return "STUDENT";
    }

    /**
     * METHOD OVERRIDING: User declares showDashboard() as abstract; here the
     * Student gives its own student-specific version. Admin gives a different one.
     */
    @Override
    public void showDashboard() {
        System.out.println();
        System.out.println("=========== STUDENT DASHBOARD ===========");
        System.out.println(" Name           : " + name + "  (ID " + id + ")");
        System.out.println(" College        : " + college);
        System.out.println(" Skill Credits  : " + wallet.getBalance());
        System.out.println(" Rating         : " + getAverageRating()
                + " / 5  (" + ratingCount + " reviews)");
        System.out.println(" I can teach    : " + skillNames(teachingSkills));
        System.out.println(" I want to learn: " + skillNames(learningSkills));
        System.out.println(" Booked sessions: " + bookedSessionIds);
        System.out.println(" Waiting list   : " + waitingSessionIds);
        System.out.println("=========================================");
    }

    private String skillNames(Set<Skill> skills) {
        if (skills.isEmpty()) {
            return "(none yet)";
        }
        StringBuilder text = new StringBuilder();
        for (Skill skill : skills) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(skill.getName());
        }
        return text.toString();
    }

    /**
     * Comparable: used by TreeSet to keep a "leaderboard" of students sorted by
     * rating (highest first). When two ratings are equal we fall back to the id
     * so that no student is dropped from the TreeSet.
     */
    @Override
    public int compareTo(Student other) {
        int byRating = Double.compare(other.getAverageRating(), this.getAverageRating());
        if (byRating != 0) {
            return byRating;
        }
        return Integer.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return "[" + id + "] " + name + " | teaches: " + skillNames(teachingSkills)
                + " | wants: " + skillNames(learningSkills)
                + " | credits: " + wallet.getBalance();
    }
}
