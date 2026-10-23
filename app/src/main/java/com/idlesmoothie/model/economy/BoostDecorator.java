package com.idlesmoothie.model.economy;

import com.idlesmoothie.model.resource.SmoothieBatch;

public final class BoostDecorator extends PriceDecorator {
    private final double multiplier;

    /**
     * Wraps a calculator with a multiplier.
     * 
     * @param wrapped the calculator to decorate
     * @param multiplier the factor applied to its price
     * @throws NullPointerException if wrapped is null
     * @throws IllegalArgumentException if multiplier is zero or negative
     */
    public BoostDecorator(PriceCalculator wrapped, double multiplier) {
        super(wrapped);
        if (multiplier <= 0) {
            throw new IllegalArgumentException("multiplier must be positive: " + multiplier);
        }
        this.multiplier = multiplier;
    }

    /**
     * Return the wrapped price times the multiplier.
     *
     * @param batch the batch to price
     * @return the price in coins
     */
    @Override
    public double priceOf(SmoothieBatch batch) {
        return wrapped.priceOf(batch) * multiplier;
    }
}
