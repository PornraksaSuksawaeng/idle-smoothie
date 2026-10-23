package com.idlesmoothie.model.manager;

import com.idlesmoothie.model.station.StationStats;

/**
 * Manager ability that makes its station work faster.
 */
public final class SpeedBoostAbility implements ManagerAbility {

    private static final double EPSILON = 1e-9;

    private final double factor;

    /**
     * Creates the ability.
     *
     * @param factor multiplier applied to the station speed, such as {@code 1.5} for +50%
     * @throws IllegalArgumentException if {@code factor} is less than 1
     */
    public SpeedBoostAbility(double factor) {
        if (factor < 1) {
            throw new IllegalArgumentException("A bonus cannot slow the station down: " + factor);
        }
        this.factor = factor;
    }

    @Override
    public StationStats apply(StationStats stats) {
        if (stats == null) {
            throw new IllegalArgumentException("Stats must not be null");
        }
        return stats.withSpeed(factor);
    }

    @Override
    public String describe() {
        if (factor - 1 < EPSILON) {
            return "Automation only";
        }
        return "+" + Math.round((factor - 1) * 100) + "% speed";
    }

    /**
     * Returns the multiplier applied to the station speed.
     *
     * @return the factor, at least 1
     */
    public double getFactor() {
        return factor;
    }
}