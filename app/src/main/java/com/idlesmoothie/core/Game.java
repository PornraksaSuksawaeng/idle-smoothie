package com.idlesmoothie.core;

import com.idlesmoothie.model.economy.BasePriceCalculator;
import com.idlesmoothie.model.economy.BuyMode;
import com.idlesmoothie.model.economy.PriceCalculator;
import com.idlesmoothie.model.economy.UpgradeReport;
import com.idlesmoothie.model.economy.UpgradeService;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Mine;
import com.idlesmoothie.model.station.Station;
import com.idlesmoothie.model.station.StationType;

/**
 * Single entry point of the model for the view (Facade pattern).
 * Builds the whole game, advances it each frame and turns the player's actions into model calls.
 * The view reads the state through {@link #getMine()} and {@link #getCoins()}.
 */
public class Game {

    private final Wallet wallet;
    private final Mine mine;
    private final UpgradeService upgradeService;

    /**
     * Starts a new game with the default balance values.
     */
    public Game() {
        this(new BalanceConfig());
    }

    /**
     * Starts a new game with the given balance values and the default prices.
     *
     * @param config game balance values
     */
    public Game(BalanceConfig config) {
        this(config, new BasePriceCalculator(config.getSmoothieBasePrice()), new Wallet());
    }

    /**
     * Starts a new game with the given parts, so tests can choose the prices and the balance.
     *
     * @param config          game balance values
     * @param priceCalculator gives the price of the smoothie sold
     * @param wallet          the player's coins
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public Game(BalanceConfig config, PriceCalculator priceCalculator, Wallet wallet) {
        if (config == null || priceCalculator == null || wallet == null) {
            throw new IllegalArgumentException("Config, price calculator and wallet must not be null");
        }
        this.wallet = wallet;
        this.mine = new Mine(priceCalculator, wallet, config);
        this.upgradeService = new UpgradeService(wallet, config.getUpgradeCostGrowth());
    }

    /**
     * Advances the game. Called by the game loop on every frame.
     *
     * @param dt elapsed time since the last frame, in seconds
     * @throws IllegalArgumentException if {@code dt} is negative
     */
    public void update(double dt) {
        mine.update(dt);
    }

    /**
     * Starts one round of the floor the player tapped.
     *
     * @param index index of the floor, 0 for the top one
     * @throws IllegalArgumentException if no unlocked floor has this index
     */
    public void tapFloor(int index) {
        if (index < 0 || index >= mine.getFloors().size()) {
            throw new IllegalArgumentException("No unlocked floor at index " + index);
        }
        mine.getFloors().get(index).start();
    }

    /**
     * Starts one trip of the elevator.
     */
    public void tapElevator() {
        mine.getElevator().start();
    }

    /**
     * Starts one sale of the sell station.
     */
    public void tapSellStation() {
        mine.getSellStation().start();
    }

    /**
     * @return {@code true} if the next floor can be unlocked and the player can pay for it
     */
    public boolean canBuyNextFloor() {
        return mine.canUnlockNextFloor() && wallet.canAfford(mine.getNextFloorCost());
    }

    /**
     * Pays for the next floor and unlocks it, or does nothing if {@link #canBuyNextFloor()} is false.
     *
     * @return the new floor, or {@code null} if it could not be bought
     */
    public Floor buyNextFloor() {
        if (!canBuyNextFloor() || !wallet.spendCoins(mine.getNextFloorCost())) {
            return null;
        }
        return mine.unlockNextFloor();
    }

    /**
     * Returns the price shown on an upgrade button, without buying anything.
     * In {@link BuyMode#MAX} with too few coins for one level, it is the price of one level.
     *
     * @param station the station to upgrade
     * @param mode    how many levels to buy: one, ten, or as many as the player can pay
     * @return the price in coins, possibly more than the player has
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public double getUpgradeCost(Station station, BuyMode mode) {
        requireUpgradable(station, mode);
        return upgradeService.previewCost(station, mode);
    }

    /**
     * @param station the station to upgrade
     * @param mode    how many levels to buy
     * @return {@code true} if the player can pay, used to grey out the button
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public boolean canUpgrade(Station station, BuyMode mode) {
        return wallet.canAfford(getUpgradeCost(station, mode));
    }

    /**
     * Pays for the levels and upgrades the station, or does nothing if the player cannot pay.
     *
     * @param station the station to upgrade
     * @param mode    how many levels to buy
     * @return the levels gained and the coins spent, both 0 if the player could not pay
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public UpgradeReport upgrade(Station station, BuyMode mode) {
        requireUpgradable(station, mode);
        return upgradeService.buy(station, mode);
    }

    /**
     * @return the type of station that limits the income, to highlight it
     */
    public StationType getBottleneck() {
        return mine.findBottleneck();
    }

    /**
     * @return the player's coins
     */
    public double getCoins() {
        return wallet.getCoins();
    }

    /**
     * @return the mine, for the view to read the stations
     */
    public Mine getMine() {
        return mine;
    }

    private static void requireUpgradable(Station station, BuyMode mode) {
        if (station == null || mode == null) {
            throw new IllegalArgumentException("Station and mode must not be null");
        }
    }
}