package com.idlesmoothie.core;

/**
 * Every tuning value of the game in one place, so the balance can be changed without touching the logic.
 * Created once by the game and passed to every class that needs it.
 * Immutable: the values never change during a game.
 */
public final class BalanceConfig {

    private final double floorTripSpeed = 0.25;
    private final double floorBaseCarry = 10;
    private final double floorBaseCost = 50;
    private final double upgradeCostGrowth = 1.07;
    private final int levelsPerPulpie = 25;
    private final int maxPulpies = 5;
    private final double elevatorMoveSpeed = 1.0;
    private final double elevatorBaseCapacity = 50;
    private final double elevatorBaseCost = 75;
    private final double sellSpeed = 0.5;
    private final double sellBaseCapacity = 40;
    private final double sellBaseCost = 60;
    private final int floorsPerFruit = 3;
    private final double newFloorBaseCost = 500;
    private final double newFloorCostGrowth = 3.85;
    private final double barrierCostMultiplier = 1;
    private final int barrierMinLevel = 10;
    private final double smoothieBasePrice = 1;
    private final double managerCostMultiplier = 3;
    private final double managerCostGrowth = 1.5;
    private final double floorDepthProductionGrowth = 2.5;
    private final double floorDepthCostGrowth = 3;
    private final int milestoneInterval = 25;
    private final double milestoneMultiplier = 2;
    private final double elevatorSpeedGrowth = 0.02;
    private final double transportMilestoneMultiplier = 2.5;

    /**
     * @return the number of full trips a Pulpie makes per second
     */
    public double getFloorTripSpeed() {
        return floorTripSpeed;
    }

    /**
     * @return the quantity a Pulpie carries per trip on the first floor at level 1
     */
    public double getFloorBaseCarry() {
        return floorBaseCarry;
    }

    /**
     * @return the price of the first upgrade of the first floor
     */
    public double getFloorBaseCost() {
        return floorBaseCost;
    }

    /**
     * @return the factor applied to the upgrade price at each level, such as {@code 1.15} for +15%
     */
    public double getUpgradeCostGrowth() {
        return upgradeCostGrowth;
    }

    /**
     * @return the number of levels a floor needs to gain one more Pulpie
     */
    public int getLevelsPerPulpie() {
        return levelsPerPulpie;
    }

    /**
     * @return the maximum number of Pulpies on a floor
     */
    public int getMaxPulpies() {
        return maxPulpies;
    }
    /**
     * @return the number of floors the elevator travels per second
     */
    public double getElevatorMoveSpeed() {
        return elevatorMoveSpeed;
    }

    /**
     * @return the quantity the elevator cabin holds at level 1
     */
    public double getElevatorBaseCapacity() {
        return elevatorBaseCapacity;
    }

    /**
     * @return the price of the first elevator upgrade
     */
    public double getElevatorBaseCost() {
        return elevatorBaseCost;
    }

    /**
     * @return the number of sales the sell station makes per second
     */
    public double getSellSpeed() {
        return sellSpeed;
    }

    /**
     * @return the quantity the sell station sells per sale at level 1
     */
    public double getSellBaseCapacity() {
        return sellBaseCapacity;
    }

    /**
     * @return the price of the first sell station upgrade
     */
    public double getSellBaseCost() {
        return sellBaseCost;
    }

    /**
     * @return the number of floors of each fruit zone
     */
    public int getFloorsPerFruit() {
        return floorsPerFruit;
    }

    /**
     * @return the price of the second floor of the mine
     */
    public double getNewFloorBaseCost() {
        return newFloorBaseCost;
    }

    /**
     * @return the factor applied to the price of each deeper floor
     */
    public double getNewFloorCostGrowth() {
        return newFloorCostGrowth;
    }

    /**
     * @return how many times more a floor behind a barrier costs than a normal floor
     */
    public double getBarrierCostMultiplier() {
        return barrierCostMultiplier;
    }

    /**
     * @return the level the deepest floor needs before the barrier below it can be crossed
     */
    public int getBarrierMinLevel() {
        return barrierMinLevel;
    }

    /**
     * @return the coins earned for one unit of smoothie, before any bonus
     */
    public double getSmoothieBasePrice() {
        return smoothieBasePrice;
    }

    /**
     * Returns how many times the station's next upgrade a manager costs.
     *
     * @return the multiplier applied to the next upgrade price
     */
    public double getManagerCostMultiplier() {
        return managerCostMultiplier;
    }

    /**
     * Returns the factor applied to the manager price each time one is bought for the same station.
     *
     * @return the growth, such as {@code 1.5} for +50% per hire
     */
    public double getManagerCostGrowth() {
        return managerCostGrowth;
    }

    /**
     * Returns how many times more a floor produces than the floor above it.
     *
     * @return the production factor between two floors
     */
    public double getFloorDepthProductionGrowth() {
        return floorDepthProductionGrowth;
    }

    /**
     * Returns how many times more a floor's upgrades cost than those of the floor above it.
     *
     * @return the price factor between two floors
     */
    public double getFloorDepthCostGrowth() {
        return floorDepthCostGrowth;
    }

    /**
     * Returns every how many levels a station reaches a milestone that multiplies its capacity.
     *
     * @return the number of levels between two milestones
     */
    public int getMilestoneInterval() {
        return milestoneInterval;
    }

    /**
     * Returns how much each milestone multiplies the capacity of a station.
     *
     * @return the multiplier of one milestone, such as {@code 2} to double it
     */
    public double getMilestoneMultiplier() {
        return milestoneMultiplier;
    }

    /**
     * Returns how much faster the elevator gets with each level, as a share of its base speed.
     *
     * @return the speed gained per level, such as {@code 0.02} for +2% per level
     */
    public double getElevatorSpeedGrowth() {
        return elevatorSpeedGrowth;
    }

    /**
     * Returns how much each milestone multiplies the capacity of the elevator and the sell station.
     * It is higher than for floors, so the transport keeps up with a deeper and deeper mine.
     *
     * @return the multiplier of one transport milestone
     */
    public double getTransportMilestoneMultiplier() {
        return transportMilestoneMultiplier;
    }
}