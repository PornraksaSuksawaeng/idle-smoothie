package com.idlesmoothie.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import org.junit.jupiter.api.Test;

class BoostDecoratorTest {

    private static final double DELTA = 1e-9;

    @Test
    void multipliesTheWrappedPrice() {
        PriceCalculator pricing = new BoostDecorator(new BasePriceCalculator(1), 2);

        assertEquals(20, pricing.priceOf(new SmoothieBatch(Fruit.STRAWBERRY, 10)), DELTA);
    }

    @Test
    void twoBoostsMultiplyEachOther() {
        PriceCalculator once = new BoostDecorator(new BasePriceCalculator(1), 2);
        PriceCalculator twice = new BoostDecorator(once, 3);

        assertEquals(60, twice.priceOf(new SmoothieBatch(Fruit.KIWI, 10)), DELTA);
    }

    @Test
    void multiplierMustBePositive() {
        PriceCalculator base = new BasePriceCalculator(1);

        assertThrows(IllegalArgumentException.class, () -> new BoostDecorator(base, 0));
        assertThrows(IllegalArgumentException.class, () -> new BoostDecorator(base, -1));
    }

    @Test
    void wrappedCalculatorIsRequired() {
        assertThrows(NullPointerException.class, () -> new BoostDecorator(null, 2));
    }
}
