package skillloop.matching;

import skillloop.users.Student;

/**
 * FUNCTIONAL INTERFACE: an interface with exactly ONE abstract method.
 *
 * That single method is what allows a LAMBDA EXPRESSION to be written instead of
 * a whole class:
 *
 *      SkillMatcher javaTeachers = student -> student.teaches("Java");
 *      SkillMatcher goodTeachers = student -> student.getAverageRating() >= 4.0;
 *
 * The @FunctionalInterface annotation is optional, but it makes the compiler
 * refuse a second abstract method, which protects the lambdas above.
 */
@FunctionalInterface
public interface SkillMatcher {

    /** The one abstract method - the lambda becomes the body of this method. */
    boolean matches(Student student);

    /**
     * default method: combine two conditions with AND.
     *      javaTeachers.and(goodTeachers)
     */
    default SkillMatcher and(SkillMatcher other) {
        return student -> this.matches(student) && other.matches(student);
    }

    /** default method: invert a condition. */
    default SkillMatcher negate() {
        return student -> !this.matches(student);
    }

    /** static factory that builds a ready-made matcher for a skill name. */
    static SkillMatcher teachesSkill(String skillName) {
        return student -> student.teaches(skillName);
    }

    /** static factory: students who WANT this skill. */
    static SkillMatcher wantsSkill(String skillName) {
        return student -> student.wantsToLearn(skillName);
    }
}
