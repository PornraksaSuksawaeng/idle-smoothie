package com.idlesmoothie.model.station;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.economy.PriceCalculator;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.resource.Fruit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineTest {

    private static final double DELTA = 1e-9;
    private static final double STEP = 0.01;
    private static final double UNIT_PRICE = 2;

    private final BalanceConfig config = new BalanceConfig();
    private final PriceCalculator prices = batch -> batch.getQuantity() * UNIT_PRICE;
    private final Wallet wallet = new Wallet();

    private Mine newMine() {
        return new Mine(prices, wallet, config);
    }

    private static void run(Mine mine, double seconds) {
        for (double t = 0; t < seconds; t += STEP) {
            mine.update(STEP);
        }
    }

    /** Unlocks floors until the mine has the given count, upgrading the deepest floor before barriers. */
    private void growTo(Mine mine, int floorCount) {
        while (mine.getFloors().size() < floorCount) {
            if (!mine.canUnlockNextFloor()) {
                Floor deepest = mine.getDeepestFloor();
                deepest.upgrade(config.getBarrierMinLevel() - deepest.getLevel());
            }
            mine.unlockNextFloor();
        }
    }

    @Test
    void newMineHasOneFloorAndEmptySurface() {
        Mine mine = newMine();

        assertEquals(1, mine.getFloors().size());
        assertEquals(Fruit.values()[0], mine.getFloors().get(0).getFruit());
        assertEquals(0, mine.getSurfaceAmount(), DELTA);
        assertTrue(mine.hasNextFloor());
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Mine(null, wallet, config));
        assertThrows(IllegalArgumentException.class, () -> new Mine(prices, null, config));
        assertThrows(IllegalArgumentException.class, () -> new Mine(prices, wallet, null));
    }

    @Test
    void maxFloorsIsOneZonePerFruit() {
        assertEquals(Fruit.values().length * config.getFloorsPerFruit(), newMine().getMaxFloors());
    }

    @Test
    void fruitChangesEveryZone() {
        Mine mine = newMine();
        int perFruit = config.getFloorsPerFruit();

        assertEquals(Fruit.values()[0], mine.fruitForFloor(perFruit - 1));
        assertEquals(Fruit.values()[1], mine.fruitForFloor(perFruit));
        assertEquals(Fruit.values()[Fruit.values().length - 1], mine.fruitForFloor(mine.getMaxFloors() - 1));
    }

    @Test
    void fruitForFloorRejectsIndexOutsideTheMine() {
        Mine mine = newMine();

        assertThrows(IllegalArgumentException.class, () -> mine.fruitForFloor(-1));
        assertThrows(IllegalArgumentException.class, () -> mine.fruitForFloor(mine.getMaxFloors()));
    }

    @Test
    void barriersStandBeforeEachNewZone() {
        Mine mine = newMine();
        int perFruit = config.getFloorsPerFruit();

        assertFalse(mine.isBehindBarrier(0));
        assertFalse(mine.isBehindBarrier(perFruit - 1));
        assertTrue(mine.isBehindBarrier(perFruit));
        assertTrue(mine.isBehindBarrier(2 * perFruit));
    }

    @Test
    void unlockAddsNextFloorWithItsFruit() {
        Mine mine = newMine();

        Floor floor = mine.unlockNextFloor();

        assertEquals(2, mine.getFloors().size());
        assertEquals(1, floor.getIndex());
        assertEquals(mine.fruitForFloor(1), floor.getFruit());
        assertEquals(1, floor.getLevel());
        assertSame(floor, mine.getDeepestFloor());
    }

    @Test
    void nextFloorCostGrowsWithDepth() {
        Mine mine = newMine();
        assertEquals(config.getFloorPrice(1), mine.getNextFloorCost(), DELTA);

        mine.unlockNextFloor();

        assertEquals(config.getFloorPrice(2), mine.getNextFloorCost(), DELTA);
        assertTrue(config.getFloorPrice(2) > config.getFloorPrice(1));
    }

    @Test
    void floorBehindBarrierCostsMore() {
        Mine mine = newMine();
        int barrier = config.getFloorsPerFruit();
        growTo(mine, barrier);

        double normal = config.getFloorPrice(barrier);

        assertEquals(normal * config.getBarrierCostMultiplier(), mine.getNextFloorCost(), DELTA);
    }

    @Test
    void barrierNeedsDeepestFloorLevel() {
        Mine mine = newMine();
        growTo(mine, config.getFloorsPerFruit());
        Floor deepest = mine.getDeepestFloor();

        deepest.upgrade(config.getBarrierMinLevel() - 2);
        assertFalse(mine.canUnlockNextFloor());
        assertThrows(IllegalStateException.class, mine::unlockNextFloor);

        deepest.upgrade(1);
        assertTrue(mine.canUnlockNextFloor());
    }

    @Test
    void completeMineCannotGrow() {
        Mine mine = newMine();
        growTo(mine, mine.getMaxFloors());

        assertFalse(mine.hasNextFloor());
        assertFalse(mine.canUnlockNextFloor());
        assertThrows(IllegalStateException.class, mine::getNextFloorCost);
        assertThrows(IllegalStateException.class, mine::unlockNextFloor);
    }

    @Test
    void floorsListIsReadOnly() {
        Mine mine = newMine();

        assertThrows(UnsupportedOperationException.class, () -> mine.getFloors().clear());
    }

    @Test
    void elevatorServesNewFloors() {
        Mine mine = newMine();
        mine.unlockNextFloor();

        mine.getElevator().start();
        run(mine, 1.5 / config.getElevatorMoveSpeed());

        assertTrue(mine.getElevator().getPosition() > 1);
    }

    @Test
    void tapsMoveSmoothieFromFloorToWallet() {
        Mine mine = newMine();
        Floor floor = mine.getFloors().get(0);

        floor.start();
        run(mine, 1 / config.getFloorTripSpeed() + 0.1);
        double produced = floor.getStoredAmount();
        assertTrue(produced > 0);

        mine.getElevator().start();
        run(mine, 2 / config.getElevatorMoveSpeed() + 0.1);
        assertEquals(produced, mine.getSurfaceAmount(), DELTA);

        mine.getSellStation().start();
        run(mine, 1 / config.getSellSpeed() + 0.1);
        assertEquals(0, mine.getSurfaceAmount(), DELTA);
        assertEquals(produced * UNIT_PRICE, wallet.getCoins(), DELTA);
    }

    @Test
    void bottleneckIsTheSlowestStation() {
        Mine mine = newMine();
        assertEquals(StationType.FLOOR, mine.findBottleneck());

        Floor floor = mine.getFloors().get(0);
        floor.upgrade(floor.getLevelsLeft());
        assertTrue(mine.findBottleneck() != StationType.FLOOR);

        mine.getElevator().upgrade(mine.getElevator().getLevelsLeft());
        assertEquals(StationType.SELL, mine.findBottleneck());
    }

    @Test
    void updateRejectsNegativeTime() {
        assertThrows(IllegalArgumentException.class, () -> newMine().update(-1));
    }
}