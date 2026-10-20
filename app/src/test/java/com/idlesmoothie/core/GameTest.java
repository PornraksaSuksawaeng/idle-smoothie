package com.idlesmoothie.core;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.model.economy.BuyMode;
import com.idlesmoothie.model.economy.PriceCalculator;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.manager.ManagerFactory;
import com.idlesmoothie.model.manager.Rarity;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Mine;
import com.idlesmoothie.model.station.Station;
import com.idlesmoothie.model.station.StationType;

class GameTest {

    private static final double DELTA = 1e-9;
    private static final double STEP = 0.01;
    private static final double UNIT_PRICE = 2;

    private final BalanceConfig config = new BalanceConfig();
    private final PriceCalculator prices = batch -> batch.getQuantity() * UNIT_PRICE;
    private final Wallet wallet = new Wallet();

    private Game newGame() {
        return new Game(config, prices, wallet);
    }

    private static void run(Game game, double seconds) {
        for (double t = 0; t < seconds; t += STEP) {
            game.update(STEP);
        }
    }

    @Test
    void defaultGameStartsWithOneFloorAndNoCoins() {
        Game game = new Game();

        assertEquals(1, game.getMine().getFloors().size());
        assertEquals(0, game.getCoins(), DELTA);
    }

    @Test
    void gameWithConfigUsesTheBasePrice() {
        Game game = new Game(config);

        game.tapFloor(0);
        run(game, 1 / config.getFloorTripSpeed() + 0.1);
        double produced = game.getMine().getFloors().get(0).getStoredAmount();
        game.tapElevator();
        run(game, 2 / config.getElevatorMoveSpeed() + 0.1);
        game.tapSellStation();
        run(game, 1 / config.getSellSpeed() + 0.1);

        assertEquals(produced * config.getSmoothieBasePrice(), game.getCoins(), DELTA);
    }

    @Test
    void gameWithNullConfigFails() {
        assertThrows(NullPointerException.class, () -> new Game((BalanceConfig) null));
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Game(null, prices, wallet));
        assertThrows(IllegalArgumentException.class, () -> new Game(config, null, wallet));
        assertThrows(IllegalArgumentException.class, () -> new Game(config, prices, null));
    }

    @Test
    void tapsTurnSmoothieIntoCoins() {
        Game game = newGame();

        game.tapFloor(0);
        run(game, 1 / config.getFloorTripSpeed() + 0.1);
        double produced = game.getMine().getFloors().get(0).getStoredAmount();
        game.tapElevator();
        run(game, 2 / config.getElevatorMoveSpeed() + 0.1);
        game.tapSellStation();
        run(game, 1 / config.getSellSpeed() + 0.1);

        assertTrue(produced > 0);
        assertEquals(produced * UNIT_PRICE, game.getCoins(), DELTA);
    }

    @Test
    void nothingHappensWithoutTaps() {
        Game game = newGame();

        run(game, 10);

        assertEquals(0, game.getCoins(), DELTA);
        assertEquals(0, game.getMine().getFloors().get(0).getStoredAmount(), DELTA);
    }

    @Test
    void tapFloorRejectsLockedFloor() {
        Game game = newGame();

        assertThrows(IllegalArgumentException.class, () -> game.tapFloor(-1));
        assertThrows(IllegalArgumentException.class, () -> game.tapFloor(1));
    }

    @Test
    void cannotBuyFloorWithoutEnoughCoins() {
        Game game = newGame();
        wallet.addCoins(game.getMine().getNextFloorCost() - 1);

        assertFalse(game.canBuyNextFloor());
        assertNull(game.buyNextFloor());
        assertEquals(1, game.getMine().getFloors().size());
        assertEquals(game.getMine().getNextFloorCost() - 1, game.getCoins(), DELTA);
    }

    @Test
    void buyingFloorPaysAndUnlocksIt() {
        Game game = newGame();
        double cost = game.getMine().getNextFloorCost();
        wallet.addCoins(cost + 10);

        Floor floor = game.buyNextFloor();

        assertEquals(2, game.getMine().getFloors().size());
        assertEquals(1, floor.getIndex());
        assertEquals(10, game.getCoins(), DELTA);
    }

    @Test
    void cannotBuyBarrierFloorBelowMinLevel() {
        Game game = newGame();
        wallet.addCoins(1e12);
        while (!game.getMine().isBehindBarrier(game.getMine().getFloors().size())) {
            game.buyNextFloor();
        }
        double coins = game.getCoins();

        assertFalse(game.canBuyNextFloor());
        assertNull(game.buyNextFloor());
        assertEquals(coins, game.getCoins(), DELTA);
    }

    @Test
    void updateRejectsNegativeTime() {
        assertThrows(IllegalArgumentException.class, () -> newGame().update(-1));
    }

    @Test
    void upgradeWithEnoughCoinsPaysAndLevelsUp() {
        Game game = newGame();
        Station floor = game.getMine().getFloors().get(0);
        wallet.addCoins(floor.getUpgradeCost() + 10);

        assertTrue(game.canUpgrade(floor, BuyMode.X1));
        game.upgrade(floor, BuyMode.X1);

        assertEquals(2, floor.getLevel());
        assertEquals(10, game.getCoins(), DELTA);
    }

    @Test
    void upgradeWithoutEnoughCoinsChangesNothing() {
        Game game = newGame();
        Station elevator = game.getMine().getElevator();
        wallet.addCoins(elevator.getUpgradeCost() - 1);
        double coins = game.getCoins();

        assertFalse(game.canUpgrade(elevator, BuyMode.X1));
        game.upgrade(elevator, BuyMode.X1);

        assertEquals(1, elevator.getLevel());
        assertEquals(coins, game.getCoins(), DELTA);
    }

    @Test
    void tenLevelsCostMoreThanTenTimesOne() {
        Game game = newGame();
        Station sell = game.getMine().getSellStation();

        assertTrue(game.getUpgradeCost(sell, BuyMode.X10) > 10 * game.getUpgradeCost(sell, BuyMode.X1));
    }

    @Test
    void tenLevelsUpgradeAddsTenLevels() {
        Game game = newGame();
        Station sell = game.getMine().getSellStation();
        wallet.addCoins(game.getUpgradeCost(sell, BuyMode.X10));

        game.upgrade(sell, BuyMode.X10);

        assertEquals(11, sell.getLevel());
        assertEquals(0, game.getCoins(), 1e-6);
    }

    @Test
    void maxBuysEveryAffordableLevel() {
        Game game = newGame();
        Station floor = game.getMine().getFloors().get(0);
        double fiveLevels = floor.getUpgradeCost()
                * (Math.pow(config.getUpgradeCostGrowth(), 5) - 1) / (config.getUpgradeCostGrowth() - 1);
        wallet.addCoins(fiveLevels + 1);

        game.upgrade(floor, BuyMode.MAX);

        assertEquals(6, floor.getLevel());
        assertFalse(game.canUpgrade(floor, BuyMode.MAX));
    }

    @Test
    void upgradeRejectsNullArguments() {
        Game game = newGame();
        Station floor = game.getMine().getFloors().get(0);

        assertThrows(IllegalArgumentException.class, () -> game.upgrade(null, BuyMode.X1));
        assertThrows(IllegalArgumentException.class, () -> game.upgrade(floor, null));
        assertThrows(IllegalArgumentException.class, () -> game.getUpgradeCost(null, BuyMode.X1));
        assertThrows(IllegalArgumentException.class, () -> game.canUpgrade(floor, null));
    }

    @Test
    void bottleneckComesFromTheMine() {
        Game game = newGame();

        assertEquals(game.getMine().findBottleneck(), game.getBottleneck());
        assertEquals(StationType.FLOOR, game.getBottleneck());
    }

    /** Game whose managers are always the most common rarity with a speed bonus. */
    private Game newGameWithFixedRolls() {
        return new Game(config, prices, wallet, new ManagerFactory(() -> 0.0));
    }

    @Test
    void firstManagerCostsAMultipleOfTheNextUpgrade() {
        Game game = newGame();
        Station floor = game.getMine().getFloors().get(0);

        assertEquals(floor.getUpgradeCost() * config.getManagerCostMultiplier(), game.getManagerCost(floor), DELTA);
    }

    @Test
    void hiringPaysAndAutomatesTheStation() {
        Game game = newGameWithFixedRolls();
        Station floor = game.getMine().getFloors().get(0);
        wallet.addCoins(game.getManagerCost(floor) + 10);

        assertTrue(game.canHireManager(floor));
        Manager manager = game.hireManager(floor);

        assertSame(manager, floor.getManager());
        assertEquals(Rarity.COMMON, manager.getRarity());
        assertTrue(floor.isAutomated());
        assertEquals(10, game.getCoins(), DELTA);
    }

    @Test
    void hiredManagerWorksOnTheRightStationType() {
        Game game = newGameWithFixedRolls();
        wallet.addCoins(1e9);

        assertEquals(StationType.ELEVATOR, game.hireManager(game.getMine().getElevator()).getStationType());
        assertEquals(StationType.SELL, game.hireManager(game.getMine().getSellStation()).getStationType());
    }

    @Test
    void eachNewManagerOnAStationCostsMore() {
        Game game = newGameWithFixedRolls();
        Station elevator = game.getMine().getElevator();
        double first = game.getManagerCost(elevator);
        wallet.addCoins(1e9);

        Manager old = game.hireManager(elevator);
        Manager replacement = game.hireManager(elevator);

        assertSame(replacement, elevator.getManager());
        assertTrue(old != replacement);
        assertEquals(first * config.getManagerCostGrowth() * config.getManagerCostGrowth(),
                game.getManagerCost(elevator), DELTA);
    }

    @Test
    void hiringWithoutEnoughCoinsChangesNothing() {
        Game game = newGameWithFixedRolls();
        Station sell = game.getMine().getSellStation();
        wallet.addCoins(game.getManagerCost(sell) - 1);
        double coins = game.getCoins();

        assertFalse(game.canHireManager(sell));
        assertNull(game.hireManager(sell));

        assertNull(sell.getManager());
        assertEquals(coins, game.getCoins(), DELTA);
    }

    @Test
    void automatedChainEarnsCoinsWithoutTaps() {
        Game game = newGameWithFixedRolls();
        wallet.addCoins(1e9);
        game.hireManager(game.getMine().getFloors().get(0));
        game.hireManager(game.getMine().getElevator());
        game.hireManager(game.getMine().getSellStation());
        double coins = game.getCoins();

        run(game, 30);

        assertTrue(game.getCoins() > coins);
    }

    @Test
    void managerMethodsRejectNullStation() {
        Game game = newGame();

        assertThrows(IllegalArgumentException.class, () -> game.getManagerCost(null));
        assertThrows(IllegalArgumentException.class, () -> game.canHireManager(null));
        assertThrows(IllegalArgumentException.class, () -> game.hireManager(null));
    }

    @Test
    void constructorRejectsNullFactory() {
        assertThrows(IllegalArgumentException.class, () -> new Game(config, prices, wallet, null));
    }

    /** A game with a barrier crossed, upgrades and managers, one of them rerolled. */
    private Game playedGame() {
        Wallet richWallet = new Wallet();
        Game game = new Game(config, prices, richWallet, new ManagerFactory());
        richWallet.addCoins(1e12);
        Mine mine = game.getMine();
        while (mine.getFloors().size() <= config.getFloorsPerFruit()) {
            Floor deepest = mine.getDeepestFloor();
            if (deepest.getLevel() < config.getBarrierMinLevel()) {
                deepest.upgrade(config.getBarrierMinLevel() - deepest.getLevel());
            }
            game.buyNextFloor();
        }
        game.upgrade(mine.getElevator(), BuyMode.X10);
        game.hireManager(mine.getFloors().get(1));
        game.hireManager(mine.getElevator());
        game.hireManager(mine.getElevator());
        return game;
    }

    @Test
    void restoreGivesBackTheSavedGame() {
        GameSnapshot saved = playedGame().createSnapshot();

        Game loaded = newGame();
        loaded.restore(saved);

        assertEquals(saved, loaded.createSnapshot());
    }

    @Test
    void restoredManagersWorkAndKeepTheirPrice() {
        Game played = playedGame();

        Game loaded = newGame();
        loaded.restore(played.createSnapshot());

        assertTrue(loaded.getMine().getElevator().isAutomated());
        assertEquals(played.getManagerCost(played.getMine().getElevator()),
                loaded.getManagerCost(loaded.getMine().getElevator()), DELTA);
    }

    @Test
    void invalidSnapshotLeavesTheGameUntouched() {
        Game game = newGame();
        GameSnapshot before = game.createSnapshot();
        ManagerSnapshot ghost = new ManagerSnapshot(StationType.FLOOR, 7, 1, UUID.randomUUID(),
                Rarity.RARE, true, 1.5);

        assertThrows(IllegalArgumentException.class,
                () -> game.restore(new GameSnapshot(-1, List.of(1), 1, 1, List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> game.restore(new GameSnapshot(500, List.of(1), 1, 1, List.of(ghost))));

        assertEquals(before, game.createSnapshot());
    }
}