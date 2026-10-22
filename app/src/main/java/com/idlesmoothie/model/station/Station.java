package com.idlesmoothie.model.station;

import com.idlesmoothie.model.economy.Upgradable;
import com.idlesmoothie.model.manager.Manager;
import java.util.List;
import java.util.OptionalInt;

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
     * Returns the levels where the station reaches a milestone and its capacity is multiplied.
     *
     * @return the milestone levels, in increasing order
     */
    public abstract List<Integer> getMilestoneLevels();

    /**
     * Returns how much each milestone multiplies the capacity of this station.
     *
     * @return the multiplier of one milestone
     */
    public abstract double getMilestoneMultiplier();

    /**
     * Returns the raw stats the station has at the given level, before any bonus.
     * It must not change the station, so the view can preview an upgrade.
     *
     * @param level the level to compute the stats for
     * @return the raw stats at that level
     */
    protected abstract StationStats statsAt(int level);

    /**
     * Returns the raw stats for the current level, before any bonus.
     * Use {@link #getStats()} everywhere else.
     *
     * @return the raw stats
     */
    protected final StationStats baseStats() {
        return statsAt(level);
    }

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
        return withManager(baseStats());
    }

    /**
     * Returns the stats the station would have after buying more levels, with the manager's bonus applied.
     * The station does not change.
     *
     * @param extraLevels the levels that would be bought, 0 for the current stats
     * @return the final stats at that level
     * @throws IllegalArgumentException if {@code extraLevels} is negative or goes past the maximum level
     */
    public StationStats previewStats(int extraLevels) {
        return withManager(statsAt(levelAfter(extraLevels)));
    }

    /**
     * Returns the quantity of smoothie handled per second.
     *
     * @return the throughput
     */
    public double getThroughput() {
        return throughputOf(getStats(), level);
    }

    /**
     * Returns the throughput the station would have after buying more levels. The station does not change.
     *
     * @param extraLevels the levels that would be bought, 0 for the current throughput
     * @return the throughput at that level
     * @throws IllegalArgumentException if {@code extraLevels} is negative or goes past the maximum level
     */
    public double previewThroughput(int extraLevels) {
        int target = levelAfter(extraLevels);
        return throughputOf(withManager(statsAt(target)), target);
    }

    /**
     * Returns the first milestone level above the current level.
     *
     * @return the next milestone, or an empty value once every milestone is reached
     */
    public OptionalInt getNextMilestone() {
        for (int milestone : getMilestoneLevels()) {
            if (milestone > level) {
                return OptionalInt.of(milestone);
            }
        }
        return OptionalInt.empty();
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
     * Returns the quantity of smoothie handled per second with the given stats, speed times capacity.
     * A subclass changes it when more things than the stats matter, such as its number of Pulpies.
     *
     * @param stats the final stats
     * @param level the level the stats belong to
     * @return the throughput
     */
    protected double throughputOf(StationStats stats, int level) {
        return stats.getSpeed() * stats.getCapacity();
    }

    /**
     * Returns the level the station would reach after buying more levels.
     *
     * @param extraLevels the levels that would be bought
     * @return the level after the purchase
     * @throws IllegalArgumentException if {@code extraLevels} is negative or goes past the maximum level
     */
    protected final int levelAfter(int extraLevels) {
        if (extraLevels < 0 || extraLevels > getLevelsLeft()) {
            throw new IllegalArgumentException("Cannot preview " + extraLevels + " more levels");
        }
        return level + extraLevels;
    }

    /**
     * Returns how many milestone levels the station has reached at the given level.
     *
     * @param level the level to check
     * @return the number of milestones at or below that level
     */
    protected final int milestonesReached(int level) {
        int count = 0;
        for (int milestone : getMilestoneLevels()) {
            if (level >= milestone) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns how many times its level 1 capacity the station has at the given level: it grows at each level,
     * and each milestone reached multiplies it by {@link #getMilestoneMultiplier()}.
     *
     * @param growth the factor gained at each level
     * @param level  the level to compute the bonus for
     * @return the total capacity multiplier at that level
     */
    protected final double levelBonus(double growth, int level) {
        return Math.pow(growth, level - 1) * Math.pow(getMilestoneMultiplier(), milestonesReached(level));
    }

    // Applies the manager's bonus to raw stats, if the station has a manager.
    private StationStats withManager(StationStats stats) {
        return manager == null ? stats : manager.apply(stats);
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