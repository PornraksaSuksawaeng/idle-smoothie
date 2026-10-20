package com.idlesmoothie.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.idlesmoothie.model.economy.BasePriceCalculator;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.station.Mine;

class GameSnapshotTest {

    private static final double DELTA = 1e-9;

    private final BalanceConfig config = new BalanceConfig();

    @Test
    void restoreRebuildsAGameThatCrossedABarrier() {
        Wallet wallet = new Wallet();
        Game game = new Game(config, new BasePriceCalculator(config.getSmoothieBasePrice()), wallet);
        Mine mine = game.getMine();
        wallet.addCoins(1e12);
        while (!mine.isBehindBarrier(mine.getFloors().size())) {
            game.buyNextFloor();
        }
        mine.getDeepestFloor().upgrade(config.getBarrierMinLevel());
        game.buyNextFloor();
        mine.getFloors().get(1).upgrade(4);
        mine.getElevator().upgrade(2);
        mine.getSellStation().upgrade(6);
        Game restored = new Game(config);

        restored.restore(game.createSnapshot());

        Mine restoredMine = restored.getMine();
        assertTrue(restoredMine.isBehindBarrier(restoredMine.getFloors().size() - 1));
        assertEquals(game.getCoins(), restored.getCoins(), DELTA);
        assertEquals(mine.getFloors().size(), restoredMine.getFloors().size());
        for (int i = 0; i < mine.getFloors().size(); i++) {
            assertEquals(mine.getFloors().get(i).getLevel(), restoredMine.getFloors().get(i).getLevel());
        }
        assertEquals(mine.getElevator().getLevel(), restoredMine.getElevator().getLevel());
        assertEquals(mine.getSellStation().getLevel(), restoredMine.getSellStation().getLevel());
    }

    @Test
    void restoreRefusesAGameThatAlreadyProgressed() {
        GameSnapshot snapshot = new Game(config).createSnapshot();
        Game game = new Game(config);
        game.getMine().getElevator().upgrade(1);

        assertThrows(IllegalStateException.class, () -> game.restore(snapshot));
    }

    @Test
    void restoreRejectsNullSnapshot() {
        Game game = new Game(config);

        assertThrows(IllegalArgumentException.class, () -> game.restore(null));
    }

    @Test
    void snapshotKeepsItsOwnCopyOfTheFloorLevels() {
        List<Integer> levels = new ArrayList<>(List.of(3, 1));
        GameSnapshot snapshot = new GameSnapshot(10, levels, 1, 1);

        levels.set(0, 99);

        assertEquals(List.of(3, 1), snapshot.floorLevels());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.floorLevels().add(2));
    }
}