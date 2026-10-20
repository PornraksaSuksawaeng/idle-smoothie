package com.idlesmoothie.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveManagerTest {

    @TempDir
    Path folder;

    @Test
    void loadReturnsWhatWasSaved() throws IOException {
        SaveManager saves = new SaveManager(folder.resolve("save.json"));
        GameSnapshot snapshot = new GameSnapshot(1234.5, List.of(11, 5, 1), 3, 7);

        saves.save(snapshot);

        assertEquals(Optional.of(snapshot), saves.load());
    }

    @Test
    void saveReplacesThePreviousSave() throws IOException {
        SaveManager saves = new SaveManager(folder.resolve("save.json"));
        saves.save(new GameSnapshot(1, List.of(1), 1, 1));
        GameSnapshot newer = new GameSnapshot(99, List.of(4, 2), 2, 2);

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
}