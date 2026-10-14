package com.idlesmoothie.model.economy;

import java.util.Objects;

/**
 * A temporary price multiplier, for example a x2 potions for 30 seconds.
 */
public final class Boost {
    private static final double EPSILON = 1e-9;

    private final String name;
    private final double multiplier;
    private double remaining;

    /**
     * Create a boost that start now.
     *
     * @param name the name shown to the player
     * @param multiplier the factor applied to the price, 2 double it
     * @param duration how long the boost last, in second
     * @throws NullPoiterException if name is null
     * @throws IllegalArgumentException if multiplier or duration is zero or negative
     */
    public Boost(String name, double multiplier, double duration) {
        Objects.requireNonNull(name, "name");
        if (multiplier <= 0) {
            throw new IllegalArgumentException("multiplier must be positive: " + multiplier);
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be positive: " + duration);
        }
        this.name = name;
        this.multiplier = multiplier;
        this.remaining = duration;
    }

    /**
     * return true once the the time is up
     */
    public boolean isExpired() {
        return remaining <= EPSILON;
    }

    /**
     * return the name shown to the player
     */
    public String getName() {
        return name;
    }

    /**
     * return factor applied to the price
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * return the second left, never negative
     */
    public double getRemaining() {
        return remaining;
    }

    // only economyService calls it. 
    void update(double dt) {
        remaining -= dt;
        if (remaining < 0) {
            remaining = 0;
        } 
    }
}
