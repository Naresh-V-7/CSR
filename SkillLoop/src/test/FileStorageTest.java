package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import skillloop.skills.Skill;
import skillloop.storage.FileStorage;
import skillloop.users.Student;
import skillloop.users.User;

/**
 * JUnit 5 tests for the file handling layer.
 *
 * @TempDir gives the test its own empty folder, so the real data/ folder of the
 * application is never touched. This works only because FileStorage receives its
 * folder through the CONSTRUCTOR (dependency injection again).
 */
class FileStorageTest {

    @Test
    @DisplayName("Students are written to students.txt and read back")
    void studentsAreSavedAndLoaded(@TempDir Path tempFolder) throws IOException {
        User.resetCounters();
        FileStorage storage = new FileStorage(tempFolder.toString());

        Student ravi = new Student("Ravi", "ravi@c.edu", "pass", "CSE");
        ravi.addTeachingSkill(new Skill("Java", "Programming", 4));
        ravi.addLearningSkill(new Skill("Python"));
        ravi.getWallet().earn(10, "Taught Java");

        List<Student> toSave = new ArrayList<>();
        toSave.add(ravi);
        storage.saveStudents(toSave);

        List<String> lines = storage.readLines("students.txt");
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("Ravi"));
        assertTrue(lines.get(0).contains("Java:4"));

        List<Student> loaded = storage.loadStudents();
        assertEquals(1, loaded.size());
        assertEquals("Ravi", loaded.get(0).getName());
        assertEquals(30, loaded.get(0).getWallet().getBalance(), "the balance is restored");
        assertTrue(loaded.get(0).teaches("Java"));
        assertTrue(loaded.get(0).wantsToLearn("Python"));
    }

    @Test
    @DisplayName("The skill catalogue survives a save/load round trip")
    void skillsAreSavedAndLoaded(@TempDir Path tempFolder) throws IOException {
        FileStorage storage = new FileStorage(tempFolder.toString());

        List<Skill> skills = new ArrayList<>();
        skills.add(new Skill("Java", "Programming", 4));
        skills.add(new Skill("Public Speaking", "Soft Skills", 5));
        storage.saveSkills(skills);

        List<Skill> loaded = storage.loadSkills();
        assertEquals(2, loaded.size());
        assertEquals("Java", loaded.get(0).getName());
        assertEquals(5, loaded.get(1).getLevel());
    }

    @Test
    @DisplayName("backup() copies a file byte by byte with the byte streams")
    void backupCopiesTheFile(@TempDir Path tempFolder) throws IOException {
        FileStorage storage = new FileStorage(tempFolder.toString());

        List<Skill> skills = new ArrayList<>();
        skills.add(new Skill("Java", "Programming", 4));
        storage.saveSkills(skills);

        storage.backup("skills.txt");

        List<String> original = storage.readLines("skills.txt");
        List<String> copy = storage.readLines("skills.txt.bak");
        assertEquals(original, copy);
    }

    @Test
    @DisplayName("Reading a file that does not exist returns an empty list, not a crash")
    void missingFileIsHandled(@TempDir Path tempFolder) throws IOException {
        FileStorage storage = new FileStorage(tempFolder.toString());
        assertTrue(storage.loadStudents().isEmpty());
        assertTrue(storage.readLines("nothing.txt").isEmpty());
    }
}
