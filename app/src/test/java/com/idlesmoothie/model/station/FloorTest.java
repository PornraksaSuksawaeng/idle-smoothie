package com.idlesmoothie.model.station;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.creature.Pulpie;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.manager.Rarity;
import com.idlesmoothie.model.manager.SpeedBoostAbility;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import com.idlesmoothie.model.resource.Storage;

class FloorTest {

    private static final double DELTA = 1e-9;
    private static final double STEP = 0.01;

    private final BalanceConfig config = new BalanceConfig();

    /** Duration of one full Pulpie trip, plus a small margin for frame rounding. */
    private final double oneTrip = 1 / config.getFloorTripSpeed() + 0.1;

    /** Floor that behaves as if a manager was assigned. */
    private static class AutomatedFloor extends Floor {

        AutomatedFloor(int index, Fruit fruit, BalanceConfig config) {
            super(index, fruit, config);
        }

        @Override
        public boolean isAutomated() {
            return true;
        }
    }

    private static void run(Station station, double seconds) {
        for (double t = 0; t < seconds; t += STEP) {
            station.update(STEP);
        }
    }

    /** Raises a level 1 floor to the first level that unlocks a second Pulpie. */
    private void upgradeToSecondPulpie(Floor floor) {
        floor.upgrade(config.getLevelsPerPulpie());
    }

    @Test
    void newFloorStartsAtLevelOneWithOnePulpie() {
        Floor floor = new Floor(2, Fruit.KIWI, config);

        assertEquals(StationType.FLOOR, floor.getType());
        assertEquals(2, floor.getIndex());
        assertEquals(Fruit.KIWI, floor.getFruit());
        assertEquals(1, floor.getLevel());
        assertEquals(1, floor.getPulpieCount());
        assertEquals(0, floor.getStoredAmount(), DELTA);
    }

    @Test
    void constructorRejectsNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> new Floor(-1, Fruit.KIWI, config));
    }

    @Test
    void constructorRejectsNullFruit() {
        assertThrows(IllegalArgumentException.class, () -> new Floor(0, null, config));
    }

    @Test
    void constructorRejectsNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> new Floor(0, Fruit.KIWI, null));
    }

    @Test
    void pulpieListCannotBeModifiedFromOutside() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        List<Pulpie> pulpies = floor.getPulpies();

        assertThrows(UnsupportedOperationException.class,
                () -> pulpies.add(new Pulpie(Fruit.STRAWBERRY, new Storage(1))));
    }

    @Test
    void firstFloorUsesBaseValues() {
        StationStats stats = new Floor(0, Fruit.STRAWBERRY, config).getStats();

        assertEquals(config.getFloorTripSpeed(), stats.getSpeed(), DELTA);
        assertEquals(config.getFloorBaseCarry(), stats.getCapacity(), DELTA);
    }

    @Test
    void deeperFloorCarriesMore() {
        double first = new Floor(0, Fruit.MANGO, config).getStats().getCapacity();
        double fifth = new Floor(4, Fruit.MANGO, config).getStats().getCapacity();

        assertEquals(Math.pow(config.getFloorDepthProductionGrowth(), 4) * first, fifth, DELTA);
    }

    @Test
    void levelMultipliesCapacity() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        floor.upgrade(4);

        assertEquals(5 * config.getFloorBaseCarry(), floor.getStats().getCapacity(), DELTA);
    }

    @Test
    void upgradeCostStartsAtBaseCost() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        assertEquals(config.getFloorBaseCost(), floor.getUpgradeCost(), DELTA);
    }

    @Test
    void deeperFloorCostsMore() {
        Floor floor = new Floor(2, Fruit.STRAWBERRY, config);

        assertEquals(Math.pow(config.getFloorDepthCostGrowth(), 2) * config.getFloorBaseCost(),
                floor.getUpgradeCost(), DELTA);
    }

    @Test
    void upgradeCostGrowsWithLevel() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        double before = floor.getUpgradeCost();

        floor.upgrade(1);

        assertEquals(before * config.getUpgradeCostGrowth(), floor.getUpgradeCost(), DELTA);
    }

    @Test
    void floorGainsAPulpieAtEachPulpieLevel() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        floor.upgrade(config.getLevelsPerPulpie() - 1);
        assertEquals(1, floor.getPulpieCount());

        floor.upgrade(1);
        assertEquals(Math.min(2, config.getMaxPulpies()), floor.getPulpieCount());
    }

    @Test
    void pulpieCountIsCapped() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        floor.upgrade(config.getLevelsPerPulpie() * (config.getMaxPulpies() + 5));

        assertEquals(config.getMaxPulpies(), floor.getPulpieCount());
    }

    @Test
    void throughputCountsEveryPulpie() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        upgradeToSecondPulpie(floor);
        StationStats stats = floor.getStats();

        double expected = floor.getPulpieCount() * stats.getSpeed() * stats.getCapacity();

        assertEquals(expected, floor.getThroughput(), DELTA);
    }

    @Test
    void upgradeRejectsZeroLevels() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        assertThrows(IllegalArgumentException.class, () -> floor.upgrade(0));
    }

    @Test
    void floorDoesNotProduceWithoutTap() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        run(floor, 3 * oneTrip);

        assertEquals(0, floor.getStoredAmount(), DELTA);
    }

    @Test
    void tapMakesEachPulpieDepositOnce() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        floor.start();
        run(floor, oneTrip);

        assertEquals(floor.getStats().getCapacity(), floor.getStoredAmount(), DELTA);
        assertFalse(floor.isRunning());
    }

    @Test
    void roundIsNotOverWhilePulpiesAreOnTheirWay() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        floor.start();
        run(floor, oneTrip / 2);

        assertTrue(floor.isRunning());
        assertEquals(0, floor.getStoredAmount(), DELTA);
    }

    @Test
    void tapSendsEveryPulpie() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        upgradeToSecondPulpie(floor);

        floor.start();
        run(floor, oneTrip);

        double expected = floor.getPulpieCount() * floor.getStats().getCapacity();
        assertEquals(expected, floor.getStoredAmount(), DELTA);
    }

    @Test
    void floorWaitsForNextTapAfterRound() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        floor.start();
        run(floor, oneTrip);

        run(floor, 3 * oneTrip);

        assertEquals(floor.getStats().getCapacity(), floor.getStoredAmount(), DELTA);
    }

    @Test
    void secondTapStartsAnotherRound() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        floor.start();
        run(floor, oneTrip);

        floor.start();
        run(floor, oneTrip);

        assertEquals(2 * floor.getStats().getCapacity(), floor.getStoredAmount(), DELTA);
    }

    @Test
    void automatedFloorKeepsProducingWithoutTap() {
        Floor floor = new AutomatedFloor(0, Fruit.STRAWBERRY, config);

        run(floor, 2 * oneTrip);

        assertEquals(2 * floor.getStats().getCapacity(), floor.getStoredAmount(), DELTA);
    }

    @Test
    void collectTakesFromCrate() {
        Floor floor = new Floor(0, Fruit.BLUEBERRY, config);
        floor.start();
        run(floor, oneTrip);
        double stored = floor.getStoredAmount();
        double amount = stored / 2;

        List<SmoothieBatch> taken = floor.collect(amount);

        assertEquals(1, taken.size());
        assertEquals(Fruit.BLUEBERRY, taken.get(0).getFruit());
        assertEquals(amount, taken.get(0).getQuantity(), DELTA);
        assertEquals(stored - amount, floor.getStoredAmount(), DELTA);
    }

    @Test
    void collectFromEmptyCrateReturnsNothing() {
        Floor floor = new Floor(0, Fruit.BLUEBERRY, config);

        assertTrue(floor.collect(10).isEmpty());
    }

    @Test
    void managerBonusMakesPulpiesFaster() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        floor.assignManager(new Manager(Rarity.EPIC, StationType.FLOOR, new SpeedBoostAbility(2)));

        run(floor, oneTrip / 2 + 0.1);

        assertEquals(floor.baseStats().getCapacity(), floor.getStoredAmount(), DELTA);
    }

    @Test
    void tapAfterRemovingTheManagerSendsIdlePulpiesAgain() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        Pulpie pulpie = floor.getPulpies().get(0);
        floor.assignManager(new Manager(Rarity.COMMON, StationType.FLOOR, new SpeedBoostAbility(1)));
        floor.update(STEP);
        while (!pulpie.isIdle()) {
            floor.update(STEP);
        }
        double before = floor.getStoredAmount();

        floor.removeManager();
        floor.start();
        run(floor, oneTrip);

        assertEquals(before + floor.getStats().getCapacity(), floor.getStoredAmount(), DELTA);
    }

    @Test
    void baseUpgradeCostDoesNotChangeWithLevel() {
        Floor station = new Floor(2, Fruit.STRAWBERRY, config);
        double base = station.getBaseUpgradeCost();

        station.upgrade(20);

        assertEquals(config.getFloorBaseCost() * Math.pow(config.getFloorDepthCostGrowth(), 2), base, DELTA);
        assertEquals(base, station.getBaseUpgradeCost(), DELTA);
    }

    @Test
    void milestoneMultipliesCapacity() {
        Floor before = new Floor(0, Fruit.STRAWBERRY, config);
        before.upgrade(config.getMilestoneInterval() - 2);
        Floor after = new Floor(0, Fruit.STRAWBERRY, config);
        after.upgrade(config.getMilestoneInterval() - 1);

        double perLevelBefore = before.getStats().getCapacity() / before.getLevel();
        double perLevelAfter = after.getStats().getCapacity() / after.getLevel();

        assertEquals(config.getMilestoneMultiplier() * perLevelBefore, perLevelAfter, DELTA);
    }
}