package com.idlesmoothie.core;

import java.util.List;

/**
 * Every tuning value of the game in one place, so the balance can be changed without touching the logic.
 * Created once by the game and passed to every class that needs it.
 * Immutable: the values never change during a game.
 */
public final class BalanceConfig {

    private final double floorTripSpeed = 0.25;
    private final List<Double> floorBaseCarries = List.of(20.0, 1_000.0, 10_000.0);
    private final double floorCarryGrowth = 10;
    private final List<Double> floorPrices = List.of(10.0, 2_600.0, 78_000.0);
    private final double floorPriceGrowth = 20;
    private final double floorUpgradeCostRatio = 1.16;
    private final double statGrowth = 1.09;
    private final double lateStatGrowth = 1.24;
    private final int lateGrowthLevel = 250;
    private final double upgradeCostGrowth = 1.12;
    private final int maxLevel = 500;
    private final List<Integer> milestoneLevels = List.of(25, 50, 100, 250, 500);
    private final double milestoneMultiplier = 2.16;
    private final double elevatorMilestoneMultiplier = 3;
    private final int maxPulpies = 5;
    private final double elevatorMoveSpeed = 1.0;
    private final double elevatorBaseCapacity = 50;
    private final double elevatorBaseCost = 75;
    private final double sellSpeed = 0.5;
    private final double sellBaseCapacity = 40;
    private final double sellBaseCost = 60;
    private final int floorsPerFruit = 3;
    private final double barrierCostMultiplier = 1;
    private final int barrierMinLevel = 10;
    private final double smoothieBasePrice = 1;
    private final double managerCostMultiplier = 3;
    private final double managerCostGrowth = 1.5;
    private final double elevatorSpeedGrowth = 0.02;

    /**
     * @return the number of full trips a Pulpie makes per second
     */
    public double getFloorTripSpeed() {
        return floorTripSpeed;
    }

    /**
     * Returns what a Pulpie carries per trip on a floor at level 1.
     * The first floors are set by hand, then each floor carries {@code floorCarryGrowth} times more.
     *
     * @param index the floor index, starting at 0
     * @return the quantity carried per trip at level 1
     */
    public double getFloorBaseCarry(int index) {
        return fromTable(floorBaseCarries, floorCarryGrowth, index);
    }

    /**
     * Returns the price of a floor. The first floors are set by hand, then each floor costs
     * {@code floorPriceGrowth} times more. The first floor is free, its price only sets its upgrade prices.
     *
     * @param index the floor index, starting at 0
     * @return the price in coins
     */
    public double getFloorPrice(int index) {
        return fromTable(floorPrices, floorPriceGrowth, index);
    }

    /**
     * Returns how many times the floor price its first upgrade costs.
     *
     * @return the ratio between the first upgrade and the floor price
     */
    public double getFloorUpgradeCostRatio() {
        return floorUpgradeCostRatio;
    }

    /**
     * Returns the factor applied to the capacity of every station at each level.
     *
     * @return the growth, such as {@code 1.09} for +9% per level
     */
    public double getStatGrowth() {
        return statGrowth;
    }

    /**
     * Returns the factor applied to the capacity of the elevator and of the sell station at each level
     * past {@link #getLateGrowthLevel()}, so they keep up with all the floors of the end of the game.
     *
     * @return the growth, such as {@code 1.24} for +24% per level
     */
    public double getLateStatGrowth() {
        return lateStatGrowth;
    }

    /**
     * Returns the level from which the elevator and the sell station grow by {@link #getLateStatGrowth()}
     * instead of {@link #getStatGrowth()}.
     *
     * @return the last level that still grows by the normal growth
     */
    public int getLateGrowthLevel() {
        return lateGrowthLevel;
    }

    /**
     * @return the factor applied to the upgrade price at each level, such as {@code 1.15} for +15%
     */
    public double getUpgradeCostGrowth() {
        return upgradeCostGrowth;
    }

    /**
     * @return the highest level a station can reach
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Returns the levels where a station reaches a milestone: its capacity is multiplied,
     * and floors and the sell station gain a Pulpie.
     *
     * @return the milestone levels, in increasing order
     */
    public List<Integer> getMilestoneLevels() {
        return milestoneLevels;
    }

    /**
     * Returns how much each milestone multiplies the capacity of a floor or of the sell station.
     *
     * @return the multiplier of one milestone
     */
    public double getMilestoneMultiplier() {
        return milestoneMultiplier;
    }

    /**
     * Returns how much each milestone multiplies the capacity of the elevator.
     * It is higher than for the other stations, since the elevator never gains a Pulpie.
     *
     * @return the multiplier of one elevator milestone
     */
    public double getElevatorMilestoneMultiplier() {
        return elevatorMilestoneMultiplier;
    }

    /**
     * @return the maximum number of Pulpies on a floor or at the sell station
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
     * Returns how much faster the elevator gets with each level, as a share of its base speed.
     *
     * @return the speed gained per level, such as {@code 0.02} for +2% per level
     */
    public double getElevatorSpeedGrowth() {
        return elevatorSpeedGrowth;
    }

    // The value of the last floor of the table, multiplied by growth for each floor after it.
    private static double fromTable(List<Double> table, double growth, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative: " + index);
        }
        int last = table.size() - 1;
        if (index <= last) {
            return table.get(index);
        }
        return table.get(last) * Math.pow(growth, index - last);
    }
}
