package com.idlesmoothie.model.station;

/**
 * Base class of every station of the mine: floors, elevator and sell station.
 * Implements the Template Method pattern: {@link #update(double)} decides when a cycle
 * ends, and each subclass decides what happens at the end of a cycle in {@link #performCycle()}.
 * Without a manager, a station runs one cycle per tap; with a manager, it runs on its own.
 */
public abstract class Station {

    private int level;
    private double progress;
    private boolean running;

    /**
     * Creates a station at level 1, idle and with an empty progress bar.
     */
    protected Station() {
        this.level = 1;
        this.progress = 0;
    }

    /**
     * @return the kind of station, used by the view and to match managers
     */
    public abstract StationType getType();

    /**
     * @return the price in coins of the next level
     */
    public abstract double getUpgradeCost();

    /**
     * Applies the result of a finished cycle, such as moving smoothie from one storage to another.
     * Called by {@link #update(double)} only, never directly.
     */
    protected abstract void performCycle();

    /**
     * Returns the raw stats for the current level, before any bonus.
     * Use {@link #getStats()} everywhere else.
     *
     * @return the raw stats
     */
    protected abstract StationStats baseStats();

    /**
     * Starts one cycle when the player taps the station.
     * Does nothing if a cycle is already running or if the station is automated.
     */
    public void start() {
        if (!running && !isAutomated()) {
            running = true;
        }
    }

    /**
     * @return {@code true} if the station runs cycles on its own, without taps
     */
    public boolean isAutomated() {
        // TODO: return manager != null once the manager package exists
        return false;
    }

    /**
     * Advances the station by the elapsed time and runs every cycle finished meanwhile.
     * A manual station stops after one cycle; an automated one keeps the leftover progress
     * so that no production is lost between frames.
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
        progress += dt * getStats().getSpeed();
        while (progress >= 1) {
            performCycle();
            progress -= 1;
            if (!isAutomated()) {
                running = false;
                progress = 0;
                break;
            }
        }
    }

    /**
     * Raises the level of the station.
     *
     * @param levels number of levels to add
     * @throws IllegalArgumentException if {@code levels} is not positive
     */
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
     * @return the progress of the current cycle, between 0 and 1
     */
    public double getProgress() {
        return progress;
    }

    /**
     * @return {@code true} if a cycle started by a tap is in progress
     */
    public boolean isRunning() {
        return running;
    }
}