package com.idlesmoothie.view;

import com.idlesmoothie.model.manager.Rarity;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

/**
 * The manager sprites, one per rarity, loaded once from the sheet made by {@code tools/sprites.py}.
 * <p>
 * The sheet is one row of cells, from COMMON to MYTHIC, each with the manager facing the player,
 * its feet on the same ground line and its body centered. If the sheet does not exist yet, or does not
 * hold one cell per rarity, no sprite is available and the callers keep their placeholder.
 * A second sheet, with a single cell, holds the ghost shown where no manager is hired yet.
 */
public final class ManagerSprites {

    /** Cell size, ground line and body x of the sheet, in sheet pixels; must match tools/sprites.py. */
    private static final double CELL_WIDTH = 256;
    private static final double CELL_HEIGHT = 256;
    private static final double CELL_GROUND_Y = 236;
    private static final double CELL_BODY_X = 128;
    private static final String PATH = "/images/managers/rarities.png";
    private static final String GHOST_PATH = "/images/managers/ghost.png";

    private final Map<Rarity, Image> sprites = new EnumMap<>(Rarity.class);
    private final Image ghost;
    private final double height;

    /**
     * Loads the sheet, scaled once to the size it is shown at.
     *
     * @param height height of one cell on screen, in pixels
     */
    public ManagerSprites(double height) {
        this.height = height;
        this.ghost = loadSheet(GHOST_PATH);
        Image sheet = loadSheet(PATH);
        Rarity[] rarities = Rarity.values();
        if (sheet == null || Math.round(sheet.getWidth() / getWidth()) != rarities.length) {
            return;
        }
        int frameWidth = (int) Math.round(sheet.getWidth() / rarities.length);
        int frameHeight = (int) Math.round(sheet.getHeight());
        for (int i = 0; i < rarities.length; i++) {
            sprites.put(rarities[i], new WritableImage(sheet.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight));
        }
    }

    /**
     * @return {@code true} if the sheet was loaded
     */
    public boolean isLoaded() {
        return !sprites.isEmpty();
    }

    /**
     * @return the most a manager rises above the ground on screen, hat included
     */
    public double getStandingHeight() {
        return CELL_GROUND_Y * height / CELL_HEIGHT;
    }

    /**
     * @return the sprite of the rarity, or {@code null} if the sheet was not loaded
     */
    public Image get(Rarity rarity) {
        return sprites.get(rarity);
    }

    /**
     * Draws the manager with its feet on {@code groundY} and its body centered on {@code bodyX}.
     * The squash stretches it from the feet: above 1 it is taller and thinner, below 1 shorter and wider.
     *
     * @param gc      where to draw
     * @param rarity  rarity of the manager; the sheet must be loaded
     * @param bodyX   screen x of the body center
     * @param groundY screen y of the ground under the feet
     * @param squash  vertical stretch, 1 for the sprite as drawn
     */
    public void draw(GraphicsContext gc, Rarity rarity, double bodyX, double groundY, double squash) {
        drawCell(gc, sprites.get(rarity), bodyX, groundY, squash, 1);
    }

    /**
     * @return {@code true} if the ghost of an empty manager slot was loaded
     */
    public boolean hasGhost() {
        return ghost != null;
    }

    /**
     * Draws the ghost of an empty manager slot, see-through, like {@link #draw}.
     *
     * @param gc      where to draw
     * @param bodyX   screen x of the body center
     * @param groundY screen y of the ground under the feet
     * @param squash  vertical stretch, 1 for the sprite as drawn
     * @param opacity from 0 (invisible) to 1 (solid)
     */
    public void drawGhost(GraphicsContext gc, double bodyX, double groundY, double squash, double opacity) {
        drawCell(gc, ghost, bodyX, groundY, squash, opacity);
    }

    private void drawCell(GraphicsContext gc, Image image, double bodyX, double groundY, double squash,
            double opacity) {
        double scale = height / CELL_HEIGHT;
        gc.save();
        gc.setGlobalAlpha(opacity);
        gc.translate(bodyX, groundY);
        gc.scale(2 - squash, squash);
        gc.drawImage(image, -CELL_BODY_X * scale, -CELL_GROUND_Y * scale, getWidth(), height);
        gc.restore();
    }

    private double getWidth() {
        return CELL_WIDTH * height / CELL_HEIGHT;
    }

    /**
     * @return the sheet scaled to {@code height} pixels high, or {@code null} if the file does not exist yet
     */
    private Image loadSheet(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            return stream == null ? null : new Image(stream, 0, height, true, true);
        } catch (IOException e) {
            return null;
        }
    }
}
