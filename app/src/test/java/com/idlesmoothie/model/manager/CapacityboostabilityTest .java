package com.idlesmoothie.model.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.model.station.StationStats;

class CapacityBoostAbilityTest {

    private static final double DELTA = 1e-9;
    private static final double SPEED = 2.0;
    private static final double CAPACITY = 10.0;

    private final StationStats stats = new StationStats(SPEED, CAPACITY);

    @Test
    void applyMultipliesOnlyTheCapacity() {
        StationStats boosted = new CapacityBoostAbility(1.5).apply(stats);

        assertEquals(SPEED, boosted.getSpeed(), DELTA);
        assertEquals(CAPACITY * 1.5, boosted.getCapacity(), DELTA);
    }

    @Test
    void applyKeepsTheOriginalStats() {
        new CapacityBoostAbility(2.0).apply(stats);

        assertEquals(SPEED, stats.getSpeed(), DELTA);
        assertEquals(CAPACITY, stats.getCapacity(), DELTA);
    }

    @Test
    void factorOfOneLeavesTheStatsUnchanged() {
        StationStats same = new CapacityBoostAbility(1.0).apply(stats);

        assertEquals(SPEED, same.getSpeed(), DELTA);
        assertEquals(CAPACITY, same.getCapacity(), DELTA);
    }

    @Test
    void factorBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CapacityBoostAbility(0.9));
    }

    @Test
    void applyRejectsNullStats() {
        assertThrows(IllegalArgumentException.class, () -> new CapacityBoostAbility(1.5).apply(null));
    }

    @Test
    void describeShowsTheBonusInPercent() {
        assertEquals("+50% capacity", new CapacityBoostAbility(1.5).describe());
        assertEquals("+175% capacity", new CapacityBoostAbility(2.75).describe());
        assertEquals("+300% capacity", new CapacityBoostAbility(4.0).describe());
    }

    @Test
    void describeShowsAutomationOnlyWithoutBonus() {
        assertEquals("Automation only", new CapacityBoostAbility(1.0).describe());
    }

    @Test
    void factorIsKept() {
        assertEquals(2.75, new CapacityBoostAbility(2.75).getFactor(), DELTA);
    }
}