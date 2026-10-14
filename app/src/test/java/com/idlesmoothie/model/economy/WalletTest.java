package com.idlesmoothie.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WalletTest {

    private static final double DELTA = 1e-9;

    @Test
    void newWalletIsEmpty() {
        Wallet wallet = new Wallet();

        assertEquals(0, wallet.getCoins(), DELTA);
        assertEquals(0, wallet.getSugarNuggets());
    }

    @Test
    void addCoinsIncreasesTheBalance() {
        Wallet wallet = new Wallet();

        wallet.addCoins(10);
        wallet.addCoins(2.5);

        assertEquals(12.5, wallet.getCoins(), DELTA);
    }

    @Test
    void addingZeroCoinsIsAllowed() {
        Wallet wallet = new Wallet();

        wallet.addCoins(0);

        assertEquals(0, wallet.getCoins(), DELTA);
    }

    @Test
    void addCoinsRejectsNegativeAmount() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class, () -> wallet.addCoins(-1));
    }

    @Test
    void addCoinsRejectsNaNAndInfinity() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class, () -> wallet.addCoins(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> wallet.addCoins(Double.POSITIVE_INFINITY));
    }

    @Test
    void spendCoinsRemovesThemWhenAffordable() {
        Wallet wallet = new Wallet();
        wallet.addCoins(10);

        assertTrue(wallet.spendCoins(4));
        assertEquals(6, wallet.getCoins(), DELTA);
    }

    @Test
    void spendingTheExactBalanceLeavesZero() {
        Wallet wallet = new Wallet();
        wallet.addCoins(10);

        assertTrue(wallet.spendCoins(10));
        assertEquals(0, wallet.getCoins(), DELTA);
    }

    @Test
    void spendCoinsChangesNothingWhenTooExpensive() {
        Wallet wallet = new Wallet();
        wallet.addCoins(10);

        assertFalse(wallet.spendCoins(10.5));
        assertEquals(10, wallet.getCoins(), DELTA);
    }

    @Test
    void spendCoinsRejectsNegativeAmount() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class, () -> wallet.spendCoins(-1));
    }

    @Test
    void canAffordAcceptsARoundingError() {
        Wallet wallet = new Wallet();
        wallet.addCoins(0.7);
        wallet.addCoins(0.1); // gives 0.7999999999999999, not 0.8

        assertTrue(wallet.canAfford(0.8));
    }

    @Test
    void roundingErrorNeverLeavesANegativeBalance() {
        Wallet wallet = new Wallet();
        wallet.addCoins(0.7);
        wallet.addCoins(0.1);

        assertTrue(wallet.spendCoins(0.8));
        assertTrue(wallet.getCoins() >= 0);
    }

    @Test
    void resetCoinsKeepsTheNuggets() {
        Wallet wallet = new Wallet();
        wallet.addCoins(500);
        wallet.addSugarNuggets(4);

        wallet.resetCoins();

        assertEquals(0, wallet.getCoins(), DELTA);
        assertEquals(4, wallet.getSugarNuggets());
    }

    @Test
    void addSugarNuggetsIncreasesThem() {
        Wallet wallet = new Wallet();

        wallet.addSugarNuggets(2);
        wallet.addSugarNuggets(3);

        assertEquals(5, wallet.getSugarNuggets());
    }

    @Test
    void addSugarNuggetsRejectsNegativeAmount() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class, () -> wallet.addSugarNuggets(-1));
    }
}
