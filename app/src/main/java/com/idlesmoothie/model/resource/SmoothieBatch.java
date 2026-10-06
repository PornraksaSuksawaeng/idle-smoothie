package com.idlesmoothie.model.resource;

/**
 * A batch of smoothies made from a single fruit.
 * Batches move along the production chain: they can be split into smaller batches
 * or merged with another batch of the same fruit.
 */
public class SmoothieBatch {

    /** Tolerance for floating-point rounding when checking if a batch is empty. */
    private static final double EPSILON = 1e-9;

    private final Fruit fruit;
    private double quantity;

    /**
     * Creates a batch of smoothies.
     *
     * @param fruit    the fruit the smoothies are made from
     * @param quantity the number of smoothies in the batch
     * @throws IllegalArgumentException if the fruit is {@code null} or the quantity is negative
     */
    public SmoothieBatch(Fruit fruit, double quantity) {
        if (fruit == null) {
            throw new IllegalArgumentException("Fruit must not be null");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.fruit = fruit;
        this.quantity = quantity;
    }

    /**
     * Removes up to the given amount from this batch and returns it as a new batch.
     * If the amount exceeds what this batch holds, everything is taken.
     *
     * @param amount maximum quantity to take
     * @return a new batch of the same fruit holding the quantity actually taken
     * @throws IllegalArgumentException if the amount is negative
     */
    public SmoothieBatch split(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        double taken = Math.min(amount, quantity);
        quantity -= taken;
        return new SmoothieBatch(fruit, taken);
    }

    /**
     * Moves the whole content of another batch into this one.
     * The other batch is left empty, so no smoothie is ever duplicated.
     *
     * @param other the batch to pour into this one, must be of the same fruit
     * @return this batch, for chaining
     * @throws IllegalArgumentException if {@code other} is this batch or holds a different fruit
     */
    public SmoothieBatch merge(SmoothieBatch other) {
        if (other == this) {
            throw new IllegalArgumentException("Cannot merge a batch with itself");
        }
        if (other.fruit != this.fruit) {
            throw new IllegalArgumentException("Cannot merge batches of different fruits");
        }
        this.quantity += other.quantity;
        other.quantity = 0;
        return this;
    }

    /**
     * @return {@code true} if the batch holds no smoothie
     */
    public boolean isEmpty() {
        return quantity <= EPSILON;
    }

    /**
     * @return the fruit the smoothies are made from
     */
    public Fruit getFruit() {
        return fruit;
    }

    /**
     * @return the number of smoothies in the batch
     */
    public double getQuantity() {
        return quantity;
    }
}