package com.idlesmoothie.model.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.model.station.StationStats;

class SpeedBoostAbilityTest {

    private static final double DELTA = 1e-9;
    private static final double SPEED = 2.0;
    private static final double CAPACITY = 10.0;

    private final StationStats stats = new StationStats(SPEED, CAPACITY);

    @Test
    void applyMultipliesOnlyTheSpeed() {
        StationStats boosted = new SpeedBoostAbility(1.5).apply(stats);

        assertEquals(SPEED * 1.5, boosted.getSpeed(), DELTA);
        assertEquals(CAPACITY, boosted.getCapacity(), DELTA);
    }

    @Test
    void applyKeepsTheOriginalStats() {
        new SpeedBoostAbility(2.0).apply(stats);

        assertEquals(SPEED, stats.getSpeed(), DELTA);
        assertEquals(CAPACITY, stats.getCapacity(), DELTA);
    }

    @Test
    void factorOfOneLeavesTheStatsUnchanged() {
        StationStats same = new SpeedBoostAbility(1.0).apply(stats);

        assertEquals(SPEED, same.getSpeed(), DELTA);
        assertEquals(CAPACITY, same.getCapacity(), DELTA);
    }

    @Test
    void factorBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SpeedBoostAbility(0.9));
    }

    @Test
    void applyRejectsNullStats() {
        assertThrows(IllegalArgumentException.class, () -> new SpeedBoostAbility(1.5).apply(null));
    }

    @Test
    void describeShowsTheBonusInPercent() {
        assertEquals("+50% speed", new SpeedBoostAbility(1.5).describe());
        assertEquals("+175% speed", new SpeedBoostAbility(2.75).describe());
        assertEquals("+300% speed", new SpeedBoostAbility(4.0).describe());
    }

    @Test
    void describeShowsAutomationOnlyWithoutBonus() {
        assertEquals("Automation only", new SpeedBoostAbility(1.0).describe());
    }

    @Test
    void factorIsKept() {
        assertEquals(2.75, new SpeedBoostAbility(2.75).getFactor(), DELTA);
    }
}