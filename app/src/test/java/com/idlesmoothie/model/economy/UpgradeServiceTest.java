package com.idlesmoothie.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpgradeServiceTest {

    private static final double DELTA = 1e-9;
    private static final double GROWTH = 1.13;
    private static final double BASE_COST = 10;

    private Wallet wallet;
    private UpgradeService upgrades;
    private FakeStation station;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        upgrades = new UpgradeService(wallet, GROWTH);
        station = new FakeStation(BASE_COST);
    }

    @Test
    void x1BuysOneLevelForTheCostOfTheNextLevel() {
        wallet.addCoins(100);

        UpgradeReport report = upgrades.buy(station, BuyMode.X1);

        assertEquals(1, report.levelsGained());
        assertEquals(BASE_COST, report.coinsSpent(), DELTA);
        assertEquals(1, station.level);
        assertEquals(100 - BASE_COST, wallet.getCoins(), DELTA);
    }

    @Test
    void x10CostsTheNextTenLevelsAddedTogether() {
        station.level = 3;

        assertEquals(costOfLevels(3, 10), upgrades.previewCost(station, BuyMode.X10), DELTA);
    }

    @Test
    void x10BuysNothingWhenTheWalletCannotPayAllTen() {
        wallet.addCoins(costOfLevels(0, 10) - 1);
        double before = wallet.getCoins();

        UpgradeReport report = upgrades.buy(station, BuyMode.X10);

        assertEquals(0, report.levelsGained());
        assertEquals(0, report.coinsSpent(), DELTA);
        assertEquals(0, station.level);
        assertEquals(before, wallet.getCoins(), DELTA);
    }

    @Test
    void maxBuysEveryLevelTheWalletCanPay() {
        wallet.addCoins(costOfLevels(0, 5));

        UpgradeReport report = upgrades.buy(station, BuyMode.MAX);

        assertEquals(5, report.levelsGained());
        assertEquals(5, station.level);
        assertEquals(0, wallet.getCoins(), DELTA);
    }

    @Test
    void maxShowsTheNextLevelCostWhenTheWalletIsEmpty() {
        assertEquals(BASE_COST, upgrades.previewCost(station, BuyMode.MAX), DELTA);

        UpgradeReport report = upgrades.buy(station, BuyMode.MAX);

        assertEquals(0, report.levelsGained());
        assertEquals(0, station.level);
    }

    @Test
    void previewCostChangesNothing() {
        wallet.addCoins(1000);

        upgrades.previewCost(station, BuyMode.MAX);

        assertEquals(1000, wallet.getCoins(), DELTA);
        assertEquals(0, station.level);
    }

    @Test
    void stationWithoutCostIsRejectedInsteadOfLoopingForever() {
        FakeStation broken = new FakeStation(0);

        assertThrows(IllegalArgumentException.class, () -> upgrades.previewCost(broken, BuyMode.MAX));
    }

    @Test
    void growthMustBeGreaterThanOne() {
        assertThrows(IllegalArgumentException.class, () -> new UpgradeService(wallet, 1));
        assertThrows(IllegalArgumentException.class, () -> new UpgradeService(wallet, 0.5));
    }

    @Test
    void walletIsRequired() {
        assertThrows(NullPointerException.class, () -> new UpgradeService(null, GROWTH));
    }

    // Adds the levels one by one, to check the formula used by UpgradeService.
    private static double costOfLevels(int fromLevel, int count) {
        double total = 0;
        for (int level = fromLevel; level < fromLevel + count; level++) {
            total += BASE_COST * Math.pow(GROWTH, level);
        }
        return total;
    }

    // Smallest possible Upgradable: the next level costs baseCost * GROWTH^level.
    private static final class FakeStation implements Upgradable {
        private final double baseCost;
        private int level;

        FakeStation(double baseCost) {
            this.baseCost = baseCost;
        }

        @Override
        public double getUpgradeCost() {
            return baseCost * Math.pow(GROWTH, level);
        }

        @Override
        public void upgrade(int levels) {
            level += levels;
        }
    }
}
