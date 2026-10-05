package skillloop.exceptions;

/**
 * Thrown when a skill name is searched for but does not exist in the catalogue.
 *
 * CHECKED exception: search results are part of normal application flow.
 */
public class SkillNotFoundException extends SkillLoopException {

    private final String skillName;

    public SkillNotFoundException(String skillName) {
        super("No skill found with name '" + skillName + "'.");
        this.skillName = skillName;
    }

    public String getSkillName() {
        return skillName;
    }
}
