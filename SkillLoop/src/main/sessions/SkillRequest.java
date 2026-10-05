package skillloop.sessions;

/**
 * "Student 101 wants to learn Python."
 *
 * A SkillRequest is put into a Queue and processed later by the background
 * matching thread, exactly like a help-desk ticket.
 */
public class SkillRequest {

    private static int requestCounter = 0;

    private final String requestId;
    private final int studentId;
    private final String studentName;
    private final String skillName;
    private boolean processed;
    private String result;

    public SkillRequest(int studentId, String studentName, String skillName) {
        requestCounter++;
        this.requestId = "REQ" + requestCounter;
        this.studentId = studentId;
        this.studentName = studentName;
        this.skillName = skillName;
        this.processed = false;
        this.result = "waiting in queue";
    }

    public String getRequestId() {
        return requestId;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getSkillName() {
        return skillName;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getResult() {
        return result;
    }

    /** Called by the matching thread once the request has been looked at. */
    public void complete(String result) {
        this.processed = true;
        this.result = result;
    }

    @Override
    public String toString() {
        return requestId + ": " + studentName + " wants to learn " + skillName
                + "  -> " + result;
    }
}
