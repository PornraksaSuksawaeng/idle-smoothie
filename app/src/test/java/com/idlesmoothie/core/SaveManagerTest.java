package com.idlesmoothie.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.idlesmoothie.model.manager.Rarity;
import com.idlesmoothie.model.station.StationType;

class SaveManagerTest {

    @TempDir
    Path folder;

    @Test
    void loadReturnsWhatWasSaved() throws IOException {
        SaveManager saves = new SaveManager(folder.resolve("save.json"));
        ManagerSnapshot manager = new ManagerSnapshot(StationType.ELEVATOR, 0, 2, UUID.randomUUID(),
        Rarity.EPIC, true, 2.0);
        GameSnapshot snapshot = new GameSnapshot(1234.5, List.of(11, 5, 1), 3, 7, List.of(manager));

        saves.save(snapshot);

        assertEquals(Optional.of(snapshot), saves.load());
    }

    @Test
    void saveReplacesThePreviousSave() throws IOException {
        SaveManager saves = new SaveManager(folder.resolve("save.json"));
        saves.save(new GameSnapshot(1, List.of(1), 1, 1, List.of()));
        GameSnapshot newer = new GameSnapshot(99, List.of(4, 2), 2, 2, List.of());

        saves.save(newer);

        assertEquals(Optional.of(newer), saves.load());
    }

    @Test
    void loadIsEmptyWithoutASaveFile() throws IOException {
        SaveManager saves = new SaveManager(folder.resolve("save.json"));

        assertTrue(saves.load().isEmpty());
    }

    @Test
    void loadIsEmptyWhenTheFileIsEmpty() throws IOException {
        Path file = folder.resolve("save.json");
        Files.writeString(file, "");

        assertTrue(new SaveManager(file).load().isEmpty());
    }

    @Test
    void nullArgumentsAreRejected() {
        SaveManager saves = new SaveManager(folder.resolve("save.json"));

        assertThrows(IllegalArgumentException.class, () -> new SaveManager(null));
        assertThrows(IllegalArgumentException.class, () -> saves.save(null));
    }

    @Test
    void damagedFileIsReportedAsAnIoError() throws IOException {
        Path file = folder.resolve("save.json");
        Files.writeString(file, "{ \"coins\": 12, \"floorLevels\": [");

        assertThrows(IOException.class, () -> new SaveManager(file).load());
    }

    @Test
    void saveLeavesNoTemporaryFile() throws IOException {
        Path file = folder.resolve("save.json");

        new SaveManager(file).save(new GameSnapshot(1, List.of(1), 1, 1, List.of()));

        assertFalse(Files.exists(folder.resolve("save.json.tmp")));
    }
}