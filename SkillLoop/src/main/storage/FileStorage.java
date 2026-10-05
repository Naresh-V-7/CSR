package skillloop.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import skillloop.credits.CreditTransaction;
import skillloop.credits.SkillWallet;
import skillloop.sessions.Review;
import skillloop.sessions.Session;
import skillloop.skills.Skill;
import skillloop.users.Student;

/**
 * FILE HANDLING layer of SkillLoop.
 *
 * Everything is stored as plain text with '|' between the fields, so the files
 * can be opened in Notepad during the project review:
 *
 *   data/students.txt      101|Ravi|ravi@college.edu|pass123|CSE|20|Java,C|Python
 *   data/skills.txt        Java|Programming|4
 *   data/sessions.txt      S1|Java|101|1|1|2026-03-11|SCHEDULED
 *   data/transactions.txt  TXN1|101|EARNED|10|Taught Java
 *   data/reviews.txt       RV1|S1|102|101|5|Very clear explanation
 *
 * Classes used (straight from the syllabus):
 *   File, FileWriter, BufferedWriter, PrintWriter, FileReader, BufferedReader
 *
 * try-with-resources is used everywhere so the streams are closed automatically
 * even if an IOException is thrown in the middle of writing.
 */
public class FileStorage {

    public static final String SEPARATOR = "\\|";      // for split()
    public static final String JOIN = "|";

    private final String dataFolder;

    /** DEPENDENCY INJECTION: the folder is a constructor parameter, so the unit
     *  tests can write into a temporary folder instead of data/. */
    public FileStorage(String dataFolder) {
        this.dataFolder = dataFolder;
        createFolderIfMissing();
    }

    public FileStorage() {
        this("data");
    }

    /** The File class is used here to create the folder when it does not exist. */
    private void createFolderIfMissing() {
        File folder = new File(dataFolder);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("[storage] created data folder: " + folder.getPath());
            }
        }
    }

    private File fileFor(String fileName) {
        return new File(dataFolder, fileName);
    }

    // ==================================================================
    // WRITING
    // ==================================================================

    /**
     * Save every student.
     *
     * FileWriter  -> writes characters to the file
     * BufferedWriter -> collects them in memory first, so the disk is touched
     *                   once instead of once per character (much faster)
     */
    public void saveStudents(List<Student> students) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileFor("students.txt")))) {
            for (Student student : students) {
                writer.write(studentToLine(student));
                writer.newLine();
            }
        }
        // the try-with-resources block closed the writer for us
    }

    private String studentToLine(Student student) {
        return student.getId() + JOIN
                + student.getName() + JOIN
                + student.getEmail() + JOIN
                + student.getPassword() + JOIN
                + student.getCollege() + JOIN
                + student.getWallet().getBalance() + JOIN
                + skillsToText(student.getTeachingSkills()) + JOIN
                + skillsToText(student.getLearningSkills());
    }

    private String skillsToText(java.util.Set<Skill> skills) {
        StringBuilder text = new StringBuilder();
        for (Skill skill : skills) {
            if (text.length() > 0) {
                text.append(",");
            }
            text.append(skill.getName()).append(":").append(skill.getLevel());
        }
        return text.length() == 0 ? "-" : text.toString();
    }

    /** PrintWriter is the most comfortable writer: it has println() and printf(). */
    public void saveSkills(List<Skill> skills) throws IOException {
        try (PrintWriter printer = new PrintWriter(new FileWriter(fileFor("skills.txt")))) {
            for (Skill skill : skills) {
                printer.println(skill.getName() + JOIN + skill.getCategory() + JOIN + skill.getLevel());
            }
        }
    }

    public void saveSessions(List<Session> sessions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileFor("sessions.txt")))) {
            for (Session session : sessions) {
                writer.write(session.getSessionId() + JOIN
                        + session.getSkill().getName() + JOIN
                        + session.getTeacher().getId() + JOIN
                        + session.getMaxSeats() + JOIN
                        + session.getDurationHours() + JOIN
                        + session.getScheduledDate() + JOIN
                        + session.getStatus());
                writer.newLine();
            }
        }
    }

    /** Every credit movement of every student, so the economy is auditable. */
    public void saveTransactions(List<Student> students) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileFor("transactions.txt")))) {
            for (Student student : students) {
                for (CreditTransaction transaction : student.getWallet().getHistory()) {
                    writer.write(transaction.getTransactionId() + JOIN
                            + transaction.getStudentId() + JOIN
                            + transaction.getType() + JOIN
                            + transaction.getAmount() + JOIN
                            + transaction.getReason());
                    writer.newLine();
                }
            }
        }
    }

    public void saveReviews(List<Review> reviews) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileFor("reviews.txt")))) {
            for (Review review : reviews) {
                writer.write(review.getReviewId() + JOIN
                        + review.getSessionId() + JOIN
                        + review.getReviewerId() + JOIN
                        + review.getTeacherId() + JOIN
                        + review.getRating() + JOIN
                        + review.getComment());
                writer.newLine();
            }
        }
    }

    // ==================================================================
    // READING
    // ==================================================================

    /**
     * Read the students back.
     *
     * FileReader -> reads characters from the file
     * BufferedReader -> adds readLine(), which is what makes line-based files easy
     *
     * Note: ids are NOT restored (the User class generates fresh ones); this is a
     * deliberate simplification for a college project.
     */
    public List<Student> loadStudents() throws IOException {
        List<Student> students = new ArrayList<>();
        File file = fileFor("students.txt");
        if (!file.exists()) {
            return students;                      // nothing saved yet - normal on first run
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR);     // ARRAY of fields
                if (parts.length < 8) {
                    continue;                                // skip a damaged line
                }
                Student student = new Student(parts[1], parts[2], parts[3], parts[4]);
                int savedBalance = Integer.parseInt(parts[5]);
                int difference = savedBalance - student.getWallet().getBalance();
                if (difference > 0) {
                    student.getWallet().earn(difference, "Balance restored from file");
                }
                addSkillsFromText(student, parts[6], true);
                addSkillsFromText(student, parts[7], false);
                students.add(student);
            }
        }
        return students;
    }

    private void addSkillsFromText(Student student, String text, boolean teaching) {
        if (text == null || text.equals("-") || text.isEmpty()) {
            return;
        }
        for (String entry : text.split(",")) {           // ARRAY again
            String[] nameAndLevel = entry.split(":");
            String name = nameAndLevel[0];
            int level = nameAndLevel.length > 1 ? Integer.parseInt(nameAndLevel[1]) : 1;
            Skill skill = new Skill(name, Skill.DEFAULT_CATEGORY, level);
            if (teaching) {
                student.addTeachingSkill(skill);
            } else {
                student.addLearningSkill(skill);
            }
        }
    }

    /** Read the skill catalogue with BufferedReader/FileReader. */
    public List<Skill> loadSkills() throws IOException {
        List<Skill> skills = new ArrayList<>();
        File file = fileFor("skills.txt");
        if (!file.exists()) {
            return skills;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR);
                if (parts.length < 3) {
                    continue;
                }
                skills.add(new Skill(parts[0], parts[1], Integer.parseInt(parts[2])));
            }
        }
        return skills;
    }

    /** Raw lines of any data file - used by the "view file" option of the UI. */
    public List<String> readLines(String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        File file = fileFor(fileName);
        if (!file.exists()) {
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Append one line to the platform log.
     * FileWriter with the second parameter "true" means APPEND instead of overwrite.
     */
    public void appendLog(String message) {
        try (PrintWriter printer = new PrintWriter(new FileWriter(fileFor("activity.log"), true))) {
            printer.println(System.currentTimeMillis() + " " + message);
        } catch (IOException e) {
            // logging must never crash the application
            System.out.println("[storage] could not write the log: " + e.getMessage());
        }
    }

    /**
     * BYTE STREAMS: copy a data file to "<name>.bak" before it is overwritten.
     *
     * FileInputStream / FileOutputStream read and write RAW BYTES, not characters.
     * For a backup that is exactly what we want: the copy must be byte-for-byte
     * identical, and we never need to understand the text inside it.
     * (The reader/writer classes above are CHARACTER streams; this is the other
     * family of streams in the syllabus.)
     */
    public void backup(String fileName) throws IOException {
        File source = fileFor(fileName);
        if (!source.exists()) {
            return;
        }
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(fileFor(fileName + ".bak"))) {

            byte[] buffer = new byte[1024];      // ARRAY used as the copy buffer
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    /** Save everything in one call - used by the "Save & Exit" menu option. */
    public void saveAll(List<Student> students, List<Skill> skills,
                        List<Session> sessions, List<Review> reviews) throws IOException {
        backup("students.txt");                  // keep yesterday's file just in case
        backup("transactions.txt");
        saveStudents(students);
        saveSkills(skills);
        saveSessions(sessions);
        saveTransactions(students);
        saveReviews(reviews);
    }

    public String getDataFolder() {
        return dataFolder;
    }

    /** Wallet rule reminder used in file reports. */
    public static String economyNote() {
        return "1 hour of teaching = " + SkillWallet.CREDITS_PER_HOUR + " Skill Credits";
    }
}
