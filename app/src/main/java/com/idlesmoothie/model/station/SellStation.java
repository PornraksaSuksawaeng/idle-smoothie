package com.idlesmoothie.model.station;

import java.util.ArrayList;
import java.util.List;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.core.CycleTimer;
import com.idlesmoothie.model.economy.PriceCalculator;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.resource.SmoothieBatch;
import com.idlesmoothie.model.resource.Storage;

/**
 * Station at the surface that sells smoothie for coins.
 * A sale takes up to its capacity from the surface stock for each of its Pulpies, lasts one cycle,
 * then pays the wallet. Each milestone level adds a Pulpie.
 * Its speed is the number of sales per second; its capacity is the quantity one Pulpie sells per sale.
 */
public class SellStation extends Station {

    private final BalanceConfig config;
    private final Storage surface;
    private final PriceCalculator priceCalculator;
    private final Wallet wallet;
    private final CycleTimer timer = new CycleTimer();
    private final List<SmoothieBatch> onSale = new ArrayList<>();

    /**
     * Creates an idle sell station at level 1, with nothing on sale.
     *
     * @param surface         surface stock the station sells from
     * @param priceCalculator gives the price of each batch sold
     * @param wallet          receives the coins of each sale
     * @param config          game balance values
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public SellStation(Storage surface, PriceCalculator priceCalculator, Wallet wallet, BalanceConfig config) {
        if (surface == null || priceCalculator == null || wallet == null || config == null) {
            throw new IllegalArgumentException("Surface, price calculator, wallet and config must not be null");
        }
        this.surface = surface;
        this.priceCalculator = priceCalculator;
        this.wallet = wallet;
        this.config = config;
    }

    @Override
    public StationType getType() {
        return StationType.SELL;
    }

    @Override
    public double getBaseUpgradeCost() {
        return config.getSellBaseCost();
    }

    @Override
    public int getMaxLevel() {
        return config.getMaxLevel();
    }

    /**
     * @return the number of Pulpies selling at the bar
     */
    public int getPulpieCount() {
        return Math.min(1 + milestonesReached(config.getMilestoneLevels()), config.getMaxPulpies());
    }

    /**
     * @return the smoothie sold per second by all the Pulpies of the bar
     */
    @Override
    public double getThroughput() {
        return getPulpieCount() * super.getThroughput();
    }

    @Override
    public double getUpgradeCost() {
        return getBaseUpgradeCost() * Math.pow(config.getUpgradeCostGrowth(), getLevel() - 1);
    }

    /**
     * @return the quantity of smoothie in the sale in progress
     */
    public double getOnSaleAmount() {
        double amount = 0;
        for (SmoothieBatch batch : onSale) {
            amount += batch.getQuantity();
        }
        return amount;
    }

    /**
     * @return the progress of the sale in progress, from 0 to 1
     */
    public double getProgress() {
        return timer.getProgress();
    }

    /**
     * Speed is the number of sales per second; capacity is the quantity sold per sale.
     */
    @Override
    protected StationStats baseStats() {
        return new StationStats(
                config.getSellSpeed(),
                config.getSellBaseCapacity() * levelBonus(config.getStatGrowth(),
                        config.getMilestoneLevels(), config.getMilestoneMultiplier())
        );
    }

    /**
     * Starts a sale with what the surface holds, then pays once the cycle is over.
     * With nothing to sell, a tap is lost and a manager waits for the next delivery.
     */
    @Override
    protected void work(double dt) {
        StationStats stats = getStats();
        if (onSale.isEmpty()) {
            onSale.addAll(surface.take(getPulpieCount() * stats.getCapacity()));
            timer.reset();
            if (onSale.isEmpty()) {
                if (!isAutomated()) {
                    endRound();
                }
                return;
            }
        }
        if (timer.advance(dt, stats.getSpeed()) > 0) {
            sell();
            if (!isAutomated()) {
                endRound();
            }
        }
    }

    private void sell() {
        for (SmoothieBatch batch : onSale) {
            wallet.addCoins(priceCalculator.priceOf(batch));
        }
        onSale.clear();
        timer.reset();
    }
}