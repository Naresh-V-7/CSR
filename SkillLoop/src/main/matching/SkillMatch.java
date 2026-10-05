package skillloop.matching;

import skillloop.users.Student;

/**
 * One result row of the matching engine:
 * "Meera can teach you Java - score 87".
 */
public class SkillMatch {

    private final Student teacher;
    private final String skillName;
    private final int score;             // 0..100, computed with the Math class
    private final boolean directExchange;

    public SkillMatch(Student teacher, String skillName, int score, boolean directExchange) {
        this.teacher = teacher;
        this.skillName = skillName;
        this.score = score;
        this.directExchange = directExchange;
    }

    public Student getTeacher() {
        return teacher;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getScore() {
        return score;
    }

    public boolean isDirectExchange() {
        return directExchange;
    }

    @Override
    public String toString() {
        return String.format("%-18s teaches %-10s score %3d %s",
                teacher.getName(), skillName, score,
                directExchange ? "[DIRECT EXCHANGE - you can teach them back]" : "");
    }
}
