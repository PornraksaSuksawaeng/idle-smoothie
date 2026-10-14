package com.idlesmoothie.model.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EconomyService {
    
    private final Wallet wallet;
    private final PriceCalculator base;
    private final double bonusPerNugget;
    private final List<Boost> boosts;
    private PriceCalculator pricing;

    /**
     * Create the service with no boost running.
     *
     * @param wallet the wallet holding the sugar nuggets
     * @param smoothiePrice
     * @param bonusPerNugget
     * @throws NullPointerException if wallet is null
     * @throws IllegalArgumentException if smoothiePrice is zero or negative or bonusPerNugget is negative
     */
    public EconomyService(Wallet wallet, double smoothiePrice, double bonusPerNugget) {
        this.wallet = wallet;
        this.base = new BasePriceCalculator(smoothiePrice);
        this.bonusPerNugget = bonusPerNugget;
        this.boosts = new ArrayList<>();
        rebuildPricing(); // the decorators check wallet and bonusPerNugget
    }

    /**
     * Start a boost. The new price applies at once.
     *
     * @param boost the boost start
     * @throws NullPointerException if boost is null
     */
    public void addBoost(Boost boost) {
        Objects.requireNonNull(boost, "boost");
        boosts.add(boost);
        rebuildPricing();
    }

    /**
     * Make time pass for every boost and remove the expired ones.
     *
     * @param dt the seconds since the last update
     */
    public void update(double dt) {
        for (Boost boost : boosts) {
            boost.update(dt);
        }
        boolean removed = boosts.removeIf(Boost::isExpired);
        if (removed) {
            rebuildPricing();
        }
    }

    /**
    * Returns the current price chain. A new chain is built when a boost
    * starts or ends, so the sell station must get it again after
    * {@link #addBoost} and {@link #update}.
    *
    * @return the calculator to give to the sell station
    */
    public PriceCalculator getPricing() {
        return pricing;
    }

    /**
    * {@return a read-only copy of the running boosts, oldest first}
    */
    public List<Boost> getActiveBoosts() {
        return List.copyOf(boosts);
    }

    private void rebuildPricing() {
        PriceCalculator chain = base;
        for (Boost boost : boosts) {
            chain = new BoostDecorator(chain, boost.getMultiplier());
        }
        pricing = new RebirthDecorator(chain, wallet, bonusPerNugget);
    }
}
