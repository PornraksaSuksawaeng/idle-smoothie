package com.idlesmoothie.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyServiceTest {

    private static final double DELTA = 1e-9;

    private Wallet wallet;
    private EconomyService economy;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        economy = new EconomyService(wallet, 1, 0.10);
    }

    @Test
    void withoutBoostThePriceIsTheBasePrice() {
        assertEquals(10, priceOfTen(), DELTA);
    }

    @Test
    void boostAppliesAsSoonAsItIsAdded() {
        economy.addBoost(new Boost("Potion", 2, 30));

        assertEquals(20, priceOfTen(), DELTA);
    }

    @Test
    void boostsMultiplyEachOther() {
        economy.addBoost(new Boost("Potion", 2, 30));
        economy.addBoost(new Boost("Rush", 3, 10));

        assertEquals(60, priceOfTen(), DELTA);
    }

    @Test
    void runningBoostsLoseTimeAtEachUpdate() {
        economy.addBoost(new Boost("Potion", 2, 30));

        economy.update(5);

        assertEquals(25, economy.getActiveBoosts().get(0).getRemaining(), DELTA);
        assertEquals(20, priceOfTen(), DELTA);
    }

    @Test
    void expiredBoostIsRemovedAndThePriceGoesBack() {
        economy.addBoost(new Boost("Potion", 2, 30));
        economy.addBoost(new Boost("Rush", 3, 10));

        economy.update(10);

        assertEquals(1, economy.getActiveBoosts().size());
        assertEquals(20, priceOfTen(), DELTA);
    }

    @Test
    void sugarNuggetsRaiseThePrice() {
        wallet.addSugarNuggets(2);

        assertEquals(12, priceOfTen(), DELTA);
    }

    @Test
    void activeBoostsCannotBeChangedFromOutside() {
        economy.addBoost(new Boost("Potion", 2, 30));
        List<Boost> boosts = economy.getActiveBoosts();

        assertThrows(UnsupportedOperationException.class, () -> boosts.clear());
        assertEquals(1, economy.getActiveBoosts().size());
    }

    @Test
    void nullBoostIsRejected() {
        assertThrows(NullPointerException.class, () -> economy.addBoost(null));
    }

    @Test
    void invalidBalanceValuesAreRejected() {
        assertThrows(NullPointerException.class, () -> new EconomyService(null, 1, 0.10));
        assertThrows(IllegalArgumentException.class, () -> new EconomyService(wallet, 0, 0.10));
        assertThrows(IllegalArgumentException.class, () -> new EconomyService(wallet, 1, -0.10));
    }

    private double priceOfTen() {
        return economy.getPricing().priceOf(new SmoothieBatch(Fruit.STRAWBERRY, 10));
    }
}
