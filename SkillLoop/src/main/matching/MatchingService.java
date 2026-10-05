package skillloop.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import skillloop.collections.Repository;
import skillloop.skills.Skill;
import skillloop.users.Student;

/**
 * Finds people who can teach what you want to learn.
 *
 * DEPENDENCY INJECTION:
 *   This service does not create the student repository itself - it receives it
 *   through the constructor. That keeps the service independent of where the
 *   students come from, and it lets the unit tests inject a small repository
 *   containing only three test students.
 */
public class MatchingService {

    private final Repository<Student> studentRepository;

    /** Constructor injection - the ONLY way to build this service. */
    public MatchingService(Repository<Student> studentRepository) {
        this.studentRepository = studentRepository;
    }

    // ------------------------------------------------------------------
    // Lambda + functional interface
    // ------------------------------------------------------------------

    /**
     * FUNCTIONAL INTERFACE in action: the caller decides the rule, this method
     * only applies it.
     *      matchingService.filter(student -> student.teaches("Java"));
     */
    public List<Student> filter(SkillMatcher matcher) {
        List<Student> result = new ArrayList<>();
        for (Student student : studentRepository.getAll()) {
            if (matcher.matches(student)) {          // lambda body runs here
                result.add(student);
            }
        }
        return result;
    }

    /**
     * LAMBDA + STREAM: everybody who can teach the given skill.
     * Reads almost like English: take the students, keep those who teach it,
     * collect them into a list.
     */
    public List<Student> findTeachersOf(String skillName) {
        return studentRepository.getAll().stream()
                .filter(student -> student.teaches(skillName))
                .collect(Collectors.toList());
    }

    /** LAMBDA: everybody who wants to learn the given skill. */
    public List<Student> findLearnersOf(String skillName) {
        return studentRepository.getAll().stream()
                .filter(student -> student.wantsToLearn(skillName))
                .collect(Collectors.toList());
    }

    /** LAMBDA with two conditions: good teachers of a skill (rating >= 4). */
    public List<Student> findTopRatedTeachersOf(String skillName) {
        return studentRepository.getAll().stream()
                .filter(student -> student.teaches(skillName))
                .filter(student -> student.getAverageRating() >= 4.0)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Match list for one learner
    // ------------------------------------------------------------------

    /**
     * For every skill the learner wants, find the students who teach it and give
     * each of them a score.
     */
    public List<SkillMatch> findMatchesFor(Student learner) {
        List<SkillMatch> matches = new ArrayList<>();
        for (Skill wanted : learner.getLearningSkills()) {
            for (Student teacher : findTeachersOf(wanted.getName())) {
                if (teacher.getId() == learner.getId()) {
                    continue;                        // do not match with yourself
                }
                boolean direct = isDirectExchange(learner, teacher);
                int score = calculateScore(teacher, wanted.getName(), direct);
                matches.add(new SkillMatch(teacher, wanted.getName(), score, direct));
            }
        }
        // LAMBDA used as a Comparator: best score first
        matches.sort((first, second) -> Integer.compare(second.getScore(), first.getScore()));
        return matches;
    }

    /**
     * DIRECT EXCHANGE = a 2-person cycle:
     * the teacher can teach something the learner wants AND the learner can teach
     * something the teacher wants.
     */
    public boolean isDirectExchange(Student learner, Student teacher) {
        for (Skill mySkill : learner.getTeachingSkills()) {
            if (teacher.wantsToLearn(mySkill.getName())) {
                return true;
            }
        }
        return false;
    }

    /** All direct (two-way) exchanges available to one student. */
    public List<Student> findDirectExchangePartners(Student learner) {
        List<Student> partners = new ArrayList<>();
        for (Student other : studentRepository.getAll()) {
            if (other.getId() == learner.getId()) {
                continue;
            }
            boolean otherCanTeachMe = false;
            for (Skill wanted : learner.getLearningSkills()) {
                if (other.teaches(wanted.getName())) {
                    otherCanTeachMe = true;
                    break;
                }
            }
            if (otherCanTeachMe && isDirectExchange(learner, other)) {
                partners.add(other);
            }
        }
        return partners;
    }

    /**
     * MATH CLASS: a simple, explainable score out of 100.
     *
     *   base                     50
     * + teacher's skill level  * 5     (level 1..5   ->  5..25)
     * + teacher's rating       * 5     (rating 0..5  ->  0..25)
     * + direct exchange bonus   20
     *
     * Math.min keeps the result at or below 100, Math.round turns the decimal
     * rating part into a whole number.
     */
    public int calculateScore(Student teacher, String skillName, boolean directExchange) {
        int level = 1;
        for (Skill skill : teacher.getTeachingSkills()) {
            if (skill.getName().equalsIgnoreCase(skillName)) {
                level = skill.getLevel();
            }
        }
        double raw = 50
                + (level * 5)
                + (teacher.getAverageRating() * 5)
                + (directExchange ? 20 : 0);
        return (int) Math.min(100, Math.round(raw));
    }

    // ------------------------------------------------------------------
    // Map / TreeMap / TreeSet views
    // ------------------------------------------------------------------

    /**
     * MAP: skill name -> students who teach it.
     *
     * A TreeMap is used (not a HashMap) because the "Find Skills" screen should
     * list the skills in alphabetical order, and a TreeMap keeps its keys sorted
     * automatically.
     */
    public Map<String, List<Student>> buildSkillToTeachersMap() {
        Map<String, List<Student>> skillMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Student student : studentRepository.getAll()) {
            for (Skill skill : student.getTeachingSkills()) {
                // computeIfAbsent: create the list the first time we see the skill
                skillMap.computeIfAbsent(skill.getName(), key -> new ArrayList<>()).add(student);
            }
        }
        return skillMap;
    }

    /**
     * TREESET: the leaderboard of students, best rating first.
     * TreeSet sorts automatically using Student.compareTo().
     */
    public Set<Student> buildRatingLeaderboard() {
        return new TreeSet<>(studentRepository.getAll());
    }

    public Repository<Student> getStudentRepository() {
        return studentRepository;
    }
}
