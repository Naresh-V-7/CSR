package skillloop.credits;

import java.util.ArrayList;
import java.util.List;

import skillloop.exceptions.InsufficientCreditsException;

/**
 * The virtual credit account of ONE student.
 *
 * Rules of the SkillLoop economy:
 *   teach 1 hour  -> +10 Skill Credits
 *   learn 1 hour  -> -10 Skill Credits
 *   a new student starts with 20 credits so they can learn before teaching
 *
 * Single responsibility: this class only manages credits. It does not know
 * anything about sessions, bookings or students.
 */
public class SkillWallet {

    /** Application constants - static final, shared by every wallet. */
    public static final int CREDITS_PER_HOUR = 10;
    public static final int WELCOME_CREDITS = 20;

    private final int ownerId;
    private int balance;
    private int totalEarned;
    private int totalSpent;

    /** ArrayList: the history grows one transaction at a time and is read in order. */
    private final List<CreditTransaction> history = new ArrayList<>();

    public SkillWallet(int ownerId) {
        this(ownerId, WELCOME_CREDITS);
    }

    /** Constructor overloading: used when loading a saved balance from file. */
    public SkillWallet(int ownerId, int openingBalance) {
        this.ownerId = ownerId;
        this.balance = openingBalance;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getBalance() {
        return balance;
    }

    public int getTotalEarned() {
        return totalEarned;
    }

    public int getTotalSpent() {
        return totalSpent;
    }

    public List<CreditTransaction> getHistory() {
        return history;
    }

    /** Credits for hours of teaching. */
    public void earn(int amount, String reason) {
        balance += amount;
        totalEarned += amount;
        history.add(new CreditTransaction(ownerId, CreditTransaction.Type.EARNED, amount, reason));
    }

    /**
     * Credits paid for learning.
     *
     * "throws" tells every caller that this operation can legitimately fail,
     * and the compiler makes sure they handle it.
     */
    public void spend(int amount, String reason) throws InsufficientCreditsException {
        if (amount > balance) {
            throw new InsufficientCreditsException(amount, balance);
        }
        balance -= amount;
        totalSpent += amount;
        history.add(new CreditTransaction(ownerId, CreditTransaction.Type.SPENT, amount, reason));
    }

    /** Cheap check used by the UI before it even offers the "Book" option. */
    public boolean canAfford(int amount) {
        return balance >= amount;
    }

    /** Used when a booking is cancelled: the credits go back without a "gift". */
    public void refund(int amount, String reason) {
        balance += amount;
        totalSpent -= amount;
        history.add(new CreditTransaction(ownerId, CreditTransaction.Type.EARNED, amount, reason));
    }

    /** Used by the file loader to replay a saved transaction into the history. */
    public void addLoadedTransaction(CreditTransaction transaction) {
        history.add(transaction);
    }

    /** Cost of a session of the given number of hours. */
    public static int costOf(int hours) {
        return hours * CREDITS_PER_HOUR;
    }

    @Override
    public String toString() {
        return "Balance: " + balance + " credits (earned " + totalEarned
                + ", spent " + totalSpent + ")";
    }
}
