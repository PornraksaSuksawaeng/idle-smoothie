package com.idlesmoothie.model.station;

import com.idlesmoothie.model.economy.Upgradable;
import com.idlesmoothie.model.manager.Manager;

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
     * @throws IllegalArgumentException if {@code levels} is not positive
     */
    @Override
    public void upgrade(int levels) {
        if (levels <= 0) {
            throw new IllegalArgumentException("Levels must be positive");
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