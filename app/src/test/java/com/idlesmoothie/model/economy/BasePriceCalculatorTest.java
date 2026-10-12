package com.idlesmoothie.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import org.junit.jupiter.api.Test;

class BasePriceCalculatorTest {

    private static final double DELTA = 1e-9;

    @Test
    void priceIsQuantityTimesSmoothiePrice() {
        PriceCalculator pricing = new BasePriceCalculator(2.5);

        double price = pricing.priceOf(new SmoothieBatch(Fruit.STRAWBERRY, 4));

        assertEquals(10, price, DELTA);
    }

    @Test
    void everyFruitHasTheSamePrice() {
        PriceCalculator pricing = new BasePriceCalculator(3);

        for (Fruit fruit : Fruit.values()) {
            assertEquals(6, pricing.priceOf(new SmoothieBatch(fruit, 2)), DELTA);
        }
    }

    @Test
    void emptyBatchIsWorthNothing() {
        PriceCalculator pricing = new BasePriceCalculator(3);

        assertEquals(0, pricing.priceOf(new SmoothieBatch(Fruit.KIWI, 0)), DELTA);
    }

    @Test
    void pricingDoesNotChangeTheBatch() {
        PriceCalculator pricing = new BasePriceCalculator(3);
        SmoothieBatch batch = new SmoothieBatch(Fruit.MANGO, 5);

        pricing.priceOf(batch);

        assertEquals(5, batch.getQuantity(), DELTA);
    }

    @Test
    void SmoothiePriceMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new BasePriceCalculator(0));
        assertThrows(IllegalArgumentException.class, () -> new BasePriceCalculator(-1));
    }

    @Test
    void priceOfRejectsNullBatch() {
        PriceCalculator pricing = new BasePriceCalculator(1);

        assertThrows(NullPointerException.class, () -> pricing.priceOf(null));
    }
}
