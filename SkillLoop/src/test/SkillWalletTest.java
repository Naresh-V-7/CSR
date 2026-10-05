package skillloop.testing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import skillloop.credits.CreditTransaction;
import skillloop.credits.SkillWallet;
import skillloop.exceptions.InsufficientCreditsException;

/** JUnit 5 tests for the credit economy. */
class SkillWalletTest {

    private SkillWallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new SkillWallet(101);          // starts with WELCOME_CREDITS = 20
    }

    @Test
    @DisplayName("Teaching earns credits and writes a transaction")
    void earningCreditsWorks() {
        wallet.earn(10, "Taught Java for 1 hour");

        assertEquals(30, wallet.getBalance());
        assertEquals(10, wallet.getTotalEarned());
        assertEquals(1, wallet.getHistory().size());
        assertEquals(CreditTransaction.Type.EARNED, wallet.getHistory().get(0).getType());
    }

    @Test
    @DisplayName("Learning deducts credits")
    void spendingCreditsWorks() throws InsufficientCreditsException {
        wallet.spend(10, "Booked a Python session");

        assertEquals(10, wallet.getBalance());
        assertEquals(10, wallet.getTotalSpent());
        assertEquals("-10", wallet.getHistory().get(0).getSignedAmount());
    }

    @Test
    @DisplayName("Spending more than the balance throws InsufficientCreditsException")
    void spendingTooMuchThrows() {
        InsufficientCreditsException thrown = assertThrows(InsufficientCreditsException.class,
                () -> wallet.spend(50, "Too expensive"));

        assertEquals(50, thrown.getRequiredCredits());
        assertEquals(20, thrown.getAvailableCredits());
        assertEquals(30, thrown.getShortfall());
        assertEquals(20, wallet.getBalance(), "the balance must not change on a failure");
    }

    @Test
    @DisplayName("canAfford() answers before the exception is needed")
    void canAffordWorks() {
        assertTrue(wallet.canAfford(20));
        assertFalse(wallet.canAfford(21));
    }

    @Test
    @DisplayName("A cancelled booking is refunded")
    void refundRestoresTheBalance() throws InsufficientCreditsException {
        wallet.spend(10, "Booked S1");
        wallet.refund(10, "Cancelled S1");

        assertEquals(20, wallet.getBalance());
        assertEquals(2, wallet.getHistory().size(), "spend + refund are both recorded");
    }

    @Test
    @DisplayName("10 credits per hour is the rule of the economy")
    void costPerHourIsTenCredits() {
        assertEquals(10, SkillWallet.costOf(1));
        assertEquals(30, SkillWallet.costOf(3));
        assertDoesNotThrow(() -> wallet.spend(SkillWallet.costOf(2), "2 hour session"));
    }
}
