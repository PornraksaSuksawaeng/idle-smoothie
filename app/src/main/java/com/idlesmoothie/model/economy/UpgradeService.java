package com.idlesmoothie.model.economy;

import java.util.Objects;

public final class UpgradeService {
    private final Wallet wallet;
    private final double growth;

    /**
     * Create the service.
     *
     * @param wallet the wallet that pays for the levels
     * @param growth how much the cost grows at each levels
     *
     * @throws NullPointerException if wallet is null
     * @throws IllegalArgumentException if growth is not greater than 1
     *
     */
    public UpgradeService(Wallet wallet, double growth) {
        Objects.requireNonNull(wallet, "wallet");
        if (growth <= 1) {
            throw new IllegalArgumentException("growth must be greater than 1: " + growth);
        }
        this.wallet = wallet;
        this.growth = growth;
    }

    /**
     * Pay for the levels of the mode and adds them to the station.
     * If the wallet cannot pay, nothing changes.
     *
     * @param station the station to level up
     * @param mode how many levels to buy
     * @return the levels gained and the coins spent, both 0 if the wallet was short
     * @throws IllegalArgumentException if the upgrade cost of station is not positive
     */
    public UpgradeReport buy(Upgradable station, BuyMode mode) {
        int levels = levelsFor(station, mode);
        double cost = costOf(station, levels);
        if (!wallet.spendCoins(cost)) {
            return new UpgradeReport(0, 0);
        }
        station.upgrade(levels);
        return new UpgradeReport(levels, cost);
    }

    /**
     * Returns what {@link #buy} would cost now, without buying anything.
     * With {@link BuyMode#MAX} and the wallet too short for one level, it return the cost of one level, so the button can still show a price.
     *
     * @param station the station to level up
     * @param mode how many levels to buy 
     * @return the cost in coins, maybe more than the wallet holds
     * @throws IllegalArgumentException if the upgrade cost of the {@code station} is not positive
     */
    public double previewCost(Upgradable station, BuyMode mode) {
        return costOf(station, levelsFor(station, mode));
    }

    private int levelsFor(Upgradable station, BuyMode mode) {
        return switch (mode) {
            case X1 -> 1;
            case X10 -> 10;
            case MAX ->countMaxLevels(station);
        };
    }

    // Start at 1, then adds a level as long as the wallet can pay one more.
    private int countMaxLevels(Upgradable station) {
        int levels = 1;
        while (wallet.canAfford(costOf(station, levels + 1))) {
            levels++;
        }
        return levels;
    }

    // Each level costs the previous one times growth, so the total is a geometric series.
    private double costOf(Upgradable station, int levels) {
        double next = station.getUpgradeCost();
        if (next <= 0) {
            throw new IllegalArgumentException("upgrade cost must be positive: " + next);
        }
        return next * (Math.pow(growth, levels) - 1) / (growth - 1);
    }
}
