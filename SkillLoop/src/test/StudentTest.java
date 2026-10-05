package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import skillloop.exceptions.InvalidSkillLevelException;
import skillloop.skills.Skill;
import skillloop.users.Student;
import skillloop.users.User;

/**
 * JUNIT 5 tests for the Student class.
 *
 * The five life-cycle annotations are all used here on purpose, so the order in
 * which JUnit calls them can be shown during the viva:
 *
 *   @BeforeAll   once, before the very first test   (static)
 *   @BeforeEach  before EVERY test
 *   @Test        the test itself
 *   @AfterEach   after EVERY test
 *   @AfterAll    once, after the last test          (static)
 */
class StudentTest {

    private Student student;

    @BeforeAll
    static void setUpClass() {
        System.out.println("@BeforeAll  -> starting the Student tests");
    }

    @BeforeEach
    void setUp() {
        // a fresh student for every test, so tests never influence each other
        User.resetCounters();
        student = new Student("Ravi", "ravi@college.edu", "pass123", "CSE");
    }

    @AfterEach
    void tearDown() {
        student = null;
    }

    @AfterAll
    static void tearDownClass() {
        System.out.println("@AfterAll   -> Student tests finished");
    }

    @Test
    @DisplayName("A new student gets an id, a name and the welcome credits")
    void studentIsCreatedCorrectly() {
        assertEquals("Ravi", student.getName());
        assertEquals("CSE", student.getCollege());
        assertEquals(20, student.getWallet().getBalance());   // WELCOME_CREDITS
        assertTrue(student.getId() > 0);
        assertEquals("STUDENT", student.getRole());
    }

    @Test
    @DisplayName("Skills can be added and are found again")
    void skillCanBeAdded() {
        student.addTeachingSkill(new Skill("Java", "Programming", 4));
        assertEquals(1, student.getTeachingSkills().size());
        assertTrue(student.teaches("Java"));
        assertTrue(student.teaches("java"));            // search is case-insensitive
        assertFalse(student.teaches("Python"));
    }

    @Test
    @DisplayName("The Set blocks a duplicate skill, even with different spelling")
    void duplicateSkillIsPrevented() {
        student.addTeachingSkill(new Skill("Java", "Programming", 4));
        student.addTeachingSkill(new Skill("Java", "Programming", 5));
        student.addTeachingSkill(new Skill("java", "Other", 2));

        assertEquals(1, student.getTeachingSkills().size(),
                "Skill.equals() compares names, so the Set keeps only one Java");
    }

    @Test
    @DisplayName("Varargs: several skills in one call")
    void varargsAddsManySkills() {
        student.addSkills("Java", "C", "Python");
        assertEquals(3, student.getTeachingSkills().size());
        assertTrue(student.teaches("C"));

        student.addWantedSkills("Data Structures", "SQL");
        assertEquals(2, student.getLearningSkills().size());
        assertTrue(student.wantsToLearn("SQL"));
    }

    @Test
    @DisplayName("An invalid skill level throws the unchecked InvalidSkillLevelException")
    void invalidLevelIsRejected() {
        // assertThrows is the JUnit 5 way of testing that an exception happens
        InvalidSkillLevelException thrown =
                assertThrows(InvalidSkillLevelException.class, () -> new Skill("Java", "Programming", 9));
        assertEquals(9, thrown.getInvalidLevel());

        assertThrows(InvalidSkillLevelException.class, () -> new Skill("Java", "Programming", 0));
    }

    @Test
    @DisplayName("The ratings array and Math.round produce the average rating")
    void averageRatingIsRounded() {
        assertEquals(0.0, student.getAverageRating());     // no reviews yet

        student.addRating(5);
        student.addRating(4);
        student.addRating(4);
        // (5+4+4)/3 = 4.3333 -> Math.round to one decimal -> 4.3
        assertEquals(4.3, student.getAverageRating());
        assertEquals(3, student.getRatingCount());
    }

    @Test
    @DisplayName("Student implements Teachable, Learnable and Bookable")
    void studentImplementsAllInterfaces() {
        assertTrue(student instanceof skillloop.users.Teachable);
        assertTrue(student instanceof skillloop.users.Learnable);
        assertTrue(student instanceof skillloop.users.Bookable);
        assertTrue(student instanceof User);               // and inherits from User
    }
}
