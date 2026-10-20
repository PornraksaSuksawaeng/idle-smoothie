package com.idlesmoothie.model.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RarityTest {

    private static final double DELTA = 1e-9;

    @Test
    void everyRarityCanBeRolledAndNeverWeakensAStation() {
        for (Rarity rarity : Rarity.values()) {
            assertTrue(rarity.getWeight() > 0, rarity + " must have a positive weight");
            assertTrue(rarity.getBonusMultiplier() >= 1, rarity + " must not weaken a station");
        }
    }

    @Test
    void rarerMeansLessLikelyAndStronger() {
        Rarity[] rarities = Rarity.values();
        for (int i = 1; i < rarities.length; i++) {
            Rarity previous = rarities[i - 1];
            Rarity current = rarities[i];
            assertTrue(current.getWeight() < previous.getWeight(), current + " must be rarer than " + previous);
            assertTrue(current.getBonusMultiplier() > previous.getBonusMultiplier(),
                    current + " must be stronger than " + previous);
        }
    }

    @Test
    void commonOnlyAutomates() {
        assertEquals(1.0, Rarity.COMMON.getBonusMultiplier(), DELTA);
    }

    @Test
    void weightsAddUpToOneThousand() {
        double total = 0;
        for (Rarity rarity : Rarity.values()) {
            total += rarity.getWeight();
        }
        assertEquals(1000, total, DELTA);
    }

    @Test
    void everyRarityHasAHexColor() {
        for (Rarity rarity : Rarity.values()) {
            assertTrue(rarity.getColorHex().matches("#[0-9A-F]{6}"), rarity + " has an invalid color");
        }
    }
}