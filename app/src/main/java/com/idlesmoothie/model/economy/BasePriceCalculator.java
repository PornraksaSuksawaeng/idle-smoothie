package com.idlesmoothie.model.economy;

import com.idlesmoothie.model.resource.SmoothieBatch;
import java.util.Objects;

/**
 * Price every smoothie the same, whatever its fruit. The fruit is only visual.
 */
public final class BasePriceCalculator implements PriceCalculator {
    private final double smoothiePrice;

    /**
     * Create a caculator with one price for every smoothie.
     *
     * @param smoothiePrice the coins earned for one unit of smoothie.
     * @throws IllegalArgumentException if smoothiePrice is zero or negative
     */
    public BasePriceCalculator(double smoothiePrice) {
        if (smoothiePrice <= 0) {
            throw new IllegalArgumentException("smoothiePrice must be positive: " + smoothiePrice);
        }
        this.smoothiePrice = smoothiePrice;
    }

    @Override
    public double priceOf(SmoothieBatch batch) {
        Objects.requireNonNull(batch, "batch");
        return batch.getQuantity() * smoothiePrice;
    }
}
