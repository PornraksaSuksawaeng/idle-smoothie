package com.idlesmoothie.model.resource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Container holding smoothie batches up to a maximum capacity.
 * Used for extractor tanks, floor crates, the elevator cabin and the surface stock.
 * Batches are kept in arrival order; consecutive batches of the same fruit are merged.
 */
public class Storage {

    /** Tolerance for floating-point rounding when comparing quantities. */
    private static final double EPSILON = 1e-9;

    private final Deque<SmoothieBatch> batches = new ArrayDeque<>();
    private double capacity;

    /**
     * Creates an empty storage.
     *
     * @param capacity maximum quantity of smoothie it can hold
     * @throws IllegalArgumentException if the capacity is negative
     */
    public Storage(double capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        this.capacity = capacity;
    }

    /**
     * Stores as much of the batch as the free space allows.
     * The stored quantity is removed from the given batch, so what did not fit stays in it.
     *
     * @param batch the batch to store
     * @return the quantity actually stored
     * @throws IllegalArgumentException if the batch is {@code null}
     */
    public double store(SmoothieBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch must not be null");
        }

        double toStore = Math.min(batch.getQuantity(), getFreeSpace());
        if (toStore <= 0) {
            return 0;
        }

        SmoothieBatch part = batch.split(toStore);
        SmoothieBatch last = batches.peekLast();
        if (last != null && last.getFruit() == part.getFruit()) {
            last.merge(part);
        } else {
            batches.addLast(part);
        }
        return toStore;
    }

    /**
     * Removes up to the given quantity, oldest batches first.
     * The last batch is split if only part of it is needed.
     * Taking more than the stored amount returns everything.
     *
     * @param amount maximum quantity to take
     * @return the removed batches, oldest first, possibly empty
     * @throws IllegalArgumentException if the amount is negative
     */
    public List<SmoothieBatch> take(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        List<SmoothieBatch> taken = new ArrayList<>();
        double remaining = amount;

        while (remaining > EPSILON && !batches.isEmpty()) {
            SmoothieBatch first = batches.peekFirst();
            if (first.getQuantity() <= remaining) {
                batches.pollFirst();
                taken.add(first);
                remaining -= first.getQuantity();
            } else {
                taken.add(first.split(remaining));
                remaining = 0;
                if (first.isEmpty()) {
                    batches.pollFirst();
                }
            }
        }
        return taken;
    }

    /**
     * @return the total quantity currently stored
     */
    public double getAmount() {
        double amount = 0;
        for (SmoothieBatch batch : batches) {
            amount += batch.getQuantity();
        }
        return amount;
    }

    /**
     * @return the remaining space, never negative
     */
    public double getFreeSpace() {
        return Math.max(0, capacity - getAmount());
    }

    /**
     * @return {@code true} if the storage holds no smoothie
     */
    public boolean isEmpty() {
        return getAmount() <= EPSILON;
    }

    /**
     * @return {@code true} if no more smoothie can be stored
     */
    public boolean isFull() {
        return getFreeSpace() <= EPSILON;
    }

    /**
     * @return the maximum quantity it can hold
     */
    public double getCapacity() {
        return capacity;
    }

    /**
     * Changes the maximum capacity, for example after an upgrade.
     * Lowering it below the stored amount keeps the content; the storage is then full.
     *
     * @param capacity the new capacity
     * @throws IllegalArgumentException if the capacity is negative
     */
    public void setCapacity(double capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        this.capacity = capacity;
    }

    /**
     * Returns copies of the stored batches, for display only.
     * Modifying them has no effect on this storage.
     *
     * @return copies of the batches, oldest first
     */
    public List<SmoothieBatch> getBatches() {
        List<SmoothieBatch> copies = new ArrayList<>();
        for (SmoothieBatch batch : batches) {
            copies.add(new SmoothieBatch(batch.getFruit(), batch.getQuantity()));
        }
        return copies;
    }
}