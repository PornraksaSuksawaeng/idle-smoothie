package com.idlesmoothie.model.station;

/**
 * Speed and capacity of a station at a given moment, once all bonuses are applied.
 * Immutable: every "with" method returns a new instance and leaves this one unchanged.
 */
public final class StationStats {

    private final double speed;
    private final double capacity;

    /**
     * Creates station stats.
     *
     * @param speed    number of work cycles per second
     * @param capacity quantity of smoothie handled per cycle
     * @throws IllegalArgumentException if a value is negative
     */
    public StationStats(double speed, double capacity) {
        if (speed < 0 || capacity < 0) {
            throw new IllegalArgumentException("Values cannot be negative");
        }
        this.speed = speed;
        this.capacity = capacity;
    }

    /**
     * Returns a copy of these stats with the speed multiplied by the given factor.
     *
     * @param factor multiplier applied to the speed, such as {@code 1.5} for +50%
     * @return new stats with the boosted speed and the same capacity
     * @throws IllegalArgumentException if the factor is negative
     */
    public StationStats withSpeed(double factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Factor cannot be negative");
        }
        return new StationStats(speed * factor, capacity);
    }

    /**
     * Returns a copy of these stats with the capacity multiplied by the given factor.
     *
     * @param factor multiplier applied to the capacity, such as {@code 2} to double it
     * @return new stats with the boosted capacity and the same speed
     * @throws IllegalArgumentException if the factor is negative
     */
    public StationStats withCapacity(double factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Factor cannot be negative");
        }
        return new StationStats(speed, capacity * factor);
    }

    /**
     * @return the number of work cycles per second
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @return the quantity of smoothie handled per cycle
     */
    public double getCapacity() {
        return capacity;
    }
}