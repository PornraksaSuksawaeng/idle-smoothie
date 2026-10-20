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
 * @param managers      the manager of every station that has one
 */
public record GameSnapshot(double coins, List<Integer> floorLevels, int elevatorLevel, int sellLevel, List<ManagerSnapshot> managers) {

    /**
     * Keeps read-only copies of the lists, so the snapshot never changes once it is made.
     *
     * @throws NullPointerException if a list or one of its elements is {@code null}
     */
    public GameSnapshot {
        floorLevels = List.copyOf(floorLevels);
        managers = List.copyOf(managers);
    }
}