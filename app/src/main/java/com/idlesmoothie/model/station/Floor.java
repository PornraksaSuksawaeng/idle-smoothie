package com.idlesmoothie.model.station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.creature.Pulpie;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import com.idlesmoothie.model.resource.Storage;

/**
 * One floor of the mine, where Pulpies carry smoothie of the floor fruit into a crate.
 * Deeper floors and higher levels produce more; each milestone level adds a Pulpie.
 * The crate has no limit: the elevator is the one that may not keep up.
 */
public class Floor extends Station {
    private final BalanceConfig config;
    private final int index;
    private final Fruit fruit;
    private final Storage crate;
    private final List<Pulpie> pulpies = new ArrayList<>();
    private boolean roundStarted;

    /**
     * Creates a floor at level 1 with its first Pulpie.
     *
     * @param index  global floor number, starting at 0 at the top of the mine
     * @param fruit  fruit extracted on this floor
     * @param config game balance values
     * @throws IllegalArgumentException if the index is negative or the fruit or config is {@code null}
     */
    public Floor(int index, Fruit fruit, BalanceConfig config) {
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }
        if (fruit == null || config == null) {
            throw new IllegalArgumentException("Fruit and config must not be null");
        }
        this.index = index;
        this.fruit = fruit;
        this.config = config;
        this.crate = new Storage(Double.POSITIVE_INFINITY);
        addMissingPulpies();
    }

    @Override
    public StationType getType() {
        return StationType.FLOOR;
    }

    /**
     * @return the global floor number, starting at 0
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the fruit extracted on this floor
     */
    public Fruit getFruit() {
        return fruit;
    }

    /**
     * @return a read-only view of the Pulpies working on this floor
     */
    public List<Pulpie> getPulpies() {
        return Collections.unmodifiableList(pulpies);
    }

    /**
     * @return the quantity of smoothie waiting in the crate
     */
    public double getStoredAmount() {
        return crate.getAmount();
    }

    /**
     * Takes smoothie out of the crate, oldest first. Used by the elevator.
     *
     * @param amount maximum quantity to take
     * @return the batches taken, possibly empty
     * @throws IllegalArgumentException if the amount is negative
     */
    public List<SmoothieBatch> collect(double amount) {
        return crate.take(amount);
    }

    /**
     * @return the number of Pulpies currently working on the floor
     */
    public int getPulpieCount() {
        return pulpies.size();
    }

    private int targetPulpieCount() {
        int count = 1 + milestonesReached(config.getMilestoneLevels());
        return Math.min(count, config.getMaxPulpies());
    }

    private void addMissingPulpies() {
        while (pulpies.size() < targetPulpieCount()) {
            pulpies.add(new Pulpie(fruit, crate));
        }
    }

    /**
     * Speed is the number of trips per second of one Pulpie; capacity is what one Pulpie carries.
     */
    @Override
    protected StationStats baseStats() {
        return new StationStats(
                config.getFloorTripSpeed(),
                config.getFloorBaseCarry(index) * levelBonus(config.getStatGrowth(),
                        config.getMilestoneLevels(), config.getMilestoneMultiplier())
        );
    }

    /**
     * The first upgrade costs a bit more than the floor itself.
     */
    @Override
    public double getBaseUpgradeCost() {
        return config.getFloorPrice(index) * config.getFloorUpgradeCostRatio();
    }

    @Override
    public int getMaxLevel() {
        return config.getMaxLevel();
    }

    @Override
    public double getUpgradeCost() {
        return getBaseUpgradeCost() * Math.pow(config.getUpgradeCostGrowth(), getLevel() - 1);
    }

    /**
     * @return the smoothie produced per second by all the Pulpies of the floor
     */
    @Override
    public double getThroughput() {
        StationStats stats = getStats();
        return pulpies.size() * stats.getSpeed() * stats.getCapacity();
    }

    /**
     * Raises the level and adds the Pulpies the new level unlocks.
     */
    @Override
    public void upgrade(int levels) {
        super.upgrade(levels);
        addMissingPulpies();
    }

    /**
     * Forgets the round in progress, so the next tap or the new manager sends every idle Pulpie again.
     */
    @Override
    protected void onManagerChanged() {
        roundStarted = false;
    }

    /**
     * Sends the Pulpies on their trips. A tap sends every Pulpie once and the round ends when all
     * are back; with a manager, each Pulpie leaves again as soon as it is back.
     */
    @Override
    protected void work(double dt) {
        StationStats stats = getStats();
        if (!roundStarted) {
            for (Pulpie pulpie : pulpies) {
                pulpie.dispatch();
            }
            roundStarted = true;
        }
        for (Pulpie pulpie : pulpies) {
            if (isAutomated()) {
                pulpie.dispatch();
            }
            pulpie.update(dt, stats.getSpeed(), stats.getCapacity());
        }
        if (!isAutomated() && allIdle()) {
            roundStarted = false;
            endRound();
        }
    }

    private boolean allIdle() {
        for (Pulpie pulpie : pulpies) {
            if (!pulpie.isIdle()) {
                return false;
            }
        }
        return true;
    }
}