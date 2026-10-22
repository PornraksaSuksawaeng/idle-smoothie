package com.idlesmoothie.model.station;

import java.util.List;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.resource.SmoothieBatch;
import com.idlesmoothie.model.resource.Storage;

/**
 * Elevator that goes down floor by floor, loads each crate until its cabin is full or it reaches
 * the bottom, then goes back up and unloads at the surface.
 * Its speed is the number of floors it travels per second; its capacity is the size of its cabin.
 */
public class Elevator extends Station {

    private final BalanceConfig config;
    private final List<Floor> floors;
    private final Storage surface;
    private final Storage cabin;
    private double position;
    private int lastStop;
    private boolean goingDown = true;

    /**
     * Creates an idle elevator at level 1, at the surface with an empty cabin.
     *
     * @param floors  floors of the mine, shared with the mine so new floors are served automatically
     * @param surface surface stock where the elevator unloads
     * @param config  game balance values
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public Elevator(List<Floor> floors, Storage surface, BalanceConfig config) {
        if (floors == null || surface == null || config == null) {
            throw new IllegalArgumentException("Floors, surface and config must not be null");
        }
        this.floors = floors;
        this.surface = surface;
        this.config = config;
        this.cabin = new Storage(config.getElevatorBaseCapacity());
    }

    @Override
    public StationType getType() {
        return StationType.ELEVATOR;
    }

    @Override
    public double getBaseUpgradeCost() {
        return config.getElevatorBaseCost();
    }

    @Override
    public int getMaxLevel() {
        return config.getMaxLevel();
    }

    @Override
    public double getUpgradeCost() {
        return getBaseUpgradeCost() * Math.pow(config.getUpgradeCostGrowth(), getLevel() - 1);
    }

    @Override
    public List<Integer> getMilestoneLevels() {
        return config.getMilestoneLevels();
    }

    /**
     * The elevator gains more than the other stations, since it never gains a Pulpie.
     */
    @Override
    public double getMilestoneMultiplier() {
        return config.getElevatorMilestoneMultiplier();
    }

    /**
     * The smoothie brought to the surface per second.
     * A round trip covers the depth of the mine twice, so deeper mines move less smoothie per second.
     */
    @Override
    protected double throughputOf(StationStats stats, int level) {
        int depth = Math.max(1, floors.size());
        return stats.getCapacity() * stats.getSpeed() / (2.0 * depth);
    }

    /**
     * @return the position of the cabin, in floors: 0 at the surface, 1 at the first floor
     */
    public double getPosition() {
        return position;
    }

    /**
     * @return {@code true} while the elevator is on its way down
     */
    public boolean isGoingDown() {
        return goingDown;
    }

    /**
     * @return the quantity of smoothie in the cabin
     */
    public double getCabinAmount() {
        return cabin.getAmount();
    }

    /**
     * Speed is the number of floors travelled per second; capacity is the size of the cabin.
     */
    @Override
    protected StationStats statsAt(int level) {
        return new StationStats(
                config.getElevatorMoveSpeed() * (1 + config.getElevatorSpeedGrowth() * (level - 1)),
                config.getElevatorBaseCapacity() * levelBonus(config.getStatGrowth(), level)
        );
    }

    /**
     * Moves the cabin. Going down, it stops at each floor to load; full or at the bottom, it turns back.
     * Back at the surface, it unloads, then stops after a tap or leaves again with a manager.
     */
    @Override
    protected void work(double dt) {
        if (floors.isEmpty()) {
            if (!isAutomated()) {
                endRound();
            }
            return;
        }
        StationStats stats = getStats();
        cabin.setCapacity(stats.getCapacity());
        double step = dt * stats.getSpeed();

        if (goingDown) {
            position += step;
            if (position >= lastStop + 1) {
                lastStop++;
                position = lastStop;
                loadFrom(floors.get(lastStop - 1));
                if (cabin.isFull() || lastStop >= floors.size()) {
                    goingDown = false;
                }
            }
        } else {
            position -= step;
            if (position <= 0) {
                position = 0;
                lastStop = 0;
                unloadToSurface();
                goingDown = true;
                if (!isAutomated()) {
                    endRound();
                }
            }
        }
    }

    private void loadFrom(Floor floor) {
        for (SmoothieBatch batch : floor.collect(cabin.getFreeSpace())) {
            cabin.store(batch);
        }
    }

    private void unloadToSurface() {
        for (SmoothieBatch batch : cabin.take(cabin.getAmount())) {
            surface.store(batch);
        }
    }
}