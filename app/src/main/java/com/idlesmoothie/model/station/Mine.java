package com.idlesmoothie.model.station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.economy.PriceCalculator;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.Storage;

/**
 * The whole production chain: the floors, the elevator, the surface stock and the sell station.
 * Builds and links the stations, updates them in order, and handles the unlocking of new floors.
 * Floors are grouped in fruit zones; the first floor of each new zone is behind a barrier.
 */
public class Mine {

    private final BalanceConfig config;
    private final List<Floor> floors = new ArrayList<>();
    private final Storage surface = new Storage(Double.POSITIVE_INFINITY);
    private final Elevator elevator;
    private final SellStation sellStation;

    /**
     * Creates a mine with its first floor, an empty surface and every station at level 1.
     *
     * @param priceCalculator gives the price of the smoothie sold
     * @param wallet receives the coins of each sale
     * @param config game balance values
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public Mine(PriceCalculator priceCalculator, Wallet wallet, BalanceConfig config) {
        if (priceCalculator == null || wallet == null || config == null) {
            throw new IllegalArgumentException("Price calculator, wallet and config must not be null");
        }
        this.config = config;
        this.elevator = new Elevator(floors, surface, config);
        this.sellStation = new SellStation(surface, priceCalculator, wallet, config);
        addFloor();
    }

    /**
     * Updates the floors, then the elevator, then the sell station.
     *
     * @param dt elapsed time since the last update, in seconds
     * @throws IllegalArgumentException if {@code dt} is negative
     */
    public void update(double dt) {
        for (Floor floor : floors) {
            floor.update(dt);
        }
        elevator.update(dt);
        sellStation.update(dt);
    }

    /**
     * @return the fruit of the floor at the given index, by zone of {@code floorsPerFruit} floors
     * @throws IllegalArgumentException if the index is outside the mine
     */
    public Fruit fruitForFloor(int index) {
        if (index < 0 || index >= getMaxFloors()) {
            throw new IllegalArgumentException("Floor index out of the mine: " + index);
        }
        return Fruit.values()[index / config.getFloorsPerFruit()];
    }

    /**
     * @return {@code true} if a barrier stands before the floor at the given index,
     *         which is the case for the first floor of every zone except the first one
     */
    public boolean isBehindBarrier(int index) {
        return index > 0 && index % config.getFloorsPerFruit() == 0;
    }

    /**
     * @return the number of floors when the mine is complete
     */
    public int getMaxFloors() {
        return Fruit.values().length * config.getFloorsPerFruit();
    }

    /**
     * @return {@code true} if the mine still has floors to unlock
     */
    public boolean hasNextFloor() {
        return floors.size() < getMaxFloors();
    }

    /**
     * Deeper floors cost more; a floor behind a barrier costs {@code barrierCostMultiplier} times more.
     *
     * @return the price in coins of the next floor
     * @throws IllegalStateException if the mine is complete
     */
    public double getNextFloorCost() {
        if (!hasNextFloor()) {
            throw new IllegalStateException("The mine is complete");
        }
        int index = floors.size();
        double cost = config.getNewFloorBaseCost() * Math.pow(config.getNewFloorCostGrowth(), index - 1);
        if (isBehindBarrier(index)) {
            cost *= config.getBarrierCostMultiplier();
        }
        return cost;
    }

    /**
     * Checks everything but the price, which is the shop's job.
     * A barrier also needs the deepest floor to reach {@code barrierMinLevel}.
     *
     * @return {@code true} if the next floor can be unlocked
     */
    public boolean canUnlockNextFloor() {
        if (!hasNextFloor()) {
            return false;
        }
        if (!isBehindBarrier(floors.size())) {
            return true;
        }
        return getDeepestFloor().getLevel() >= config.getBarrierMinLevel();
    }

    /**
     * Adds the next floor at level 1 with one Pulpie. Does not take any coins: the shop pays first.
     *
     * @return the new floor
     * @throws IllegalStateException if the next floor cannot be unlocked
     */
    public Floor unlockNextFloor() {
        if (!canUnlockNextFloor()) {
            throw new IllegalStateException("The next floor cannot be unlocked");
        }
        return addFloor();
    }

    /**
     * Compares the production of all floors, the elevator and the sell station.
     * The slowest one sets the pace of the whole mine.
     *
     * @return the type of station that limits the income
     */
    public StationType findBottleneck() {
        double production = 0;
        for (Floor floor : floors) {
            production += floor.getThroughput();
        }
        StationType bottleneck = StationType.FLOOR;
        double lowest = production;
        if (elevator.getThroughput() < lowest) {
            bottleneck = StationType.ELEVATOR;
            lowest = elevator.getThroughput();
        }
        if (sellStation.getThroughput() < lowest) {
            bottleneck = StationType.SELL;
        }
        return bottleneck;
    }

    /**
     * @return the floors, from the top one to the deepest one, read-only
     */
    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    /**
     * @return the deepest unlocked floor
     */
    public Floor getDeepestFloor() {
        return floors.get(floors.size() - 1);
    }

    /**
     * @return the elevator
     */
    public Elevator getElevator() {
        return elevator;
    }

    /**
     * @return the sell station
     */
    public SellStation getSellStation() {
        return sellStation;
    }

    /**
     * @return the quantity of smoothie waiting at the surface to be sold
     */
    public double getSurfaceAmount() {
        return surface.getAmount();
    }

    private Floor addFloor() {
        int index = floors.size();
        Floor floor = new Floor(index, fruitForFloor(index), config);
        floors.add(floor);
        return floor;
    }
}