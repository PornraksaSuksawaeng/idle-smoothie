package com.idlesmoothie.model.station;

import com.idlesmoothie.model.economy.Upgradable;
import com.idlesmoothie.model.manager.Manager;
import java.util.List;

/**
 * Base class of every station of the mine: floors, elevator and sell station.
 * Implements the Template Method pattern: {@link #update(double)} decides whether the station
 * works, and each subclass decides how it works in {@link #work(double)}.
 * Without a manager, a tap starts one round of work; with a manager, the station works on its own.
 */
public abstract class Station implements Upgradable {

    private int level;
    private boolean running;
    private Manager manager;
    private int managersHired;

    /**
     * Creates an idle station at level 1, without a manager.
     */
    protected Station() {
        this.level = 1;
    }

    /**
     * Returns the kind of station, used by the view and to match managers.
     *
     * @return the station type
     */
    public abstract StationType getType();

    /**
     * Returns the price of the next level only.
     *
     * @return the price in coins
     */
    @Override
    public abstract double getUpgradeCost();

    /**
     * Returns the price of this station's first upgrade, which never changes.
     * It sets the scale of the station's other prices, such as its managers.
     *
     * @return the base price in coins
     */
    public abstract double getBaseUpgradeCost();

    /**
     * Returns the highest level the station can reach.
     *
     * @return the maximum level
     */
    public abstract int getMaxLevel();

    /**
     * Returns how many levels can still be bought.
     *
     * @return the number of levels left, 0 at the maximum level
     */
    @Override
    public int getLevelsLeft() {
        return getMaxLevel() - level;
    }

    /**
     * Returns the raw stats for the current level, before any bonus.
     * Use {@link #getStats()} everywhere else.
     *
     * @return the raw stats
     */
    protected abstract StationStats baseStats();

    /**
     * Does the station's job for the elapsed time.
     * Called by {@link #update(double)} only, and only while the station is working.
     * A subclass calls {@link #endRound()} once the work started by a tap is done.
     *
     * @param dt elapsed time since the last update, in seconds
     */
    protected abstract void work(double dt);

    /**
     * Starts one round of work when the player taps the station.
     * Does nothing if a round is already running or if the station is automated.
     */
    public void start() {
        if (!running && !isAutomated()) {
            running = true;
        }
    }

    /**
     * Tells whether the station works on its own, which is the case when it has a manager.
     *
     * @return {@code true} if the station works without taps
     */
    public boolean isAutomated() {
        return manager != null;
    }

    /**
     * Puts a manager on this station, replacing the previous one if any.
     * The station then works on its own; a round started by a tap is ended.
     *
     * @param manager the manager to assign
     * @throws IllegalArgumentException if the manager is {@code null} or cannot work on this kind of station
     */
    public void assignManager(Manager manager) {
        if (manager == null || !manager.canWork(this)) {
            throw new IllegalArgumentException("This manager cannot work on this station");
        }
        this.manager = manager;
        managersHired++;
        endRound();
        onManagerChanged();
    }

    /**
     * Takes the manager off this station, which goes back to working on taps.
     * Does nothing if the station has no manager.
     */
    public void removeManager() {
        if (manager == null) {
            return;
        }
        manager = null;
        onManagerChanged();
    }

    /**
     * Returns the manager working on this station.
     *
     * @return the manager, or {@code null} if there is none
     */
    public Manager getManager() {
        return manager;
    }

    /**
     * Returns how many managers were assigned to this station, the current one included.
     * Each new one costs more, so the player cannot reroll the rarity for free.
     *
     * @return the number of managers hired for this station
     */
    public int getManagersHired() {
        return managersHired;
    }

    /**
     * Sets how many managers were bought for this station, when a saved game is loaded.
     * Call it after assigning the saved manager, which counts as one hire.
     *
     * @param count the saved number of hires
     * @throws IllegalArgumentException if {@code count} is negative, or is 0 while the station has a manager
     */
    public void restoreManagersHired(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Hires must not be negative: " + count);
        }
        managersHired = count;
    }

    /**
     * Lets the station work for the elapsed time if it was tapped or is automated.
     *
     * @param dt elapsed time since the last update, in seconds
     * @throws IllegalArgumentException if {@code dt} is negative
     */
    public final void update(double dt) {
        if (dt < 0) {
            throw new IllegalArgumentException("dt must be non-negative");
        }
        if (!running && !isAutomated()) {
            return;
        }
        work(dt);
    }

    /**
     * Raises the level of the station.
     *
     * @param levels number of levels to add
     * @throws IllegalArgumentException if {@code levels} is not positive or goes past the maximum level
     */
    @Override
    public void upgrade(int levels) {
        if (levels <= 0) {
            throw new IllegalArgumentException("Levels must be positive");
        }
        if (levels > getLevelsLeft()) {
            throw new IllegalArgumentException("Cannot go past level " + getMaxLevel());
        }
        level += levels;
    }

    /**
     * Returns the stats actually used by the station, with the manager's bonus applied.
     * They are computed again on every call, so an upgrade or a new manager takes effect at once.
     *
     * @return the final stats
     */
    public StationStats getStats() {
        StationStats stats = baseStats();
        if (manager != null) {
            stats = manager.apply(stats);
        }
        return stats;
    }

    /**
     * Returns the quantity of smoothie handled per second, speed times capacity.
     *
     * @return the throughput
     */
    public double getThroughput() {
        StationStats stats = getStats();
        return stats.getSpeed() * stats.getCapacity();
    }

    /**
     * Returns the current level.
     *
     * @return the level, starting at 1
     */
    public int getLevel() {
        return level;
    }

    /**
     * Tells whether a round started by a tap is in progress.
     *
     * @return {@code true} while the tapped round runs
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns how many milestone levels the station has reached.
     *
     * @param milestones the milestone levels
     * @return the number of milestones at or below the current level
     */
    protected final int milestonesReached(List<Integer> milestones) {
        int count = 0;
        for (int milestone : milestones) {
            if (level >= milestone) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns how many times its level 1 capacity the station has: it grows at each level,
     * and each milestone reached multiplies it again.
     *
     * @param growth     the factor gained at each level
     * @param milestones the milestone levels
     * @param multiplier the factor one milestone applies
     * @return the total capacity multiplier for the current level
     */
    protected final double levelBonus(double growth, List<Integer> milestones, double multiplier) {
        return Math.pow(growth, level - 1) * Math.pow(multiplier, milestonesReached(milestones));
    }

    /**
     * Ends the round started by a tap. Called by a subclass when its work is done.
     */
    protected final void endRound() {
        running = false;
    }

    /**
     * Called when a manager is assigned or removed, so a subclass can reset its own state.
     * Does nothing by default.
     */
    protected void onManagerChanged() {
    }
}