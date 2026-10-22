package com.idlesmoothie.model.economy;

public interface Upgradable {
    /**
     * Returns the cost of the next level only.
     *
     * @return the cost in coins, greater than 0
     */
    double getUpgradeCost();

    /**
     * Returns how many levels can still be bought before the maximum level.
     *
     * @return the number of levels left, 0 at the maximum level
     */
    int getLevelsLeft();

    /**
     * Adds levels.
     *
     * @param levels the number of levels to add
     */
    void upgrade(int levels);
}
