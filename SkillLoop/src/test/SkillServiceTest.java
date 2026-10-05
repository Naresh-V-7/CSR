package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import skillloop.collections.Repository;
import skillloop.exceptions.SkillNotFoundException;
import skillloop.services.SkillService;
import skillloop.skills.Skill;

/** JUnit 5 tests for the overloaded findSkill() methods and the catalogue. */
class SkillServiceTest {

    private SkillService skillService;

    @BeforeEach
    void setUp() {
        Repository<Skill> repository = new Repository<>();
        skillService = new SkillService(repository);          // dependency injection

        skillService.addSkill(new Skill("Java", "Programming", 4));
        skillService.addSkill(new Skill("Python", "Programming", 2));
        skillService.addSkill(new Skill("Public Speaking", "Soft Skills", 5));
    }

    @Test
    @DisplayName("Overload 1: search by name")
    void findByName() throws SkillNotFoundException {
        assertEquals(1, skillService.findSkill("Java").size());
        assertEquals("Java", skillService.findSkill("java").get(0).getName());
    }

    @Test
    @DisplayName("Overload 2: search by name and category")
    void findByNameAndCategory() throws SkillNotFoundException {
        assertEquals(1, skillService.findSkill("Java", "Programming").size());
        assertThrows(SkillNotFoundException.class,
                () -> skillService.findSkill("Java", "Soft Skills"));
    }

    @Test
    @DisplayName("Overload 3: search by name, category and minimum level")
    void findByNameCategoryAndLevel() throws SkillNotFoundException {
        assertEquals(1, skillService.findSkill("Java", "Programming", 3).size());
        assertThrows(SkillNotFoundException.class,
                () -> skillService.findSkill("Python", "Programming", 4));
    }

    @Test
    @DisplayName("An unknown skill throws SkillNotFoundException")
    void unknownSkillThrows() {
        SkillNotFoundException thrown = assertThrows(SkillNotFoundException.class,
                () -> skillService.findSkill("Kotlin"));
        assertTrue(thrown.getMessage().contains("Kotlin"));
    }

    @Test
    @DisplayName("Keyword search uses a lambda, the name list uses a TreeSet")
    void keywordSearchAndSortedNames() {
        assertEquals(1, skillService.searchByKeyword("pyth").size());
        assertEquals("[Java, Public Speaking, Python]",
                skillService.getSortedSkillNames().toString());
    }
}
