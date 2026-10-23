package com.idlesmoothie.model.manager;

import java.util.UUID;

import com.idlesmoothie.model.station.Station;
import com.idlesmoothie.model.station.StationStats;
import com.idlesmoothie.model.station.StationType;

/**
 * Manager hired for one kind of station: it automates the station and boosts it with its ability.
 */
public final class Manager {

    private final UUID id;
    private final Rarity rarity;
    private final StationType stationType;
    private final ManagerAbility ability;

    /**
     * Creates a new manager with a fresh id, for a new hire.
     *
     * @param rarity      rarity rolled at purchase
     * @param stationType kind of station the manager can work on
     * @param ability     bonus given to the station
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public Manager(Rarity rarity, StationType stationType, ManagerAbility ability) {
        this(UUID.randomUUID(), rarity, stationType, ability);
    }

    /**
     * Rebuilds a manager with a known id, for a saved game.
     *
     * @param id          unique id, kept across saves
     * @param rarity      rarity rolled at purchase
     * @param stationType kind of station the manager can work on
     * @param ability     bonus given to the station
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public Manager(UUID id, Rarity rarity, StationType stationType, ManagerAbility ability) {
        if (id == null || rarity == null || stationType == null || ability == null) {
            throw new IllegalArgumentException("Id, rarity, station type and ability must not be null");
        }
        this.id = id;
        this.rarity = rarity;
        this.stationType = stationType;
        this.ability = ability;
    }

    /**
     * Returns the stats of the station with this manager's bonus applied.
     *
     * @param stats the stats before the bonus
     * @return the stats after the bonus
     */
    public StationStats apply(StationStats stats) {
        return ability.apply(stats);
    }

    /**
     * Tells whether this manager can be assigned to the given station.
     *
     * @param station the station to check
     * @return {@code true} if the station is of this manager's kind
     */
    public boolean canWork(Station station) {
        return station != null && station.getType() == stationType;
    }

    /**
     * Returns the unique id of this manager, kept across saves.
     *
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the rarity rolled when the manager was bought.
     *
     * @return the rarity
     */
    public Rarity getRarity() {
        return rarity;
    }

    /**
     * Returns the kind of station this manager can work on.
     *
     * @return the station type
     */
    public StationType getStationType() {
        return stationType;
    }

    /**
     * Returns the bonus this manager gives to its station.
     *
     * @return the ability
     */
    public ManagerAbility getAbility() {
        return ability;
    }
}