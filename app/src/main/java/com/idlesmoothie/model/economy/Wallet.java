package com.idlesmoothie.model.economy;

public class Wallet {
    private static final double EPSILON = 1e-9;

    private double coins;
    private int sugarNuggets;
    
    /**
     * Create an empty wallet.
     */
    public Wallet() {
        coins = 0;
        sugarNuggets = 0;
    }

    /**
     * Add coins, for example after a sale.
     *
     * @param amount the coins to add
     * throw IllegalArgumentException if amount is negative, NaN or infinite
     */
    public void addCoins(double amount) {
        checkAmount(amount);
        coins += amount;
    }

    /**
     * Spend coins if the wallet can pay them. If it cannot, nothing change.
     *
     * @param amount the coins to spend
     * @return true if the coins were spent, false if the wallet is short
     * @throws IllegalArgumentException if amount is negative, NaN or infinite
     */
    public boolean spendCoins(double amount) {
        checkAmount(amount);
        if (!canAfford(amount)) {
            return false;
        }

        coins -= amount;
        if (coins < 0) {
            // canAfford accept a rounding error of EPSILON, it must not become a debt
            coins = 0;
        }
        return true;
    }
    
    /**
     * Tell if the wallet can pay the amount. A rounding error smaller than 1e-9 still counts as enough: 0.7 + 0.1 coins can pay 0.8.
     *
     * @param amount the coins needed
     * @return true if the wallet holds enough coins
     */
    public boolean canAfford(double amount) {
        return coins + EPSILON >= amount;
    }
    
    /**
     * Set the coins back to 0, for a rebirth. The sugar nuggets are kept.
     */
    public void resetCoins() {
        coins = 0;
    }

    /**
     * return the coins in the wallet, never negative
     */
    public double getCoins() {
        return coins;
    }

    /**
     * Adds sugar nuggets, the rebirth currency that give a permanent price bonus.
     *
     * @param amount the nuggets to add
     * @throws IllegalArgumentException if amount is negative
     */
    public void addSugarNuggets(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        sugarNuggets += amount;
    }

    /**
     * return the sugar nuggets in the wallet
     */
    public int getSugarNuggets() {
        return sugarNuggets;
    }

    private static void checkAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("amount must be a finite number, 0 or more: " + amount);
        }
    }
}
