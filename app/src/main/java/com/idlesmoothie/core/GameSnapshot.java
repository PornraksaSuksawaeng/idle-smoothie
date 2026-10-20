package com.idlesmoothie.core;

import java.util.List;

/**
 * A copy of the game state made of plain values, to save and load a game (Memento pattern).
 * Pulpie and elevator positions and crate contents are not saved: they start from zero on load.
 *
 * @param coins         the player's coins
 * @param floorLevels   the level of each unlocked floor, from the top one to the deepest one
 * @param elevatorLevel the level of the elevator
 * @param sellLevel     the level of the sell station
 */
public record GameSnapshot(double coins, List<Integer> floorLevels, int elevatorLevel, int sellLevel) {

    /**
     * Keeps a read-only copy of the floor levels, so the snapshot never changes once it is made.
     *
     * @throws NullPointerException if {@code floorLevels} or one of its levels is {@code null}
     */
    public GameSnapshot {
        floorLevels = List.copyOf(floorLevels);
    }
}