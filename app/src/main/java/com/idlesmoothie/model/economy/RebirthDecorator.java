package com.idlesmoothie.model.economy;

import com.idlesmoothie.model.resource.SmoothieBatch;
import java.util.Objects;

public final class RebirthDecorator extends PriceDecorator {
    
    private final Wallet wallet;
    private final double bonusPerNugget;

    /**
     * Wraps a calculator with the nugget bonus.
     *
     * @param wrapped the calculator to decorate
     * @param wallet the wallet holding the sugar nuggets
     * @param bonusPerNugget the bonus of one nugget, 0.10 means + 10%
     * @throws NullPointerException if wrapped or wallet is null
     * @throws IllegalArgumentException if bonusPerNugget is negative
     */
    public RebirthDecorator(PriceCalculator wrapped, Wallet wallet, double bonusPerNugget) {
        super(wrapped);
        Objects.requireNonNull(wallet, "wallet");
        if (bonusPerNugget < 0) {
            throw new IllegalArgumentException("bonusPerNugget must not be negative: " + bonusPerNugget);
        }
        this.wallet = wallet;
        this.bonusPerNugget = bonusPerNugget;
    }

    /**
     * Return the wrapped price time (1 + bonusPerNugget * nuggets).
     *
     * @param batch the batch price
     * @return the price in coins 
     */
    @Override
    public double priceOf(SmoothieBatch batch) {
        double multiplier = 1 + bonusPerNugget * wallet.getSugarNuggets();
        return wrapped.priceOf(batch) * multiplier;
    }
}
