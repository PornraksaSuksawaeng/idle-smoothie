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
        floor.upgrade(config.getMilestoneLevels().get(0) - 1);
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
        assertEquals(config.getFloorBaseCarry(0), stats.getCapacity(), DELTA);
    }

    @Test
    void deeperFloorCarriesMore() {
        double fifth = new Floor(4, Fruit.MANGO, config).getStats().getCapacity();

        assertEquals(config.getFloorBaseCarry(4), fifth, DELTA);
        assertTrue(fifth > new Floor(3, Fruit.MANGO, config).getStats().getCapacity());
    }

    @Test
    void levelMultipliesCapacity() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        floor.upgrade(4);

        assertEquals(Math.pow(config.getStatGrowth(), 4) * config.getFloorBaseCarry(0),
                floor.getStats().getCapacity(), DELTA);
    }

    @Test
    void upgradeCostStartsAtBaseCost() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        assertEquals(config.getFloorPrice(0) * config.getFloorUpgradeCostRatio(), floor.getUpgradeCost(), DELTA);
    }

    @Test
    void deeperFloorCostsMore() {
        Floor floor = new Floor(2, Fruit.STRAWBERRY, config);

        assertEquals(config.getFloorPrice(2) * config.getFloorUpgradeCostRatio(), floor.getUpgradeCost(), DELTA);
    }

    @Test
    void upgradeCostGrowsWithLevel() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        double before = floor.getUpgradeCost();

        floor.upgrade(1);

        assertEquals(before * config.getUpgradeCostGrowth(), floor.getUpgradeCost(), DELTA);
    }

    @Test
    void floorGainsAPulpieAtEachMilestone() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        int count = 1;
        for (int milestone : config.getMilestoneLevels()) {
            floor.upgrade(milestone - 1 - floor.getLevel());
            assertEquals(count, floor.getPulpieCount());

            floor.upgrade(1);
            count = Math.min(count + 1, config.getMaxPulpies());
            assertEquals(count, floor.getPulpieCount());
        }
    }

    @Test
    void levelStopsAtMaxLevel() {
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);

        floor.upgrade(config.getMaxLevel() - 1);

        assertEquals(0, floor.getLevelsLeft());
        assertThrows(IllegalArgumentException.class, () -> floor.upgrade(1));
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

        assertEquals(config.getFloorPrice(2) * config.getFloorUpgradeCostRatio(), base, DELTA);
        assertEquals(base, station.getBaseUpgradeCost(), DELTA);
    }

    @Test
    void milestoneMultipliesCapacity() {
        int milestone = config.getMilestoneLevels().get(0);
        Floor before = new Floor(0, Fruit.STRAWBERRY, config);
        before.upgrade(milestone - 2);
        Floor after = new Floor(0, Fruit.STRAWBERRY, config);
        after.upgrade(milestone - 1);

        double expected = before.getStats().getCapacity() * config.getStatGrowth() * config.getMilestoneMultiplier();
        assertEquals(expected, after.getStats().getCapacity(), DELTA * expected);
    }

    @Test
    void previewMatchesAnUpgradeWithoutChangingTheFloor() {
        for (int extra : new int[] {1, 24, 60}) {
            Floor floor = new Floor(1, Fruit.KIWI, config);
            StationStats preview = floor.previewStats(extra);
            double throughput = floor.previewThroughput(extra);
            int pulpies = floor.previewPulpieCount(extra);

            assertEquals(1, floor.getLevel());
            assertEquals(1, floor.getPulpieCount());

            floor.upgrade(extra);
            assertEquals(floor.getStats().getSpeed(), preview.getSpeed(), DELTA);
            assertEquals(floor.getStats().getCapacity(), preview.getCapacity(), DELTA);
            assertEquals(floor.getThroughput(), throughput, DELTA);
            assertEquals(floor.getPulpieCount(), pulpies);
        }
    }

    @Test
    void previewCountsTheMilestonePulpie() {
        Floor floor = new Floor(0, Fruit.KIWI, config);
        int toMilestone = config.getMilestoneLevels().get(0) - floor.getLevel();

        assertEquals(1, floor.previewPulpieCount(toMilestone - 1));
        assertEquals(2, floor.previewPulpieCount(toMilestone));
    }

    @Test
    void previewIncludesTheManagerBonus() {
        Floor floor = new Floor(0, Fruit.KIWI, config);
        floor.assignManager(new Manager(Rarity.EPIC, StationType.FLOOR, new SpeedBoostAbility(2)));
        StationStats preview = floor.previewStats(5);

        floor.upgrade(5);

        assertEquals(floor.getStats().getSpeed(), preview.getSpeed(), DELTA);
        assertEquals(2 * config.getFloorTripSpeed(), preview.getSpeed(), DELTA);
    }

    @Test
    void crateIsEmptyUntilAPulpieComesBack() {
        Floor floor = new Floor(0, Fruit.KIWI, config);
        assertTrue(floor.isCrateEmpty());

        floor.start();
        run(floor, oneTrip);

        assertFalse(floor.isCrateEmpty());
    }
}
