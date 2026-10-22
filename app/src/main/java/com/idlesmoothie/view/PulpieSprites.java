package com.idlesmoothie.view;

import com.idlesmoothie.model.resource.Fruit;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

/**
 * The animated sprite sheets of the Pulpies, one per fruit and animation, loaded once.
 * <p>
 * The sheets are made by {@code tools/sprites.py}: one row of square cells, the Pulpie facing right,
 * with its feet on the same ground line and its body at the same x in every cell.
 * A sheet that does not exist yet is just missing, and the caller keeps its placeholder drawing.
 */
public final class PulpieSprites {

    /** Cell size, ground line and body x of the sheets, in sheet pixels; must match tools/sprites.py. */
    private static final double CELL = 256;
    private static final double CELL_GROUND_Y = 236;
    private static final double CELL_BODY_X = 120;

    /**
     * What a Pulpie can be doing, one sheet each.
     */
    public enum Animation {
        WALK,
        FILL,
        CARRY,
        DEPOSIT,
        IDLE
    }

    private final Map<Fruit, Map<Animation, Image[]>> sheets = new EnumMap<>(Fruit.class);
    private final double size;

    /**
     * Loads every sheet that exists, scaled once to the size it is drawn at.
     *
     * @param size width and height of one frame on screen, in pixels
     */
    public PulpieSprites(double size) {
        this.size = size;
        for (Fruit fruit : Fruit.values()) {
            Map<Animation, Image[]> fruitSheets = new EnumMap<>(Animation.class);
            for (Animation animation : Animation.values()) {
                Image sheet = loadSheet("/images/pulpies/" + fruit.name().toLowerCase() + "/"
                        + animation.name().toLowerCase() + ".png");
                if (sheet != null) {
                    fruitSheets.put(animation, splitFrames(sheet));
                }
            }
            sheets.put(fruit, fruitSheets);
        }
    }

    /**
     * @return {@code true} if the sheet of this animation exists for this fruit
     */
    public boolean has(Fruit fruit, Animation animation) {
        return sheets.get(fruit).containsKey(animation);
    }

    /**
     * @return the number of frames of the sheet, which must exist
     */
    public int getFrameCount(Fruit fruit, Animation animation) {
        return sheets.get(fruit).get(animation).length;
    }

    /**
     * Draws one frame with the feet on {@code groundY} and the body centered on {@code bodyX}.
     *
     * @param gc         where to draw
     * @param fruit      fruit of the Pulpie
     * @param animation  the animation, whose sheet must exist
     * @param frame      index of the frame, from 0
     * @param bodyX      screen x of the body center
     * @param groundY    screen y of the ground under the feet
     * @param facingLeft {@code true} to mirror the sprite, which faces right in the sheet
     */
    public void draw(GraphicsContext gc, Fruit fruit, Animation animation, int frame,
            double bodyX, double groundY, boolean facingLeft) {
        Image image = sheets.get(fruit).get(animation)[frame];
        double scale = size / CELL;
        double top = groundY - CELL_GROUND_Y * scale;
        gc.save();
        gc.translate(bodyX, 0);
        if (facingLeft) {
            gc.scale(-1, 1);
        }
        gc.drawImage(image, -CELL_BODY_X * scale, top, size, size);
        gc.restore();
    }

    /**
     * Copies each frame of the row into its own image, so the smoothing at the edge of a frame
     * never picks up pixels of the next one.
     */
    private Image[] splitFrames(Image sheet) {
        int frameSize = (int) Math.round(sheet.getHeight());
        Image[] frames = new Image[(int) Math.round(sheet.getWidth() / sheet.getHeight())];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new WritableImage(sheet.getPixelReader(), i * frameSize, 0, frameSize, frameSize);
        }
        return frames;
    }

    /**
     * @return the sheet scaled to {@code size} pixels high, or {@code null} if the file does not exist yet
     */
    private Image loadSheet(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            return stream == null ? null : new Image(stream, 0, size, true, true);
        } catch (IOException e) {
            return null;
        }
    }
}
