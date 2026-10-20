package com.idlesmoothie.model.manager;

import java.util.Random;
import java.util.function.DoubleSupplier;

import com.idlesmoothie.model.station.StationType;

/**
 * Builds the managers the player buys (Factory pattern).
 * Rolls the rarity, weighted by {@link Rarity#getWeight()}, and the kind of bonus, speed or capacity.
 * This is the only randomness of the game.
 */
public class ManagerFactory {

    private final DoubleSupplier random;

    /**
     * Creates a factory that rolls with a real random generator.
     */
    public ManagerFactory() {
        this(new Random()::nextDouble);
    }

    /**
     * Creates a factory with the given source of randomness, so tests can choose the rolls.
     *
     * @param random gives numbers between 0 (included) and 1 (excluded)
     * @throws IllegalArgumentException if {@code random} is {@code null}
     */
    public ManagerFactory(DoubleSupplier random) {
        if (random == null) {
            throw new IllegalArgumentException("Random must not be null");
        }
        this.random = random;
    }

    /**
     * Builds a new manager for the given kind of station, with a rolled rarity and bonus.
     *
     * @param type the kind of station the manager will work on
     * @return the new manager
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    public Manager create(StationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Station type must not be null");
        }
        Rarity rarity = rollRarity();
        return new Manager(rarity, type, rollAbility(rarity));
    }

    // Draws a number below the total weight, then walks the rarities until it falls in one of them.
    private Rarity rollRarity() {
        double total = 0;
        for (Rarity rarity : Rarity.values()) {
            total += rarity.getWeight();
        }
        double roll = random.getAsDouble() * total;
        for (Rarity rarity : Rarity.values()) {
            roll -= rarity.getWeight();
            if (roll < 0) {
                return rarity;
            }
        }
        return Rarity.values()[Rarity.values().length - 1];
    }

    // Heads or tails between a speed and a capacity bonus, as strong as the rarity allows.
    private ManagerAbility rollAbility(Rarity rarity) {
        double factor = rarity.getBonusMultiplier();
        if (random.getAsDouble() < 0.5) {
            return new SpeedBoostAbility(factor);
        }
        return new CapacityBoostAbility(factor);
    }
}