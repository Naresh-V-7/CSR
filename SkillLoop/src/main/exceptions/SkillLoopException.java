package skillloop.exceptions;

/**
 * Base class of every SkillLoop exception.
 *
 * It extends Exception (not RuntimeException), so every exception that extends
 * this class is a CHECKED exception: the compiler forces the caller to either
 * catch it or declare it with "throws".
 *
 * Having one common parent means a caller that does not care about the exact
 * problem can simply write:  catch (SkillLoopException e) { ... }
 */
public class SkillLoopException extends Exception {

    public SkillLoopException(String message) {
        // super(...) calls the constructor of java.lang.Exception
        super(message);
    }

    public SkillLoopException(String message, Throwable cause) {
        super(message, cause);
    }
}
