package com.idlesmoothie.model.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.economy.PriceCalculator;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import com.idlesmoothie.model.resource.Storage;

class SellStationTest {

    private static final double DELTA = 1e-9;
    private static final double STEP = 0.01;
    private static final double UNIT_PRICE = 2;

    private final BalanceConfig config = new BalanceConfig();
    private final Storage surface = new Storage(Double.POSITIVE_INFINITY);
    private final PriceCalculator prices = batch -> batch.getQuantity() * UNIT_PRICE;
    private final Wallet wallet = new Wallet();

    /** Sell station that behaves as if a manager was assigned. */
    private static class AutomatedSellStation extends SellStation {

        AutomatedSellStation(Storage surface, PriceCalculator prices, Wallet wallet, BalanceConfig config) {
            super(surface, prices, wallet, config);
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

    /** Duration of one sale, plus a margin for frame rounding. */
    private double oneSale() {
        return 1 / config.getSellSpeed() + 0.05;
    }

    private double capacity() {
        return config.getSellBaseCapacity();
    }

    private void fillSurface(double amount) {
        surface.store(new SmoothieBatch(Fruit.STRAWBERRY, amount));
    }

    @Test
    void newStationHasNothingOnSale() {
        SellStation station = new SellStation(surface, prices, wallet, config);

        assertEquals(StationType.SELL, station.getType());
        assertEquals(0, station.getOnSaleAmount(), DELTA);
        assertEquals(0, station.getProgress(), DELTA);
        assertFalse(station.isRunning());
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SellStation(null, prices, wallet, config));
        assertThrows(IllegalArgumentException.class, () -> new SellStation(surface, null, wallet, config));
        assertThrows(IllegalArgumentException.class, () -> new SellStation(surface, prices, null, config));
        assertThrows(IllegalArgumentException.class, () -> new SellStation(surface, prices, wallet, null));
    }

    @Test
    void statsComeFromConfigAndLevel() {
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.upgrade(2);

        assertEquals(config.getSellSpeed(), station.getStats().getSpeed(), DELTA);
        assertEquals(Math.pow(config.getStatGrowth(), 2) * capacity(), station.getStats().getCapacity(), DELTA);
    }

    @Test
    void upgradeCostGrowsWithLevel() {
        SellStation station = new SellStation(surface, prices, wallet, config);
        assertEquals(config.getSellBaseCost(), station.getUpgradeCost(), DELTA);

        station.upgrade(1);

        assertEquals(config.getSellBaseCost() * config.getUpgradeCostGrowth(), station.getUpgradeCost(), DELTA);
    }

    @Test
    void nothingIsSoldWithoutTap() {
        fillSurface(capacity());
        SellStation station = new SellStation(surface, prices, wallet, config);

        run(station, oneSale());

        assertEquals(capacity(), surface.getAmount(), DELTA);
        assertEquals(0, wallet.getCoins(), DELTA);
    }

    @Test
    void tapTakesSmoothieFromSurfaceRightAway() {
        fillSurface(capacity());
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.start();
        station.update(STEP);

        assertEquals(0, surface.getAmount(), DELTA);
        assertEquals(capacity(), station.getOnSaleAmount(), DELTA);
        assertEquals(0, wallet.getCoins(), DELTA);
    }

    @Test
    void walletIsPaidAtTheEndOfTheSale() {
        fillSurface(capacity());
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.start();
        run(station, oneSale());

        assertEquals(capacity() * UNIT_PRICE, wallet.getCoins(), DELTA);
        assertEquals(0, station.getOnSaleAmount(), DELTA);
    }

    @Test
    void progressGrowsDuringTheSale() {
        fillSurface(capacity());
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.start();
        run(station, 0.5 / config.getSellSpeed());

        assertEquals(0.5, station.getProgress(), 0.05);
    }

    @Test
    void saleTakesAtMostTheCapacity() {
        fillSurface(3 * capacity());
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.start();
        run(station, oneSale());

        assertEquals(2 * capacity(), surface.getAmount(), DELTA);
        assertEquals(capacity() * UNIT_PRICE, wallet.getCoins(), DELTA);
    }

    @Test
    void saleTakesWhatIsLeftWhenSurfaceHoldsLess() {
        fillSurface(capacity() / 4);
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.start();
        run(station, oneSale());

        assertEquals(0, surface.getAmount(), DELTA);
        assertEquals(capacity() / 4 * UNIT_PRICE, wallet.getCoins(), DELTA);
    }

    @Test
    void tapSellsOnlyOnce() {
        fillSurface(3 * capacity());
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.start();
        run(station, 3 * oneSale());

        assertFalse(station.isRunning());
        assertEquals(2 * capacity(), surface.getAmount(), DELTA);
    }

    @Test
    void tapOnEmptySurfaceEndsTheRound() {
        SellStation station = new SellStation(surface, prices, wallet, config);

        station.start();
        station.update(STEP);

        assertFalse(station.isRunning());
        assertEquals(0, wallet.getCoins(), DELTA);
    }

    @Test
    void emptyTapDoesNotSellLaterDeliveries() {
        SellStation station = new SellStation(surface, prices, wallet, config);
        station.start();
        station.update(STEP);

        fillSurface(capacity());
        run(station, oneSale());

        assertEquals(capacity(), surface.getAmount(), DELTA);
    }

    @Test
    void eachBatchIsPricedSeparately() {
        surface.store(new SmoothieBatch(Fruit.STRAWBERRY, capacity() / 2));
        surface.store(new SmoothieBatch(Fruit.KIWI, capacity() / 2));
        PriceCalculator kiwiDouble = batch -> batch.getQuantity() * (batch.getFruit() == Fruit.KIWI ? 2 : 1);
        SellStation station = new SellStation(surface, kiwiDouble, wallet, config);

        station.start();
        run(station, oneSale());

        assertEquals(capacity() / 2 * 3, wallet.getCoins(), DELTA);
    }

    @Test
    void automatedStationKeepsSelling() {
        fillSurface(3 * capacity());
        SellStation station = new AutomatedSellStation(surface, prices, wallet, config);

        run(station, 3 * oneSale());

        assertEquals(0, surface.getAmount(), DELTA);
        assertEquals(3 * capacity() * UNIT_PRICE, wallet.getCoins(), DELTA);
    }

    @Test
    void automatedStationWaitsForDeliveries() {
        SellStation station = new AutomatedSellStation(surface, prices, wallet, config);
        run(station, oneSale());

        fillSurface(capacity());
        run(station, oneSale());

        assertEquals(capacity() * UNIT_PRICE, wallet.getCoins(), DELTA);
    }

    @Test
    void upgradeSellsMorePerSale() {
        fillSurface(3 * capacity());
        SellStation station = new SellStation(surface, prices, wallet, config);
        station.upgrade(1);

        station.start();
        run(station, oneSale());

        assertEquals(3 * capacity() - station.getStats().getCapacity(), surface.getAmount(), DELTA);
        assertTrue(wallet.getCoins() > capacity() * UNIT_PRICE);
    }

    @Test
    void baseUpgradeCostDoesNotChangeWithLevel() {
        SellStation station = new SellStation(surface, prices, wallet, config);
        double base = station.getBaseUpgradeCost();

        station.upgrade(20);

        assertEquals(config.getSellBaseCost(), base, DELTA);
        assertEquals(base, station.getBaseUpgradeCost(), DELTA);
    }

    @Test
    void milestoneMultipliesCapacity() {
        SellStation station = new SellStation(surface, prices, wallet, config);
        int milestone = config.getMilestoneLevels().get(0);
        station.upgrade(milestone - 1);

        double expected = capacity() * Math.pow(config.getStatGrowth(), milestone - 1) * config.getMilestoneMultiplier();

        assertEquals(expected, station.getStats().getCapacity(), DELTA * expected);
        assertEquals(2, station.getPulpieCount());
    }

    @Test
    void eachPulpieSellsItsCapacity() {
        SellStation station = new SellStation(surface, prices, wallet, config);
        station.upgrade(config.getMilestoneLevels().get(0) - 1);
        double perSale = station.getPulpieCount() * station.getStats().getCapacity();
        fillSurface(perSale + 1);

        station.start();
        station.update(0);

        assertEquals(perSale, station.getOnSaleAmount(), DELTA * perSale);
    }

    @Test
    void previewMatchesAnUpgradeWithoutChangingTheStation() {
        SellStation station = new SellStation(surface, prices, wallet, config);
        int extra = config.getMilestoneLevels().get(0);
        StationStats preview = station.previewStats(extra);
        double throughput = station.previewThroughput(extra);
        int pulpies = station.previewPulpieCount(extra);

        assertEquals(1, station.getLevel());
        assertEquals(1, station.getPulpieCount());

        station.upgrade(extra);
        assertEquals(station.getStats().getCapacity(), preview.getCapacity(), DELTA);
        assertEquals(station.getThroughput(), throughput, DELTA);
        assertEquals(2, pulpies);
        assertEquals(station.getPulpieCount(), pulpies);
    }
}
