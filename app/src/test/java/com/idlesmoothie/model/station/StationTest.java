package com.idlesmoothie.model.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.model.manager.CapacityBoostAbility;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.manager.Rarity;
import com.idlesmoothie.model.manager.SpeedBoostAbility;

class StationTest {

    private static final double DELTA = 1e-9;

    /** Minimal station whose round lasts one second and that records the time it worked. */
    private static class FakeStation extends Station {

        private static final double ROUND_DURATION = 1;

        private final double speed;
        private final double capacity;
        private double workedTime;
        private double roundTime;

        FakeStation(double speed, double capacity) {
            this.speed = speed;
            this.capacity = capacity;
        }

        @Override
        public StationType getType() {
            return StationType.FLOOR;
        }

        @Override
        public double getBaseUpgradeCost() {
            return 0;
        }
        
        @Override
        public double getUpgradeCost() {
            return 0;
        }

        @Override
        protected StationStats baseStats() {
            return new StationStats(speed, capacity);
        }

        @Override
        protected void work(double dt) {
            workedTime += dt;
            roundTime += dt;
            if (roundTime >= ROUND_DURATION) {
                roundTime = 0;
                endRound();
            }
        }

        double getWorkedTime() {
            return workedTime;
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
    void newStationIsIdleAtLevelOne() {
        FakeStation station = new FakeStation(1, 10);

        assertEquals(1, station.getLevel());
        assertFalse(station.isRunning());
        assertFalse(station.isAutomated());
    }

    @Test
    void manualStationDoesNotWorkWithoutTap() {
        FakeStation station = new FakeStation(1, 10);

        station.update(5);

        assertEquals(0, station.getWorkedTime(), DELTA);
    }

    @Test
    void tapStartsARound() {
        FakeStation station = new FakeStation(1, 10);

        station.start();

        assertTrue(station.isRunning());
    }

    @Test
    void tappedStationWorksForElapsedTime() {
        FakeStation station = new FakeStation(1, 10);

        station.start();
        station.update(0.25);
        station.update(0.25);

        assertEquals(0.5, station.getWorkedTime(), DELTA);
        assertTrue(station.isRunning());
    }

    @Test
    void stationStopsWorkingOnceRoundIsDone() {
        FakeStation station = new FakeStation(1, 10);
        station.start();

        station.update(1);
        station.update(1);

        assertFalse(station.isRunning());
        assertEquals(1, station.getWorkedTime(), DELTA);
    }

    @Test
    void tapDuringRoundKeepsItRunning() {
        FakeStation station = new FakeStation(1, 10);
        station.start();
        station.update(0.5);

        station.start();
        station.update(0.5);

        assertFalse(station.isRunning());
        assertEquals(1, station.getWorkedTime(), DELTA);
    }

    @Test
    void newTapAfterRoundStartsAnotherOne() {
        FakeStation station = new FakeStation(1, 10);
        station.start();
        station.update(1);

        station.start();
        station.update(1);

        assertEquals(2, station.getWorkedTime(), DELTA);
    }

    @Test
    void zeroElapsedTimeIsAllowed() {
        FakeStation station = new FakeStation(1, 10);
        station.start();

        station.update(0);

        assertEquals(0, station.getWorkedTime(), DELTA);
        assertTrue(station.isRunning());
    }

    @Test
    void updateRejectsNegativeTime() {
        FakeStation station = new FakeStation(1, 10);

        assertThrows(IllegalArgumentException.class, () -> station.update(-1));
    }

    @Test
    void automatedStationWorksWithoutTap() {
        AutomatedStation station = new AutomatedStation(1, 10);

        station.update(0.5);

        assertEquals(0.5, station.getWorkedTime(), DELTA);
    }

    @Test
    void automatedStationKeepsWorkingAfterRoundEnds() {
        AutomatedStation station = new AutomatedStation(1, 10);

        station.update(1);
        station.update(1);
        station.update(1);

        assertEquals(3, station.getWorkedTime(), DELTA);
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

    private static Manager floorManager(double speedFactor) {
        return new Manager(Rarity.RARE, StationType.FLOOR, new SpeedBoostAbility(speedFactor));
    }

    @Test
    void newStationHasNoManager() {
        FakeStation station = new FakeStation(2, 10);

        assertNull(station.getManager());
        assertFalse(station.isAutomated());
    }

    @Test
    void managerAutomatesTheStation() {
        FakeStation station = new FakeStation(2, 10);

        station.assignManager(floorManager(1.5));

        assertTrue(station.isAutomated());
        station.update(0.5);
        assertEquals(0.5, station.getWorkedTime(), DELTA);
    }

    @Test
    void managerBonusIsAppliedToStats() {
        FakeStation station = new FakeStation(2, 10);

        station.assignManager(floorManager(1.5));

        assertEquals(3, station.getStats().getSpeed(), DELTA);
        assertEquals(10, station.getStats().getCapacity(), DELTA);
        assertEquals(30, station.getThroughput(), DELTA);
    }

    @Test
    void newManagerReplacesTheOldOne() {
        FakeStation station = new FakeStation(2, 10);
        Manager second = new Manager(Rarity.EPIC, StationType.FLOOR, new CapacityBoostAbility(2));

        station.assignManager(floorManager(1.5));
        station.assignManager(second);

        assertSame(second, station.getManager());
        assertEquals(2, station.getStats().getSpeed(), DELTA);
        assertEquals(20, station.getStats().getCapacity(), DELTA);
    }

    @Test
    void managerOfAnotherStationTypeIsRejected() {
        FakeStation station = new FakeStation(2, 10);
        Manager elevatorManager = new Manager(Rarity.RARE, StationType.ELEVATOR, new SpeedBoostAbility(1.5));

        assertThrows(IllegalArgumentException.class, () -> station.assignManager(elevatorManager));
        assertNull(station.getManager());
    }

    @Test
    void nullManagerIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FakeStation(2, 10).assignManager(null));
    }

    @Test
    void removeManagerGoesBackToTaps() {
        FakeStation station = new FakeStation(2, 10);
        station.assignManager(floorManager(1.5));

        station.removeManager();

        assertNull(station.getManager());
        assertFalse(station.isAutomated());
        assertEquals(2, station.getStats().getSpeed(), DELTA);
        station.update(0.5);
        assertEquals(0, station.getWorkedTime(), DELTA);
    }

    @Test
    void removeManagerWithoutManagerDoesNothing() {
        FakeStation station = new FakeStation(2, 10);

        station.removeManager();

        assertFalse(station.isAutomated());
    }

    @Test
    void managerEndsTheTappedRound() {
        FakeStation station = new FakeStation(2, 10);
        station.start();

        station.assignManager(floorManager(1.5));
        station.removeManager();

        assertFalse(station.isRunning());
    }

    @Test
    void everyAssignedManagerIsCounted() {
        FakeStation station = new FakeStation(2, 10);
        assertEquals(0, station.getManagersHired());

        station.assignManager(floorManager(1.5));
        station.assignManager(floorManager(2));
        station.removeManager();

        assertEquals(2, station.getManagersHired());
    }

    @Test
    void restoreManagersHiredSetsTheSavedCount() {
        FakeStation station = new FakeStation(2, 10);
        station.assignManager(floorManager(1.5));

        station.restoreManagersHired(4);

        assertEquals(4, station.getManagersHired());
    }

    @Test
    void restoreManagersHiredRejectsANegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> new FakeStation(2, 10).restoreManagersHired(-1));
    }
}