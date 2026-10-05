package skillloop.skills;

import skillloop.exceptions.InvalidSkillLevelException;

/**
 * Represents ONE skill in SkillLoop, for example "Java / Programming / level 4".
 *
 * OOP concepts visible in this class:
 *   - Class and object
 *   - Constructors (three of them - constructor overloading)
 *   - "this" keyword
 *   - static members (skill counter + default category)
 *   - equals()/hashCode() overriding, which is what makes a HashSet able to
 *     block duplicate skills for a student
 */
public class Skill {

    /** Used when the user does not tell us a category. */
    public static final String DEFAULT_CATEGORY = "General";

    /** Default level for a newly added skill. */
    public static final int DEFAULT_LEVEL = 1;

    /** static counter shared by ALL Skill objects - counts how many were created. */
    private static int totalSkillsCreated = 0;

    private String name;
    private String category;
    private int level;          // 1 (beginner) .. 5 (expert)

    /**
     * Full constructor. The other two constructors call this one using this(...)
     * so the validation code exists in exactly one place.
     */
    public Skill(String name, String category, int level) {
        validateLevel(level);                  // may throw InvalidSkillLevelException
        this.name = normalise(name);           // "this" separates field from parameter
        this.category = category;
        this.level = level;
        totalSkillsCreated++;                  // static field, updated per object
    }

    /** Constructor overloading: name + category, default level. */
    public Skill(String name, String category) {
        this(name, category, DEFAULT_LEVEL);   // this(...) = constructor chaining
    }

    /** Constructor overloading: only a name. */
    public Skill(String name) {
        this(name, DEFAULT_CATEGORY, DEFAULT_LEVEL);
    }

    /** Level validation is the reason InvalidSkillLevelException exists. */
    private void validateLevel(int level) {
        if (level < InvalidSkillLevelException.MIN_LEVEL
                || level > InvalidSkillLevelException.MAX_LEVEL) {
            throw new InvalidSkillLevelException(level);   // "throw" keyword
        }
    }

    /** Skill names are compared case-insensitively, so we store them tidily. */
    private String normalise(String rawName) {
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        validateLevel(level);
        this.level = level;           // "this" inside a setter
    }

    /** static method: works on the class, not on one object. */
    public static int getTotalSkillsCreated() {
        return totalSkillsCreated;
    }

    /** Only used by tests, to start counting again. */
    public static void resetCounter() {
        totalSkillsCreated = 0;
    }

    /** True when both skills mean the same subject, ignoring case. */
    public boolean isSameSkillAs(Skill other) {
        return other != null && this.name.equalsIgnoreCase(other.name);
    }

    /**
     * Overriding equals() from Object.
     * Two skills are equal when their NAMES match (category/level may differ).
     * This is what makes Set<Skill> refuse duplicates such as "java" and "Java".
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Skill)) {
            return false;
        }
        Skill that = (Skill) other;
        return this.name.equalsIgnoreCase(that.name);
    }

    /** hashCode() must agree with equals(), otherwise HashSet/HashMap misbehave. */
    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name + " (" + category + ", level " + level + ")";
    }
}
