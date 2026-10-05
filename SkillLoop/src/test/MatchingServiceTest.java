package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import skillloop.collections.Repository;
import skillloop.matching.MatchingService;
import skillloop.matching.SkillMatch;
import skillloop.matching.SkillMatcher;
import skillloop.skills.Skill;
import skillloop.users.Student;
import skillloop.users.User;

/** JUnit 5 tests for the matching engine (lambdas, functional interface, maps). */
class MatchingServiceTest {

    private Repository<Student> studentRepository;
    private MatchingService matchingService;
    private Student ravi;
    private Student meera;

    @BeforeEach
    void setUp() {
        User.resetCounters();
        studentRepository = new Repository<>();
        // DEPENDENCY INJECTION: a small repository built just for this test
        matchingService = new MatchingService(studentRepository);

        ravi = new Student("Ravi", "ravi@c.edu", "p", "CSE");
        ravi.addTeachingSkill(new Skill("Java", "Programming", 4));
        ravi.addLearningSkill(new Skill("Python"));

        meera = new Student("Meera", "meera@c.edu", "p", "IT");
        meera.addTeachingSkill(new Skill("Python", "Programming", 3));
        meera.addLearningSkill(new Skill("Java"));

        studentRepository.add(ravi);
        studentRepository.add(meera);
    }

    @Test
    @DisplayName("findTeachersOf() uses a lambda to filter the students")
    void findsTeachersOfASkill() {
        List<Student> javaTeachers = matchingService.findTeachersOf("Java");

        assertEquals(1, javaTeachers.size());
        assertEquals("Ravi", javaTeachers.get(0).getName());
        assertTrue(matchingService.findTeachersOf("Kotlin").isEmpty());
    }

    @Test
    @DisplayName("A lambda can be stored in the SkillMatcher functional interface")
    void functionalInterfaceAcceptsALambda() {
        SkillMatcher teachesPython = student -> student.teaches("Python");

        List<Student> found = matchingService.filter(teachesPython);
        assertEquals(1, found.size());
        assertEquals("Meera", found.get(0).getName());

        // default methods of the functional interface
        SkillMatcher notPython = teachesPython.negate();
        assertEquals("Ravi", matchingService.filter(notPython).get(0).getName());

        SkillMatcher pythonAndWantsJava = teachesPython.and(SkillMatcher.wantsSkill("Java"));
        assertEquals(1, matchingService.filter(pythonAndWantsJava).size());
    }

    @Test
    @DisplayName("Ravi and Meera are a direct (two-way) exchange")
    void detectsADirectExchange() {
        assertTrue(matchingService.isDirectExchange(ravi, meera));
        assertEquals(1, matchingService.findDirectExchangePartners(ravi).size());
    }

    @Test
    @DisplayName("A direct exchange scores higher than a one-way match")
    void scoreUsesTheMathClass() {
        List<SkillMatch> matches = matchingService.findMatchesFor(ravi);

        assertEquals(1, matches.size());
        SkillMatch best = matches.get(0);
        assertEquals("Meera", best.getTeacher().getName());
        assertTrue(best.isDirectExchange());
        // 50 base + 3*5 level + 0 rating + 20 direct = 85
        assertEquals(85, best.getScore());
        assertTrue(best.getScore() <= 100, "Math.min keeps the score at or below 100");
    }

    @Test
    @DisplayName("The skill -> teachers map is sorted alphabetically (TreeMap)")
    void skillMapIsSorted() {
        Map<String, List<Student>> map = matchingService.buildSkillToTeachersMap();

        assertEquals(2, map.size());
        assertEquals("[Java, Python]", map.keySet().toString());
        assertEquals("Ravi", map.get("Java").get(0).getName());
    }

    @Test
    @DisplayName("The leaderboard (TreeSet) puts the best rating first")
    void leaderboardIsSortedByRating() {
        meera.addRating(5);
        ravi.addRating(3);

        List<Student> ranked = List.copyOf(matchingService.buildRatingLeaderboard());
        assertEquals("Meera", ranked.get(0).getName());
        assertEquals("Ravi", ranked.get(1).getName());
        assertFalse(ranked.isEmpty());
    }
}
