package com.idlesmoothie.view;

import com.idlesmoothie.model.creature.CarryingState;
import com.idlesmoothie.model.creature.FillingState;
import com.idlesmoothie.model.creature.GoingToExtractorState;
import com.idlesmoothie.model.creature.Pulpie;
import com.idlesmoothie.model.creature.PulpieState;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.Storage;
import com.idlesmoothie.model.station.Elevator;
import com.idlesmoothie.model.station.Floor;
import java.io.InputStream;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Draws the underground mine on a canvas and lets the player scroll it vertically.
 * <p>
 * The world is taller than the window: every element has a "world" y coordinate,
 * and is drawn at {@code worldY - scrollY}.
 * Clicking a floor sends its Pulpies on a trip; clicking the shaft calls the elevator.
 */
public class MineView implements Renderable {

    private static final double SURFACE_HEIGHT = 400;
    private static final double FLOOR_HEIGHT = 220;
    private static final double SHAFT_WIDTH = 80;
    private static final double ROOM_MARGIN = 20;
    private static final double BOX_SIZE = 40;
    private static final double PULPIE_SIZE = 24;
    private static final double CABIN_MARGIN = 8;
    private static final double CABIN_HEIGHT = 60;
    private static final int FLOORS_PER_FRUIT = 3;   // TODO read from BalanceConfig
    private static final Color SHAFT_COLOR = Color.web("#3B2A1E");
    private static final Color LOCKED_COLOR = Color.web("#7A7A7A");
    private static final Color CRATE_COLOR = Color.web("#8B5A2B");
    private static final Color EXTRACTOR_COLOR = Color.web("#4D4D4D");
    private static final Color CABIN_COLOR = Color.web("#C9A227");
    private static final Color LABEL_BACKGROUND = Color.rgb(0, 0, 0, 0.6);
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
    private final Image[] soilImages;
    private final int maxFloors;
    private final List<Floor> floors;
    private final Elevator elevator;
    private final Storage surface;
    private double scrollY;

    /**
     * Creates the mine view.
     *
     * @param width     canvas width in pixels
     * @param height    canvas height in pixels
     * @param maxFloors total number of floors the mine can have, used for the world height
     * @param floors    unlocked floors, read on every frame
     * @param elevator  elevator drawn in the shaft
     * @param surface   surface stock where the elevator unloads
     */
    public MineView(double width, double height, int maxFloors, List<Floor> floors,
            Elevator elevator, Storage surface) {
        this.canvas = new Canvas(width, height);
        this.gc = canvas.getGraphicsContext2D();
        this.maxFloors = maxFloors;
        this.floors = floors;
        this.elevator = elevator;
        this.surface = surface;
        canvas.setOnScroll(e -> onScroll(e.getDeltaY()));
        canvas.setOnMouseClicked(e -> onClick(e.getX(), e.getY()));

        this.surfaceImage = new Image(getClass().getResourceAsStream("/images/mine/surface.png"));
        this.soilImages = new Image[Fruit.values().length];
        for (Fruit fruit : Fruit.values()) {
            soilImages[fruit.ordinal()] = loadImage("/images/mine/soil_" + fruit.name().toLowerCase() + ".png");
        }
    }

    /**
     * Redraws the whole visible part of the mine.
     */
    @Override
    public void render() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawSurface();
        for (Fruit fruit : Fruit.values()) {
            if (fruit.ordinal() * FLOORS_PER_FRUIT < maxFloors) {
                drawSoil(fruit);
            }
        }
        for (Floor floor : floors) {
            drawFloor(floor);
        }
        if (floors.size() < maxFloors) {
            drawLockedFloor(floors.size());
        }
        drawShaft();
        drawElevator();
        drawSurfaceStock();
    }

    /**
     * @return the canvas to add to the scene
     */
    public Canvas getCanvas() {
        return canvas;
    }

    private void onScroll(double deltaY) {
        scrollY = Math.clamp(scrollY - deltaY, 0, getMaxScroll());
    }

    private void onClick(double x, double screenY) {
        double worldY = screenY + scrollY;
        if (x < SHAFT_WIDTH) {
            elevator.start();
            return;
        }
        for (Floor floor : floors) {
            double top = floorY(floor.getIndex());
            if (worldY >= top && worldY < top + FLOOR_HEIGHT) {
                floor.start();
            }
        }
    }

    private double getMaxScroll() {
        double worldHeight = SURFACE_HEIGHT + maxFloors * FLOOR_HEIGHT;
        return Math.max(0, worldHeight - canvas.getHeight());
    }

    /**
     * @return the world y of the top of the floor, the first floor having index 0
     */
    private double floorY(int index) {
        return SURFACE_HEIGHT + index * FLOOR_HEIGHT;
    }

    private void drawSurface() {
        gc.drawImage(surfaceImage, 0, -scrollY, canvas.getWidth(), SURFACE_HEIGHT);
    }

    private void drawSoil(Fruit fruit) {
        double y = floorY(fruit.ordinal() * FLOORS_PER_FRUIT) - scrollY;
        double height = FLOORS_PER_FRUIT * FLOOR_HEIGHT;
        Image image = soilImages[fruit.ordinal()];
        if (image != null) {
            gc.drawImage(image, 0, y, canvas.getWidth(), height);
        } else {
            gc.setFill(SOIL_COLORS[fruit.ordinal()]);
            gc.fillRect(0, y, canvas.getWidth(), height);
        }
    }

    private void drawFloor(Floor floor) {
        double y = floorY(floor.getIndex()) - scrollY;
        gc.setFill(Color.web(floor.getFruit().getColorHex()));
        gc.fillRect(getRoomX(), y + ROOM_MARGIN, getRoomWidth(), FLOOR_HEIGHT - 2 * ROOM_MARGIN);
        gc.setFill(Color.WHITE);
        gc.fillText("Floor " + (floor.getIndex() + 1), getRoomX() + ROOM_MARGIN, y + 2 * ROOM_MARGIN);

        double boxY = getGroundY(y) - BOX_SIZE;
        gc.setFill(CRATE_COLOR);
        gc.fillRect(getCrateX(), boxY, BOX_SIZE, BOX_SIZE);
        gc.setFill(EXTRACTOR_COLOR);
        gc.fillRect(getExtractorX(), boxY, BOX_SIZE, BOX_SIZE);
        drawLabel(String.format("%.1f", floor.getStoredAmount()), getCrateX(), boxY - ROOM_MARGIN);

        for (Pulpie pulpie : floor.getPulpies()) {
            drawPulpie(pulpie, y);
        }
    }

    private void drawPulpie(Pulpie pulpie, double floorScreenY) {
        double start = getCrateX() + BOX_SIZE;
        double end = getExtractorX() - PULPIE_SIZE;
        PulpieState state = pulpie.getState();
        double x = start;
        if (state instanceof GoingToExtractorState) {
            x = start + (end - start) * pulpie.getProgress();
        } else if (state instanceof FillingState) {
            x = end;
        } else if (state instanceof CarryingState) {
            x = end - (end - start) * pulpie.getProgress();
        }
        Color color = Color.WHITE;
        if (pulpie.getBag() != null) {
            color = color.darker();
        }
        gc.setFill(color);
        gc.fillRect(x, getGroundY(floorScreenY) - PULPIE_SIZE, PULPIE_SIZE, PULPIE_SIZE);
    }

    private void drawLockedFloor(int index) {
        double y = floorY(index) - scrollY;
        gc.setFill(LOCKED_COLOR);
        gc.fillRect(getRoomX(), y + ROOM_MARGIN, getRoomWidth(), FLOOR_HEIGHT - 2 * ROOM_MARGIN);
        gc.setFill(Color.WHITE);
        gc.fillText("Unlock floor " + (index + 1), getRoomX() + ROOM_MARGIN, y + 2 * ROOM_MARGIN);
    }

    private void drawShaft() {
        double bottom = floorY(floors.size());
        gc.setFill(SHAFT_COLOR);
        gc.fillRect(0, SURFACE_HEIGHT - scrollY, SHAFT_WIDTH, bottom - SURFACE_HEIGHT);
    }

    /**
     * Draws the cabin so that its bottom matches the bottom of the floor it is at:
     * at position 0 it stands on the surface ground.
     */
    private void drawElevator() {
        double y = SURFACE_HEIGHT + elevator.getPosition() * FLOOR_HEIGHT - CABIN_HEIGHT - scrollY;
        gc.setFill(CABIN_COLOR);
        gc.fillRect(CABIN_MARGIN, y, SHAFT_WIDTH - 2 * CABIN_MARGIN, CABIN_HEIGHT);
        gc.setFill(Color.BLACK);
        gc.fillText(String.format("%.1f", elevator.getCabinAmount()), 2 * CABIN_MARGIN, y + CABIN_HEIGHT / 2);
    }

    private void drawSurfaceStock() {
        double y = SURFACE_HEIGHT - ROOM_MARGIN - scrollY;
        drawLabel(String.format("Stock: %.1f", surface.getAmount()), getRoomX(), y);
    }

    /**
     * Writes white text on a dark box so it stays readable over any background.
     */
    private void drawLabel(String text, double x, double y) {
        gc.setFill(LABEL_BACKGROUND);
        gc.fillRect(x - 4, y - 14, text.length() * 7 + 8, 20);
        gc.setFill(Color.WHITE);
        gc.fillText(text, x, y);
    }

    private double getGroundY(double floorScreenY) {
        return floorScreenY + FLOOR_HEIGHT - ROOM_MARGIN;
    }

    private double getRoomX() {
        return SHAFT_WIDTH + ROOM_MARGIN;
    }

    private double getRoomWidth() {
        return canvas.getWidth() - SHAFT_WIDTH - 2 * ROOM_MARGIN;
    }

    private double getCrateX() {
        return getRoomX() + ROOM_MARGIN;
    }

    private double getExtractorX() {
        return getRoomX() + getRoomWidth() - ROOM_MARGIN - BOX_SIZE;
    }

    /**
     * Loads an image from the classpath.
     *
     * @return the image, or null if the file does not exist yet (the caller draws a placeholder)
     */
    private Image loadImage(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        return stream == null ? null : new Image(stream);
    }
}
