package com.idlesmoothie.view;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * The screen shown while the game loads: the loading art, a smoothie progress bar in the slot painted in it,
 * a strawberry Pulpie walking on the bar, and a line naming the current step.
 * <p>
 * The bar eases toward the real progress and never fills in less than {@value #MIN_SECONDS} seconds, so the
 * screen does not flash. It only draws: the loading itself is done by the caller, which reports the progress,
 * the step names, the end of the loading or an error.
 */
public final class LoadingScreen {

    private static final double WIDTH = 540;
    private static final double HEIGHT = 960;
    private static final String BACKGROUND = "/images/loading/background.jpg";
    private static final String WALK = "/images/pulpies/strawberry/walk.png";
    private static final String FONT = "/fonts/LilitaOne-Regular.ttf";

    // The empty slot painted in the background, in window pixels, and the bar drawn inside it.
    private static final double SLOT_LEFT = 132;
    private static final double SLOT_RIGHT = 408;
    private static final double SLOT_TOP = 920;
    private static final double SLOT_BOTTOM = 940;
    private static final double BAR_INSET = 3;
    private static final Color BAR_COLOR = Color.web("#E85D9A");
    private static final Color BAR_HIGHLIGHT = Color.web("#F7B3D1", 0.8);
    private static final double HIGHLIGHT_HEIGHT = 0.35;

    // The walking Pulpie, drawn like the game's: a 256px cell, feet at y 236, body at x 120.
    private static final double PULPIE_SIZE = 64;
    private static final double CELL = 256;
    private static final double CELL_GROUND_Y = 236;
    private static final double CELL_BODY_X = 120;
    private static final double WALK_FRAME_SECONDS = 0.1;

    private static final double TEXT_SIZE = 24;
    private static final double DETAIL_SIZE = 16;
    private static final double TEXT_Y = 860;
    private static final double TEXT_MAX_WIDTH = 460;
    private static final double LINE_GAP = 4;
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color OUTLINE_COLOR = Color.web("#2A1A1F");
    private static final double OUTLINE_WIDTH = 5;

    private static final double MIN_SECONDS = 2;
    // How fast the bar catches up with the real progress: it covers about 1 - e^-5 of the gap per second.
    private static final double EASE_RATE = 5;
    private static final double FULL = 0.995;
    private static final double NANOS_PER_SECOND = 1e9;

    private final Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private final Image background = Assets.image(BACKGROUND);
    private final Image[] walk = loadWalk();
    private final Font font = loadFont(TEXT_SIZE);
    private final Font detailFont = loadFont(DETAIL_SIZE);
    private final AnimationTimer timer;

    private double progress;
    private double shown;
    private String message = "";
    private String error;
    private String errorDetail;
    private Runnable whenFull;
    private long startNanos;
    private long lastNanos;

    /**
     * Creates the screen at 0%. Call {@link #start()} to animate it.
     */
    public LoadingScreen() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tick(now);
            }
        };
        draw(0, 0);
    }

    /**
     * @return the canvas to put in the scene, as big as the window
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * Starts animating the bar and the Pulpie.
     */
    public void start() {
        timer.start();
    }

    /**
     * Stops the animation, once the screen is gone.
     */
    public void stop() {
        timer.stop();
    }

    /**
     * @param progress the real progress of the loading, from 0 to 1; the bar eases toward it
     */
    public void setProgress(double progress) {
        this.progress = Math.clamp(progress, 0, 1);
    }

    /**
     * @param message the step being loaded, such as {@code "Waking up the Pulpies..."}
     */
    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    /**
     * Tells that the loading is over. The action runs once, on the JavaFX thread, when the bar is full and the
     * screen has been shown for at least {@value #MIN_SECONDS} seconds.
     *
     * @param whenFull what to do then, such as fading into the game
     */
    public void finish(Runnable whenFull) {
        this.progress = 1;
        this.whenFull = whenFull;
    }

    /**
     * Shows an error instead of the step, and stops the bar where it is.
     *
     * @param title  what failed, such as {@code "Could not open your save"}
     * @param detail why, or {@code null}
     */
    public void showError(String title, String detail) {
        this.error = title;
        this.errorDetail = detail;
    }

    private void tick(long now) {
        if (startNanos == 0) {
            startNanos = now;
            lastNanos = now;
        }
        double seconds = (now - startNanos) / NANOS_PER_SECOND;
        double dt = (now - lastNanos) / NANOS_PER_SECOND;
        lastNanos = now;
        if (error == null) {
            double target = Math.min(progress, seconds / MIN_SECONDS);
            shown += (target - shown) * (1 - Math.exp(-EASE_RATE * dt));
            if (whenFull != null && seconds >= MIN_SECONDS && shown >= FULL) {
                shown = 1;
                Runnable action = whenFull;
                whenFull = null;
                action.run();
            }
        }
        draw(shown, seconds);
    }

    /**
     * Draws the whole screen.
     *
     * @param barProgress how full the bar is, from 0 to 1
     * @param seconds     time since the screen started, which drives the walk
     */
    private void draw(double barProgress, double seconds) {
        gc.clearRect(0, 0, WIDTH, HEIGHT);
        if (background != null) {
            gc.drawImage(background, 0, 0, WIDTH, HEIGHT);
        } else {
            gc.setFill(OUTLINE_COLOR);
            gc.fillRect(0, 0, WIDTH, HEIGHT);
        }
        double barLeft = SLOT_LEFT + BAR_INSET;
        double barWidth = SLOT_RIGHT - SLOT_LEFT - 2 * BAR_INSET;
        double fill = barWidth * barProgress;
        drawBar(barLeft, fill);
        int frame = error == null ? (int) (seconds / WALK_FRAME_SECONDS) : 0;
        drawPulpie(barLeft + fill, frame);
        if (error == null) {
            drawLines(List.of(message), font, TEXT_Y);
        } else {
            double y = TEXT_Y;
            if (errorDetail != null) {
                List<String> detail = wrap(errorDetail, detailFont);
                drawLines(detail, detailFont, y);
                y -= detail.size() * (DETAIL_SIZE + LINE_GAP) + LINE_GAP;
            }
            drawLines(wrap(error, font), font, y);
        }
    }

    /**
     * Draws the pink fill with rounded ends and a lighter band along its top, inside the slot.
     */
    private void drawBar(double left, double fill) {
        double top = SLOT_TOP + BAR_INSET;
        double height = SLOT_BOTTOM - SLOT_TOP - 2 * BAR_INSET;
        if (fill <= 0) {
            return;
        }
        gc.setFill(BAR_COLOR);
        gc.fillRoundRect(left, top, fill, height, Math.min(height, fill), height);
        double highlight = height * HIGHLIGHT_HEIGHT;
        if (fill > height) {
            gc.setFill(BAR_HIGHLIGHT);
            gc.fillRoundRect(left + height / 4, top + 1, fill - height / 2, highlight, highlight, highlight);
        }
    }

    /**
     * Draws the Pulpie standing on top of the bar, its body at the front of the fill.
     */
    private void drawPulpie(double bodyX, int frame) {
        if (walk.length == 0) {
            return;
        }
        double scale = PULPIE_SIZE / CELL;
        gc.drawImage(walk[frame % walk.length], bodyX - CELL_BODY_X * scale, SLOT_TOP - CELL_GROUND_Y * scale,
                PULPIE_SIZE, PULPIE_SIZE);
    }

    /**
     * Draws centered lines in white with a dark outline, the last one on {@code bottomY}, the others above it.
     */
    private void drawLines(List<String> lines, Font lineFont, double bottomY) {
        gc.setFont(lineFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.BOTTOM);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineWidth(OUTLINE_WIDTH);
        double lineHeight = lineFont.getSize() + LINE_GAP;
        for (int i = 0; i < lines.size(); i++) {
            double y = bottomY - (lines.size() - 1 - i) * lineHeight;
            gc.setStroke(OUTLINE_COLOR);
            gc.strokeText(lines.get(i), WIDTH / 2, y);
            gc.setFill(TEXT_COLOR);
            gc.fillText(lines.get(i), WIDTH / 2, y);
        }
    }

    /**
     * Splits a text into lines that fit the screen, between words, cutting a word too long for one line
     * (such as a file path).
     */
    private static List<String> wrap(String text, Font lineFont) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : splitLongWords(text.split("\\s+"), lineFont)) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && width(candidate, lineFont) > TEXT_MAX_WIDTH) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static List<String> splitLongWords(String[] words, Font lineFont) {
        List<String> pieces = new ArrayList<>();
        for (String word : words) {
            String rest = word;
            while (width(rest, lineFont) > TEXT_MAX_WIDTH) {
                int end = rest.length() - 1;
                while (end > 1 && width(rest.substring(0, end), lineFont) > TEXT_MAX_WIDTH) {
                    end--;
                }
                pieces.add(rest.substring(0, end));
                rest = rest.substring(end);
            }
            pieces.add(rest);
        }
        return pieces;
    }

    private static double width(String text, Font lineFont) {
        Text node = new Text(text);
        node.setFont(lineFont);
        return node.getLayoutBounds().getWidth();
    }

    /**
     * @return the frames of the strawberry walk, loaded twice as big as drawn so they stay sharp on dense screens
     */
    private static Image[] loadWalk() {
        Image sheet = Assets.image(WALK, 2 * PULPIE_SIZE);
        if (sheet == null) {
            return new Image[0];
        }
        int size = (int) Math.round(sheet.getHeight());
        Image[] frames = new Image[(int) Math.round(sheet.getWidth() / sheet.getHeight())];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new WritableImage(sheet.getPixelReader(), i * size, 0, size, size);
        }
        return frames;
    }

    private static Font loadFont(double size) {
        Font loaded = Assets.font(FONT, size);
        return loaded != null ? loaded : Font.font("System", FontWeight.BOLD, size);
    }
}
