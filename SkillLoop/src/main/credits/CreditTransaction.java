package skillloop.credits;

/**
 * One line in the credit history: "+10 credits for teaching Java".
 *
 * Every change of a SkillWallet creates one CreditTransaction object, so the
 * credit economy is fully traceable (this is what the "Transaction History"
 * screen prints).
 */
public class CreditTransaction {

    /** The two kinds of movement a wallet can have. */
    public enum Type {
        EARNED,   // taught a session
        SPENT     // learned in a session
    }

    private static int transactionCounter = 0;   // static -> shared ID generator

    private final String transactionId;
    private final int studentId;
    private final Type type;
    private final int amount;
    private final String reason;
    private final long timeStamp;

    public CreditTransaction(int studentId, Type type, int amount, String reason) {
        transactionCounter++;
        this.transactionId = "TXN" + transactionCounter;
        this.studentId = studentId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.timeStamp = System.currentTimeMillis();
    }

    /** Used by the file-loading code to rebuild a transaction read from disk. */
    public CreditTransaction(String transactionId, int studentId, Type type,
                             int amount, String reason, long timeStamp) {
        this.transactionId = transactionId;
        this.studentId = studentId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.timeStamp = timeStamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getStudentId() {
        return studentId;
    }

    public Type getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    /** "+10" for earning, "-10" for spending - used by the wallet screen. */
    public String getSignedAmount() {
        return (type == Type.EARNED ? "+" : "-") + amount;
    }

    @Override
    public String toString() {
        return String.format("%-6s %-7s %-4s  %s",
                transactionId, type, getSignedAmount(), reason);
    }
}
