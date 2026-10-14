package com.idlesmoothie.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoostTest {

    private static final double DELTA = 1e-9;

    @Test
    void newBoostHasItsFullDuration() {
        Boost boost = new Boost("Potion", 2, 30);

        assertEquals("Potion", boost.getName());
        assertEquals(2, boost.getMultiplier(), DELTA);
        assertEquals(30, boost.getRemaining(), DELTA);
        assertFalse(boost.isExpired());
    }

    @Test
    void updateRemovesTheElapsedTime() {
        Boost boost = new Boost("Potion", 2, 30);

        boost.update(10);

        assertEquals(20, boost.getRemaining(), DELTA);
        assertFalse(boost.isExpired());
    }

    @Test
    void boostExpiresExactlyAtTheEnd() {
        Boost boost = new Boost("Potion", 2, 30);

        boost.update(30);

        assertTrue(boost.isExpired());
    }

    @Test
    void remainingTimeNeverGoesNegative() {
        Boost boost = new Boost("Potion", 2, 30);

        boost.update(45);

        assertEquals(0, boost.getRemaining(), DELTA);
        assertTrue(boost.isExpired());
    }

    @Test
    void roundingErrorDoesNotKeepABoostAlive() {
        Boost boost = new Boost("Potion", 2, 1);

        for (int frame = 0; frame < 10; frame++) {
            boost.update(0.1); // leaves 1.3877787807814457E-16, not 0
        }

        assertTrue(boost.isExpired());
    }

    @Test
    void multiplierMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Boost("Potion", 0, 30));
        assertThrows(IllegalArgumentException.class, () -> new Boost("Potion", -2, 30));
    }

    @Test
    void durationMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Boost("Potion", 2, 0));
        assertThrows(IllegalArgumentException.class, () -> new Boost("Potion", 2, -1));
    }

    @Test
    void nameIsRequired() {
        assertThrows(NullPointerException.class, () -> new Boost(null, 2, 30));
    }
}
