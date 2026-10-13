package com.idlesmoothie.model.economy;

/**
 * What one press of the upgrade button gave.
 *
 * @param levelsGained the levels added, 0 if the wallet could not pay
 * @param coinsSpent the coins paid, 0 if the wallet could not pay
 */
public record UpgradeReport(int levelsGained, double coinsSpent) {
}
