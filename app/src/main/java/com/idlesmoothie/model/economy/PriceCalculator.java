package com.idlesmoothie.model.economy;

import com.idlesmoothie.model.resource.SmoothieBatch;

/**
 * Give the selling price of a smoothie batch.
 */
public interface PriceCalculator {
    /**
     * Returns the price of the whole batch. The batch is only read, never changed.
     *
     * @param batch the batch price
     * @return the price in coins
     */
    double priceOf(SmoothieBatch batch);
}
