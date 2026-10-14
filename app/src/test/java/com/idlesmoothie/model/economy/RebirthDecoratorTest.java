package com.idlesmoothie.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import org.junit.jupiter.api.Test;

class RebirthDecoratorTest {

    private static final double DELTA = 1e-9;

    @Test
    void noNuggetKeepsThePrice() {
        Wallet wallet = new Wallet();
        PriceCalculator pricing = new RebirthDecorator(new BasePriceCalculator(1), wallet, 0.10);

        assertEquals(10, pricing.priceOf(new SmoothieBatch(Fruit.MANGO, 10)), DELTA);
    }

    @Test
    void eachNuggetAddsItsBonus() {
        Wallet wallet = new Wallet();
        wallet.addSugarNuggets(3);
        PriceCalculator pricing = new RebirthDecorator(new BasePriceCalculator(1), wallet, 0.10);

        assertEquals(13, pricing.priceOf(new SmoothieBatch(Fruit.MANGO, 10)), DELTA);
    }

    @Test
    void readsTheNuggetsAtEverySale() {
        Wallet wallet = new Wallet();
        PriceCalculator pricing = new RebirthDecorator(new BasePriceCalculator(1), wallet, 0.10);

        wallet.addSugarNuggets(5);

        assertEquals(15, pricing.priceOf(new SmoothieBatch(Fruit.MANGO, 10)), DELTA);
    }

    @Test
    void bonusMustNotBeNegative() {
        PriceCalculator base = new BasePriceCalculator(1);
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class, () -> new RebirthDecorator(base, wallet, -0.1));
    }

    @Test
    void walletIsRequired() {
        PriceCalculator base = new BasePriceCalculator(1);

        assertThrows(NullPointerException.class, () -> new RebirthDecorator(base, null, 0.10));
    }
}
