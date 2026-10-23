package com.idlesmoothie.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsManagerTest {

    @TempDir
    Path folder;

    @Test
    void loadReturnsWhatWasSaved() throws IOException {
        SettingsManager file = new SettingsManager(folder.resolve("settings.json"));
        Settings settings = new Settings();
        settings.setManagerAura(false);
        settings.setMusic(false);
        settings.setMusicVolume(0.8);
        settings.setNotation(NumberNotation.SCIENTIFIC);

        file.save(settings);
        Settings loaded = file.load();

        assertFalse(loaded.isManagerAura());
        assertFalse(loaded.isMusic());
        assertEquals(0.8, loaded.getMusicVolume());
        assertEquals(NumberNotation.SCIENTIFIC, loaded.getNotation());
    }

    @Test
    void loadGivesTheDefaultsWithoutAFile() throws IOException {
        Settings settings = new SettingsManager(folder.resolve("settings.json")).load();

        assertTrue(settings.isManagerAura());
        assertTrue(settings.isMusic());
        assertEquals(0.5, settings.getMusicVolume());
        assertEquals(NumberNotation.SUFFIXES, settings.getNotation());
    }

    @Test
    void missingSettingsKeepTheirDefault() throws IOException {
        Path path = folder.resolve("settings.json");
        Files.writeString(path, "{\"notation\": \"SCIENTIFIC\"}");

        Settings settings = new SettingsManager(path).load();

        assertTrue(settings.isManagerAura());
        assertEquals(NumberNotation.SCIENTIFIC, settings.getNotation());
    }

    @Test
    void loadFailsOnADamagedFile() throws IOException {
        Path path = folder.resolve("settings.json");
        Files.writeString(path, "{not json");

        assertThrows(IOException.class, () -> new SettingsManager(path).load());
    }

    @Test
    void volumeMustBeBetweenZeroAndOne() {
        Settings settings = new Settings();

        assertThrows(IllegalArgumentException.class, () -> settings.setMusicVolume(-0.1));
        assertThrows(IllegalArgumentException.class, () -> settings.setMusicVolume(1.5));
    }

    @Test
    void aVolumeOutOfRangeInTheFileIsClamped() throws IOException {
        Path path = folder.resolve("settings.json");
        Files.writeString(path, "{\"musicVolume\": 3}");

        assertEquals(1, new SettingsManager(path).load().getMusicVolume());
    }
}
