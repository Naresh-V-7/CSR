package skillloop.users;

import java.util.Set;

import skillloop.skills.Skill;

/**
 * INTERFACE: "anyone who can teach in SkillLoop must be able to do these things".
 *
 * An interface only states WHAT must be possible, never HOW.
 * Student provides the HOW in Student.java.
 */
public interface Teachable {

    /** Add a skill this person is able to teach. */
    void addTeachingSkill(Skill skill);

    /** True when this person teaches the given skill (case-insensitive). */
    boolean teaches(String skillName);

    /** All skills this person can teach. */
    Set<Skill> getTeachingSkills();

    /**
     * default method (Java 8+): a ready-made implementation every class that
     * implements this interface gets for free. Student does not have to write it.
     */
    default int teachingSkillCount() {
        return getTeachingSkills().size();
    }
}
