package com.idlesmoothie.model.station;

import com.idlesmoothie.model.economy.Upgradable;

/**
 * Base class of every station of the mine: floors, elevator and sell station.
 * Implements the Template Method pattern: {@link #update(double)} decides whether the station
 * works, and each subclass decides how it works in {@link #work(double)}.
 * Without a manager, a tap starts one round of work; with a manager, the station works on its own.
 */
public abstract class Station implements Upgradable {

    private int level;
    private boolean running;

    /**
     * Creates an idle station at level 1.
     */
    protected Station() {
        this.level = 1;
    }

    /**
     * @return the kind of station, used by the view and to match managers
     */
    public abstract StationType getType();

    /**
     * @return the price in coins of the next level
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
     * @return {@code true} if the station works on its own, without taps
     */
    public boolean isAutomated() {
        // TODO: return manager != null once the manager package exists
        return false;
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
     * Returns the stats actually used by the station, with every bonus applied.
     *
     * @return the final stats
     */
    public StationStats getStats() {
        // TODO: apply the manager bonus once the manager package exists
        return baseStats();
    }

    /**
     * @return the quantity of smoothie handled per second, speed times capacity
     */
    public double getThroughput() {
        StationStats stats = getStats();
        return stats.getSpeed() * stats.getCapacity();
    }

    /**
     * @return the current level, starting at 1
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * @return {@code true} if a round started by a tap is in progress
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
}