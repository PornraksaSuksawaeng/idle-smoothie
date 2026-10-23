package com.idlesmoothie.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Writes the settings file and reads it back, in JSON with Gson.
 */
public final class SettingsManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;

    /**
     * Creates a manager for one settings file.
     *
     * @param path the settings file, for example {@code Path.of("settings.json")}
     * @throws IllegalArgumentException if {@code path} is {@code null}
     */
    public SettingsManager(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        this.path = path;
    }

    /**
     * Writes the settings to the file, replacing the previous ones.
     *
     * @param settings the settings to save
     * @throws IllegalArgumentException if {@code settings} is {@code null}
     * @throws IOException if the file cannot be written
     */
    public void save(Settings settings) throws IOException {
        if (settings == null) {
            throw new IllegalArgumentException("Settings must not be null");
        }
        Files.writeString(path, GSON.toJson(settings));
    }

    /**
     * Reads the settings file.
     *
     * @return the saved settings, or the default ones if the file does not exist or is empty
     * @throws IOException if the file cannot be read, or is damaged
     */
    public Settings load() throws IOException {
        if (!Files.exists(path)) {
            return new Settings();
        }
        String json = Files.readString(path);
        try {
            Settings settings = GSON.fromJson(json, Settings.class);
            return settings == null ? new Settings() : settings;
        } catch (RuntimeException e) {
            // Gson throws an unchecked exception when the JSON is broken or does not match the settings.
            throw new IOException("The settings file is damaged: " + path, e);
        }
    }
}
