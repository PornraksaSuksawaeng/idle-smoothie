package com.idlesmoothie.model.station;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StationStatsTest {

    private static final double DELTA = 1e-9;

    @Test
    void constructorStoresValues() {
        StationStats stats = new StationStats(2, 10);

        assertEquals(2, stats.getSpeed(), DELTA);
        assertEquals(10, stats.getCapacity(), DELTA);
    }

    @Test
    void zeroValuesAreAllowed() {
        StationStats stats = new StationStats(0, 0);

        assertEquals(0, stats.getSpeed(), DELTA);
        assertEquals(0, stats.getCapacity(), DELTA);
    }

    @Test
    void constructorRejectsNegativeSpeed() {
        assertThrows(IllegalArgumentException.class, () -> new StationStats(-1, 10));
    }

    @Test
    void constructorRejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new StationStats(2, -1));
    }

    @Test
    void withSpeedMultipliesSpeedOnly() {
        StationStats boosted = new StationStats(2, 10).withSpeed(1.5);

        assertEquals(3, boosted.getSpeed(), DELTA);
        assertEquals(10, boosted.getCapacity(), DELTA);
    }

    @Test
    void withCapacityMultipliesCapacityOnly() {
        StationStats boosted = new StationStats(2, 10).withCapacity(2);

        assertEquals(2, boosted.getSpeed(), DELTA);
        assertEquals(20, boosted.getCapacity(), DELTA);
    }

    @Test
    void withMethodsReturnNewInstance() {
        StationStats stats = new StationStats(2, 10);

        assertNotSame(stats, stats.withSpeed(1));
        assertNotSame(stats, stats.withCapacity(1));
    }

    @Test
    void withMethodsLeaveOriginalUnchanged() {
        StationStats stats = new StationStats(2, 10);

        stats.withSpeed(3);
        stats.withCapacity(3);

        assertEquals(2, stats.getSpeed(), DELTA);
        assertEquals(10, stats.getCapacity(), DELTA);
    }

    @Test
    void withMethodsCanBeChained() {
        StationStats boosted = new StationStats(2, 10).withSpeed(2).withCapacity(3).withSpeed(1.5);

        assertEquals(6, boosted.getSpeed(), DELTA);
        assertEquals(30, boosted.getCapacity(), DELTA);
    }

    @Test
    void factorOfZeroSetsStatToZero() {
        StationStats stats = new StationStats(2, 10);

        assertEquals(0, stats.withSpeed(0).getSpeed(), DELTA);
        assertEquals(0, stats.withCapacity(0).getCapacity(), DELTA);
    }

    @Test
    void withSpeedRejectsNegativeFactor() {
        StationStats stats = new StationStats(2, 10);

        assertThrows(IllegalArgumentException.class, () -> stats.withSpeed(-1));
    }

    @Test
    void withCapacityRejectsNegativeFactor() {
        StationStats stats = new StationStats(2, 10);

        assertThrows(IllegalArgumentException.class, () -> stats.withCapacity(-1));
    }

    @Test
    void negativeFactorIsRejectedEvenWhenStatIsZero() {
        StationStats stats = new StationStats(0, 0);

        assertThrows(IllegalArgumentException.class, () -> stats.withSpeed(-1));
        assertThrows(IllegalArgumentException.class, () -> stats.withCapacity(-1));
    }
}