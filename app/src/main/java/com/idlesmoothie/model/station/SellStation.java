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
    private int salesMade;
    private double lastSaleCoins;

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

    @Override
    public List<Integer> getMilestoneLevels() {
        return config.getMilestoneLevels();
    }

    @Override
    public double getMilestoneMultiplier() {
        return config.getMilestoneMultiplier();
    }

    /**
     * @return the number of Pulpies selling at the bar
     */
    public int getPulpieCount() {
        return pulpieCountAt(getLevel());
    }

    /**
     * Returns how many Pulpies the bar would have after buying more levels. The bar does not change.
     *
     * @param extraLevels the levels that would be bought, 0 for the current count
     * @return the number of Pulpies at that level
     * @throws IllegalArgumentException if {@code extraLevels} is negative or goes past the maximum level
     */
    public int previewPulpieCount(int extraLevels) {
        return pulpieCountAt(levelAfter(extraLevels));
    }

    /**
     * The smoothie sold per second by all the Pulpies of the bar.
     */
    @Override
    protected double throughputOf(StationStats stats, int level) {
        return pulpieCountAt(level) * super.throughputOf(stats, level);
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
     * Returns how many sales were paid since the station was built. The view compares it between frames
     * to notice each new sale.
     *
     * @return the number of sales made
     */
    public int getSalesMade() {
        return salesMade;
    }

    /**
     * @return the coins paid by the last sale, 0 before the first one
     */
    public double getLastSaleCoins() {
        return lastSaleCoins;
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
    protected StationStats statsAt(int level) {
        return new StationStats(
                config.getSellSpeed(),
                config.getSellBaseCapacity() * levelBonus(config.getStatGrowth(), config.getLateStatGrowth(),
                        config.getLateGrowthLevel(), level)
        );
    }

    // One Pulpie, plus one per milestone reached, up to the maximum.
    private int pulpieCountAt(int level) {
        return Math.min(1 + milestonesReached(level), config.getMaxPulpies());
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
        double earned = 0;
        for (SmoothieBatch batch : onSale) {
            earned += priceCalculator.priceOf(batch);
        }
        wallet.addCoins(earned);
        lastSaleCoins = earned;
        salesMade++;
        onSale.clear();
        timer.reset();
    }
}