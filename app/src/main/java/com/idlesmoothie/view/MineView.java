package com.idlesmoothie.view;

import com.idlesmoothie.model.resource.Fruit;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;

/**
 * Draws the underground mine on a canvas and lets the player scroll it vertically.
 * <p>
 * The world is taller than the window: every element has a "world" y coordinate,
 * and is drawn at {@code worldY - scrollY}.
 */
public class MineView {

    private static final double SURFACE_HEIGHT = 400;
    private static final double FLOOR_HEIGHT = 220;
    private static final double SHAFT_WIDTH = 80;
    private static final double ROOM_MARGIN = 20;
    private static final int FLOORS_PER_FRUIT = 3;   // TODO read from BalanceConfig
    private static final Color SHAFT_COLOR = Color.web("#3B2A1E");
    private static final Color LOCKED_COLOR = Color.web("#7A7A7A");
    private static final Color[] SOIL_COLORS = {
        Color.web("#A8774A"),
        Color.web("#8B5E3C"),
        Color.web("#6E4A33"),
        Color.web("#55473F"),
        Color.web("#3A2F2F"),
    };

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Image surfaceImage;
    private final int maxFloors;
    private int unlockedFloors;   // TODO replace with Mine once model.station exists
    private double scrollY;

    /**
     * Creates the mine view.
     *
     * @param width          canvas width in pixels
     * @param height         canvas height in pixels
     * @param maxFloors      total number of floors in the mine
     * @param unlockedFloors number of floors already unlocked
     */
    public MineView(double width, double height, int maxFloors, int unlockedFloors) {
        this.canvas = new Canvas(width, height);
        this.gc = canvas.getGraphicsContext2D();
        this.maxFloors = maxFloors;
        this.unlockedFloors = unlockedFloors;
        canvas.setOnScroll(e -> onScroll(e.getDeltaY()));

        this.surfaceImage = new Image(getClass().getResourceAsStream("/images/mine/surface.png"));
    }

    /**
     * Redraws the whole visible part of the mine.
     */
    public void render() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawSurface();
        for (int index = 1; index <= maxFloors; index++) {
            drawSoil(index);
        }
        for (int index = 1; index <= unlockedFloors; index++) {
            drawFloor(index);
        }
        if (unlockedFloors < maxFloors) {
            drawLockedFloor(unlockedFloors + 1);
        }
        drawShaft();
    }

    /**
     * @return the canvas to add to the scene
     */
    public Canvas getCanvas() {
        return canvas;
    }

    private void onScroll(double deltaY) {
        scrollY = Math.clamp(scrollY - deltaY, 0, getMaxScroll());
        render();
    }

    private double getMaxScroll() {
        double worldHeight = SURFACE_HEIGHT + maxFloors * FLOOR_HEIGHT;
        return Math.max(0, worldHeight - canvas.getHeight());
    }

    private double floorY(int index) {
        return SURFACE_HEIGHT + (index - 1) * FLOOR_HEIGHT;
    }

    private void drawSurface() {
        gc.drawImage(surfaceImage, 0, -scrollY, canvas.getWidth(), SURFACE_HEIGHT);
    }

    private void drawSoil(int index) {
        int zone = (index - 1) / FLOORS_PER_FRUIT;
        gc.setFill(SOIL_COLORS[Math.min(zone, SOIL_COLORS.length - 1)]);
        gc.fillRect(0, floorY(index) - scrollY, canvas.getWidth(), FLOOR_HEIGHT);
    }

    private void drawFloor(int index) {
        double y = floorY(index) - scrollY;
        gc.setFill(Color.web(fruitOf(index).getColorHex()));
        gc.fillRect(getRoomX(), y + ROOM_MARGIN, getRoomWidth(), FLOOR_HEIGHT - 2 * ROOM_MARGIN);
        gc.setFill(Color.WHITE);
        gc.fillText("Floor " + index, getRoomX() + ROOM_MARGIN, y + 2 * ROOM_MARGIN);
    }

    private void drawLockedFloor(int index) {
        double y = floorY(index) - scrollY;
        gc.setFill(LOCKED_COLOR);
        gc.fillRect(getRoomX(), y + ROOM_MARGIN, getRoomWidth(), FLOOR_HEIGHT - 2 * ROOM_MARGIN);
        gc.setFill(Color.WHITE);
        gc.fillText("Unlock floor " + index, getRoomX() + ROOM_MARGIN, y + 2 * ROOM_MARGIN);
    }

    private void drawShaft() {
        double bottom = floorY(unlockedFloors) + FLOOR_HEIGHT;
        gc.setFill(SHAFT_COLOR);
        gc.fillRect(0, SURFACE_HEIGHT - scrollY, SHAFT_WIDTH, bottom - SURFACE_HEIGHT);
    }

    private double getRoomX() {
        return SHAFT_WIDTH + ROOM_MARGIN;
    }

    private double getRoomWidth() {
        return canvas.getWidth() - SHAFT_WIDTH - 2 * ROOM_MARGIN;
    }

    private Fruit fruitOf(int index) {
        // TODO temporary: will be replaced by Mine.fromFloor(index) (owned by the station package)
        return Fruit.values()[(index - 1) / FLOORS_PER_FRUIT];
    }
}
