package skillloop.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import skillloop.collections.Repository;
import skillloop.exceptions.SkillNotFoundException;
import skillloop.skills.Skill;

/**
 * The catalogue of skills offered on the platform.
 *
 * This class is the METHOD OVERLOADING example of the project: findSkill()
 * exists three times with different parameter lists.
 *
 * Why overloading is useful here:
 *   the user of the "Find Skills" screen may know only the skill name, or the
 *   name and the category, or all three details. Instead of inventing three
 *   different method names (findSkillByName, findSkillByNameAndCategory, ...)
 *   we keep ONE meaningful name and let the compiler pick the right version
 *   from the arguments that were actually supplied.
 */
public class SkillService {

    private final Repository<Skill> skillRepository;

    /** DEPENDENCY INJECTION: the repository comes from outside. */
    public SkillService(Repository<Skill> skillRepository) {
        this.skillRepository = skillRepository;
    }

    public void addSkill(Skill skill) {
        skillRepository.add(skill);
    }

    // ------------------------------------------------------------------
    // METHOD OVERLOADING - three versions of findSkill()
    // ------------------------------------------------------------------

    /** Version 1: search by name only. */
    public List<Skill> findSkill(String skillName) throws SkillNotFoundException {
        List<Skill> found = new ArrayList<>();
        for (Skill skill : skillRepository.getAll()) {
            if (skill.getName().equalsIgnoreCase(skillName)) {
                found.add(skill);
            }
        }
        if (found.isEmpty()) {
            throw new SkillNotFoundException(skillName);
        }
        return found;
    }

    /** Version 2: name + category. */
    public List<Skill> findSkill(String skillName, String category) throws SkillNotFoundException {
        List<Skill> found = new ArrayList<>();
        for (Skill skill : findSkill(skillName)) {          // reuses version 1
            if (skill.getCategory().equalsIgnoreCase(category)) {
                found.add(skill);
            }
        }
        if (found.isEmpty()) {
            throw new SkillNotFoundException(skillName + " in category " + category);
        }
        return found;
    }

    /** Version 3: name + category + minimum level. */
    public List<Skill> findSkill(String skillName, String category, int minimumLevel)
            throws SkillNotFoundException {
        List<Skill> found = new ArrayList<>();
        for (Skill skill : findSkill(skillName, category)) { // reuses version 2
            if (skill.getLevel() >= minimumLevel) {
                found.add(skill);
            }
        }
        if (found.isEmpty()) {
            throw new SkillNotFoundException(skillName + " (" + category
                    + ") at level " + minimumLevel + " or higher");
        }
        return found;
    }

    // ------------------------------------------------------------------
    // Other catalogue operations
    // ------------------------------------------------------------------

    /** LAMBDA: partial-name search used by the "Find Skills" screen. */
    public List<Skill> searchByKeyword(String keyword) {
        return skillRepository.findBy(
                skill -> skill.getName().toLowerCase().contains(keyword.toLowerCase()));
    }

    /**
     * TREESET of names: every skill name, sorted alphabetically and without
     * duplicates - exactly what the catalogue screen needs.
     */
    public Set<String> getSortedSkillNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Skill skill : skillRepository.getAll()) {
            names.add(skill.getName());
        }
        return names;
    }

    public List<Skill> getAllSkills() {
        return skillRepository.getAll();
    }

    public Repository<Skill> getSkillRepository() {
        return skillRepository;
    }
}
