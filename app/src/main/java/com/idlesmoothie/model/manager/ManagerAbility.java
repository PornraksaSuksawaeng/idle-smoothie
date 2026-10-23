package com.idlesmoothie.model.manager;

import com.idlesmoothie.model.station.StationStats;

/**
 * Bonus a manager gives to its station (Strategy pattern).
 */
public interface ManagerAbility {

    /**
     * Returns the given stats with the bonus applied; the original stats are not changed.
     *
     * @param stats the stats before the bonus
     * @return the stats after the bonus
     */
    StationStats apply(StationStats stats);

    /**
     * Returns the text shown to the player, such as {@code "+50% speed"}.
     *
     * @return the description
     */
    String describe();
}