package com.idlesmoothie.core;

import java.util.UUID;

import com.idlesmoothie.model.manager.Rarity;
import com.idlesmoothie.model.station.StationType;

/**
 * Saved copy of a manager and of where it works, so loading rebuilds the same manager instead of rolling again.
 *
 * @param stationType   the kind of station it works on
 * @param floorIndex    the floor it works on, ignored for the elevator and the sell station
 * @param managersHired how many managers were bought for that station, so rerolls keep their price
 * @param id            the manager's id
 * @param rarity        the rarity rolled at purchase
 * @param speedBonus    {@code true} for a speed bonus, {@code false} for a capacity bonus
 * @param factor        how much the bonus multiplies the stat
 */
public record ManagerSnapshot(StationType stationType, int floorIndex, int managersHired, UUID id, Rarity rarity, boolean speedBonus, double factor) {}