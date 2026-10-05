package skillloop.exceptions;

/**
 * Thrown when a skill level outside 1..5 is used.
 *
 * UNCHECKED exception (extends RuntimeException): a wrong level is a PROGRAMMING
 * / input-validation mistake, not a normal business outcome, so we do not want
 * every method in the project to declare "throws" for it.
 *
 * This class is the one place in the project where we show the difference
 * between checked and unchecked exceptions.
 */
public class InvalidSkillLevelException extends RuntimeException {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;

    private final int invalidLevel;

    public InvalidSkillLevelException(int invalidLevel) {
        super("Skill level " + invalidLevel + " is invalid. Allowed range is "
                + MIN_LEVEL + " to " + MAX_LEVEL + ".");
        this.invalidLevel = invalidLevel;
    }

    public int getInvalidLevel() {
        return invalidLevel;
    }
}
