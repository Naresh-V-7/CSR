package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import skillloop.collections.Repository;
import skillloop.matching.SkillCycle;
import skillloop.matching.SkillCycleMatcher;
import skillloop.skills.Skill;
import skillloop.users.Student;
import skillloop.users.User;

/**
 * JUnit 5 tests for the innovative feature: SKILL CYCLE MATCHING.
 *
 * The classic example of the project is built here:
 *      A teaches Java  and wants Python
 *      B teaches Python and wants C
 *      C teaches C     and wants Java
 * which must be detected as the cycle  A -> B -> C -> A.
 */
class SkillCycleMatcherTest {

    private Repository<Student> studentRepository;
    private SkillCycleMatcher matcher;
    private Student a;
    private Student b;
    private Student c;

    @BeforeEach
    void setUp() {
        User.resetCounters();
        studentRepository = new Repository<>();
        matcher = new SkillCycleMatcher(studentRepository);

        a = new Student("A", "a@c.edu", "p", "CSE");
        a.addTeachingSkill(new Skill("Java"));
        a.addLearningSkill(new Skill("Python"));

        b = new Student("B", "b@c.edu", "p", "CSE");
        b.addTeachingSkill(new Skill("Python"));
        b.addLearningSkill(new Skill("C"));

        c = new Student("C", "c@c.edu", "p", "CSE");
        c.addTeachingSkill(new Skill("C"));
        c.addLearningSkill(new Skill("Java"));

        studentRepository.add(a);
        studentRepository.add(b);
        studentRepository.add(c);
    }

    @Test
    @DisplayName("The three-student cycle A -> B -> C -> A is detected")
    void detectsThreeStudentCycle() {
        List<SkillCycle> cycles = matcher.findAllCycles();

        assertEquals(1, cycles.size(), "exactly one cycle, counted only once");
        SkillCycle cycle = cycles.get(0);
        assertEquals(3, cycle.size());
        assertTrue(cycle.describe().contains("--Java-->"));
        assertTrue(cycle.describe().contains("--Python-->"));
        assertTrue(cycle.describe().contains("--C-->"));
    }

    @Test
    @DisplayName("Every member of the cycle can see it")
    void everyMemberBelongsToTheCycle() {
        assertEquals(1, matcher.findCyclesFor(a).size());
        assertEquals(1, matcher.findCyclesFor(b).size());
        assertEquals(1, matcher.findCyclesFor(c).size());
    }

    @Test
    @DisplayName("Two students who can teach each other form a direct exchange (cycle of 2)")
    void detectsDirectExchange() {
        Repository<Student> pairRepository = new Repository<>();
        Student x = new Student("X", "x@c.edu", "p", "CSE");
        x.addTeachingSkill(new Skill("Java"));
        x.addLearningSkill(new Skill("Python"));

        Student y = new Student("Y", "y@c.edu", "p", "CSE");
        y.addTeachingSkill(new Skill("Python"));
        y.addLearningSkill(new Skill("Java"));

        pairRepository.add(x);
        pairRepository.add(y);

        List<SkillCycle> cycles = new SkillCycleMatcher(pairRepository).findAllCycles();
        assertEquals(1, cycles.size());
        assertTrue(cycles.get(0).isDirectExchange());
        assertEquals(2, cycles.get(0).size());
    }

    @Test
    @DisplayName("No cycle is reported when the skills do not close a loop")
    void noCycleWhenTheLoopIsOpen() {
        Repository<Student> openRepository = new Repository<>();
        Student x = new Student("X", "x2@c.edu", "p", "CSE");
        x.addTeachingSkill(new Skill("Java"));
        x.addLearningSkill(new Skill("Python"));

        Student y = new Student("Y", "y2@c.edu", "p", "CSE");
        y.addTeachingSkill(new Skill("C"));            // nobody wants C
        y.addLearningSkill(new Skill("Java"));

        openRepository.add(x);
        openRepository.add(y);

        assertTrue(new SkillCycleMatcher(openRepository).findAllCycles().isEmpty());
    }

    @Test
    @DisplayName("findMultiPersonCycles() ignores the two-person exchanges")
    void multiPersonCyclesOnly() {
        List<SkillCycle> multi = matcher.findMultiPersonCycles();
        assertEquals(1, multi.size());
        assertEquals(3, multi.get(0).size());
    }
}
