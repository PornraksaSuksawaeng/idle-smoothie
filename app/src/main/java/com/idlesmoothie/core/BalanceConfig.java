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
    private final double upgradeCostGrowth = 1.15;
    private final int levelsPerPulpie = 10;
    private final int maxPulpies = 5;
    private final double elevatorMoveSpeed = 1.0;
    private final double elevatorBaseCapacity = 50;
    private final double elevatorBaseCost = 75;
    private final double sellSpeed = 0.5;
    private final double sellBaseCapacity = 40;
    private final double sellBaseCost = 60;
    private final int floorsPerFruit = 3;
    private final double newFloorBaseCost = 500;
    private final double newFloorCostGrowth = 2.5;
    private final double barrierCostMultiplier = 5;
    private final int barrierMinLevel = 10;

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
}