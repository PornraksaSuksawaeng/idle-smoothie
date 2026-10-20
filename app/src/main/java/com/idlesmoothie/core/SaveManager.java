package com.idlesmoothie.core;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Writes the save file and reads it back, in JSON with Gson.
 * Keeps the snapshots made by {@link Game} without looking inside them (caretaker of the Memento pattern).
 */
public final class SaveManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;

    /**
     * Creates a manager for one save file. The file is only touched by {@link #save} and {@link #load}.
     *
     * @param path the save file, for example {@code Path.of("save.json")}
     * @throws IllegalArgumentException if {@code path} is {@code null}
     */
    public SaveManager(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        this.path = path;
    }

    /**
     * Writes the snapshot to the file, replacing the previous save.
     * The JSON goes to a temporary file first, then replaces the save in one move,
     * so a crash while writing never leaves a half-written save.
     *
     * @param snapshot the state to save
     * @throws IllegalArgumentException if {@code snapshot} is {@code null}
     * @throws IOException if the file cannot be written
     */
    public void save(GameSnapshot snapshot) throws IOException {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot must not be null");
        }
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(snapshot));
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Reads the save file.
     *
     * @return the saved state, or an empty {@code Optional} if the file does not exist or is empty
     * @throws IOException if the file cannot be read, or is damaged and cannot be turned back into a snapshot
     */
    public Optional<GameSnapshot> load() throws IOException {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        String json = Files.readString(path);
        try {
            return Optional.ofNullable(GSON.fromJson(json, GameSnapshot.class));
        } catch (RuntimeException e) {
            // Gson throws an unchecked exception when the JSON is broken or does not match a snapshot.
            throw new IOException("The save file is damaged: " + path, e);
        }
    }
}