package com.idlesmoothie.model.station;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationTest {

    private static final double DELTA = 1e-9;

    /** Minimal station that only counts its finished cycles. */
    private static class FakeStation extends Station {

        private final double speed;
        private final double capacity;
        private int cycles;

        FakeStation(double speed, double capacity) {
            this.speed = speed;
            this.capacity = capacity;
        }

        @Override
        public StationType getType() {
            return StationType.FLOOR;
        }

        @Override
        public double getUpgradeCost() {
            return 0;
        }

        @Override
        protected void performCycle() {
            cycles++;
        }

        @Override
        protected StationStats baseStats() {
            return new StationStats(speed, capacity);
        }

        int getCycles() {
            return cycles;
        }
    }

    /** Fake station that behaves as if a manager was assigned. */
    private static class AutomatedStation extends FakeStation {

        AutomatedStation(double speed, double capacity) {
            super(speed, capacity);
        }

        @Override
        public boolean isAutomated() {
            return true;
        }
    }

    @Test
    void newStationStartsIdleAtLevelOne() {
        FakeStation station = new FakeStation(1, 10);

        assertEquals(1, station.getLevel());
        assertEquals(0, station.getProgress(), DELTA);
        assertFalse(station.isRunning());
        assertFalse(station.isAutomated());
    }

    @Test
    void manualStationDoesNothingWithoutTap() {
        FakeStation station = new FakeStation(1, 10);

        station.update(5);

        assertEquals(0, station.getCycles());
        assertEquals(0, station.getProgress(), DELTA);
    }

    @Test
    void tapRunsOneFullCycleThenStops() {
        FakeStation station = new FakeStation(1, 10);

        station.start();
        station.update(1);

        assertEquals(1, station.getCycles());
        assertEquals(0, station.getProgress(), DELTA);
        assertFalse(station.isRunning());
    }

    @Test
    void tapCycleProgressesOverTime() {
        FakeStation station = new FakeStation(1, 10);

        station.start();
        station.update(0.5);

        assertEquals(0, station.getCycles());
        assertEquals(0.5, station.getProgress(), DELTA);
        assertTrue(station.isRunning());
    }

    @Test
    void manualStationRunsOnlyOneCycleEvenWithLongUpdate() {
        FakeStation station = new FakeStation(1, 10);

        station.start();
        station.update(10);

        assertEquals(1, station.getCycles());
        assertEquals(0, station.getProgress(), DELTA);
    }

    @Test
    void tapDuringCycleDoesNotRestartIt() {
        FakeStation station = new FakeStation(1, 10);
        station.start();
        station.update(0.5);

        station.start();

        assertEquals(0.5, station.getProgress(), DELTA);
        assertTrue(station.isRunning());
    }

    @Test
    void newTapAfterCycleStartsAnotherOne() {
        FakeStation station = new FakeStation(1, 10);
        station.start();
        station.update(1);

        station.start();
        station.update(1);

        assertEquals(2, station.getCycles());
    }

    @Test
    void speedScalesProgress() {
        FakeStation station = new FakeStation(2, 10);

        station.start();
        station.update(0.25);

        assertEquals(0.5, station.getProgress(), DELTA);
    }

    @Test
    void zeroElapsedTimeChangesNothing() {
        FakeStation station = new FakeStation(1, 10);
        station.start();

        station.update(0);

        assertEquals(0, station.getProgress(), DELTA);
        assertEquals(0, station.getCycles());
    }

    @Test
    void updateRejectsNegativeTime() {
        FakeStation station = new FakeStation(1, 10);

        assertThrows(IllegalArgumentException.class, () -> station.update(-1));
    }

    @Test
    void automatedStationRunsWithoutTap() {
        AutomatedStation station = new AutomatedStation(1, 10);

        station.update(1);

        assertEquals(1, station.getCycles());
    }

    @Test
    void automatedStationRunsSeveralCyclesAndKeepsLeftover() {
        AutomatedStation station = new AutomatedStation(1, 10);

        station.update(2.5);

        assertEquals(2, station.getCycles());
        assertEquals(0.5, station.getProgress(), DELTA);
    }

    @Test
    void automatedStationAccumulatesSmallUpdates() {
        AutomatedStation station = new AutomatedStation(1, 10);

        station.update(0.4);
        station.update(0.4);
        station.update(0.4);

        assertEquals(1, station.getCycles());
        assertEquals(0.2, station.getProgress(), DELTA);
    }

    @Test
    void tapOnAutomatedStationDoesNothing() {
        AutomatedStation station = new AutomatedStation(1, 10);

        station.start();

        assertFalse(station.isRunning());
    }

    @Test
    void upgradeAddsLevels() {
        FakeStation station = new FakeStation(1, 10);

        station.upgrade(1);
        station.upgrade(10);

        assertEquals(12, station.getLevel());
    }

    @Test
    void upgradeRejectsZeroLevels() {
        FakeStation station = new FakeStation(1, 10);

        assertThrows(IllegalArgumentException.class, () -> station.upgrade(0));
    }

    @Test
    void upgradeRejectsNegativeLevels() {
        FakeStation station = new FakeStation(1, 10);

        assertThrows(IllegalArgumentException.class, () -> station.upgrade(-1));
    }

    @Test
    void getStatsReturnsBaseStatsWithoutBonus() {
        FakeStation station = new FakeStation(2, 10);

        StationStats stats = station.getStats();

        assertEquals(2, stats.getSpeed(), DELTA);
        assertEquals(10, stats.getCapacity(), DELTA);
    }

    @Test
    void throughputIsSpeedTimesCapacity() {
        FakeStation station = new FakeStation(2, 10);

        assertEquals(20, station.getThroughput(), DELTA);
    }
}