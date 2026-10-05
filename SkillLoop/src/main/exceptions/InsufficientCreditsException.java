package skillloop.exceptions;

/**
 * Thrown when a student tries to book a session but their SkillWallet does not
 * contain enough Skill Credits.
 *
 * CHECKED exception: booking is a normal business situation that the caller is
 * expected to handle (show a message, suggest teaching first), so the compiler
 * should force the caller to deal with it.
 */
public class InsufficientCreditsException extends SkillLoopException {

    private final int requiredCredits;
    private final int availableCredits;

    public InsufficientCreditsException(int requiredCredits, int availableCredits) {
        super("Not enough Skill Credits: need " + requiredCredits
                + ", wallet has " + availableCredits + ".");
        this.requiredCredits = requiredCredits;
        this.availableCredits = availableCredits;
    }

    public int getRequiredCredits() {
        return requiredCredits;
    }

    public int getAvailableCredits() {
        return availableCredits;
    }

    /** How many more credits the student must earn by teaching. */
    public int getShortfall() {
        return requiredCredits - availableCredits;
    }
}
