package skillloop.matching;

import java.util.List;

import skillloop.users.Student;

/**
 * One skill exchange cycle found by SkillCycleMatcher.
 *
 * Example with three students:
 *      members = [A, B, C]
 *      skills  = [Java, Python, C]
 * meaning   A --Java--> B --Python--> C --C--> A
 *
 * Everybody teaches one person and learns from another, and nobody is left out.
 */
public class SkillCycle {

    private final List<Student> members;
    private final List<String> skills;

    public SkillCycle(List<Student> members, List<String> skills) {
        this.members = members;
        this.skills = skills;
    }

    public List<Student> getMembers() {
        return members;
    }

    public List<String> getSkills() {
        return skills;
    }

    /** 2 = direct exchange, 3 or more = a real cycle. */
    public int size() {
        return members.size();
    }

    public boolean isDirectExchange() {
        return members.size() == 2;
    }

    /** "A --Java--> B --Python--> C --C--> A" */
    public String describe() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            text.append(members.get(i).getName());
            text.append(" --").append(skills.get(i)).append("--> ");
        }
        text.append(members.get(0).getName());     // the cycle closes on itself
        return text.toString();
    }

    @Override
    public String toString() {
        return (isDirectExchange() ? "[DIRECT ] " : "[CYCLE-" + size() + "] ") + describe();
    }
}
