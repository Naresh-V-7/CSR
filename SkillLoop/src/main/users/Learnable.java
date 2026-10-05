package skillloop.users;

import java.util.Set;

import skillloop.skills.Skill;

/**
 * INTERFACE: "anyone who can learn in SkillLoop must be able to do these things".
 *
 * Teachable and Learnable are separate on purpose: the matching engine asks a
 * student "what can you teach?" and "what do you want to learn?" through two
 * different, small interfaces.
 */
public interface Learnable {

    /** Add a skill this person wants to learn. */
    void addLearningSkill(Skill skill);

    /** True when this person wants to learn the given skill. */
    boolean wantsToLearn(String skillName);

    /** All skills this person wants to learn. */
    Set<Skill> getLearningSkills();

    default int learningSkillCount() {
        return getLearningSkills().size();
    }
}
