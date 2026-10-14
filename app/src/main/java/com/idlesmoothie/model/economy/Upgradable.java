package com.idlesmoothie.model.economy;

public interface Upgradable {
    /**
     * Returns the cost of the next level only.
     *
     * @return the cost in coins, greater than 0
     */
    double getUpgradeCost();

    /**
     * Adds levels.
     *
     * @param levels the number of levels to add
     */
    void upgrade(int levels);
}
