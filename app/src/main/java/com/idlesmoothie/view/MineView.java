package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.creature.CarryingState;
import com.idlesmoothie.model.creature.FillingState;
import com.idlesmoothie.model.creature.GoingToExtractorState;
import com.idlesmoothie.model.creature.Pulpie;
import com.idlesmoothie.model.creature.PulpieState;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.station.Elevator;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Mine;
import com.idlesmoothie.model.station.SellStation;
import java.io.InputStream;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Draws the underground mine on a canvas and lets the player scroll it vertically.
 * <p>
 * The world is taller than the window: every element has a "world" y coordinate,
 * and is drawn at {@code worldY - scrollY}.
 * Clicking a floor sends its Pulpies on a trip, clicking the shaft calls the elevator
 * and clicking the bar on the surface sells the surface stock.
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
    private static final double BAR_SIZE = 100;
    private static final Color SHAFT_COLOR = Color.web("#3B2A1E");
    private static final Color LOCKED_COLOR = Color.web("#7A7A7A");
    private static final Color BUYABLE_COLOR = Color.web("#3E8E41");
    private static final Color CRATE_COLOR = Color.web("#8B5A2B");
    private static final Color EXTRACTOR_COLOR = Color.web("#4D4D4D");
    private static final Color CABIN_COLOR = Color.web("#C9A227");
    private static final Color BAR_COLOR = Color.web("#E85D9A");
    private static final Color PROGRESS_COLOR = Color.web("#6CC644");
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
    private final Game game;
    private final Mine mine;
    private double scrollY;

    /**
     * Creates the mine view.
     *
     * @param width  canvas width in pixels
     * @param height canvas height in pixels
     * @param game   the game: read to draw the mine, and told about the player's clicks
     */
    public MineView(double width, double height, Game game) {
        this.canvas = new Canvas(width, height);
        this.gc = canvas.getGraphicsContext2D();
        this.game = game;
        this.mine = game.getMine();
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
        drawSoilZones();
        for (Floor floor : mine.getFloors()) {
            drawFloor(floor);
        }
        if (mine.hasNextFloor()) {
            drawLockedFloor(mine.getFloors().size());
        }
        drawShaft();
        drawElevator();
        drawSurfaceStock();
        drawSellStation();
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
        if (x >= getBarX() && x < getBarX() + BAR_SIZE
                && worldY >= getBarWorldY() && worldY < getBarWorldY() + BAR_SIZE) {
            game.tapSellStation();
            return;
        }
        if (x < SHAFT_WIDTH) {
            game.tapElevator();
            return;
        }
        for (Floor floor : mine.getFloors()) {
            double top = floorY(floor.getIndex());
            if (worldY >= top && worldY < top + FLOOR_HEIGHT) {
                game.tapFloor(floor.getIndex());
            }
        }
        double lockedTop = floorY(mine.getFloors().size());
        if (mine.hasNextFloor() && worldY >= lockedTop && worldY < lockedTop + FLOOR_HEIGHT) {
            game.buyNextFloor();
        }
    }

    private double getMaxScroll() {
        double worldHeight = SURFACE_HEIGHT + mine.getMaxFloors() * FLOOR_HEIGHT;
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

    /**
     * Draws one soil texture per fruit zone, a zone being the floors that share the same fruit.
     */
    private void drawSoilZones() {
        int start = 0;
        for (int index = 1; index <= mine.getMaxFloors(); index++) {
            if (index == mine.getMaxFloors() || mine.fruitForFloor(index) != mine.fruitForFloor(start)) {
                drawSoil(mine.fruitForFloor(start), start, index - start);
                start = index;
            }
        }
    }

    private void drawSoil(Fruit fruit, int firstIndex, int floorCount) {
        double y = floorY(firstIndex) - scrollY;
        double height = floorCount * FLOOR_HEIGHT;
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

    /**
     * Draws the next floor to buy, highlighted when the player can buy it.
     * Behind a barrier, it says that the deepest floor must be upgraded first.
     */
    private void drawLockedFloor(int index) {
        double y = floorY(index) - scrollY;
        gc.setFill(game.canBuyNextFloor() ? BUYABLE_COLOR : LOCKED_COLOR);
        gc.fillRect(getRoomX(), y + ROOM_MARGIN, getRoomWidth(), FLOOR_HEIGHT - 2 * ROOM_MARGIN);
        gc.setFill(Color.WHITE);
        gc.fillText("Unlock floor " + (index + 1), getRoomX() + ROOM_MARGIN, y + 2 * ROOM_MARGIN);
        String detail = mine.canUnlockNextFloor()
                ? String.format("Cost: %.0f coins", mine.getNextFloorCost())
                : "Barrier: upgrade floor " + index + " first";
        gc.fillText(detail, getRoomX() + ROOM_MARGIN, y + 3 * ROOM_MARGIN);
    }

    private void drawShaft() {
        double bottom = floorY(mine.getFloors().size());
        gc.setFill(SHAFT_COLOR);
        gc.fillRect(0, SURFACE_HEIGHT - scrollY, SHAFT_WIDTH, bottom - SURFACE_HEIGHT);
    }

    /**
     * Draws the cabin so that its bottom matches the bottom of the floor it is at:
     * at position 0 it stands on the surface ground.
     */
    private void drawElevator() {
        Elevator elevator = mine.getElevator();
        double y = SURFACE_HEIGHT + elevator.getPosition() * FLOOR_HEIGHT - CABIN_HEIGHT - scrollY;
        gc.setFill(CABIN_COLOR);
        gc.fillRect(CABIN_MARGIN, y, SHAFT_WIDTH - 2 * CABIN_MARGIN, CABIN_HEIGHT);
        gc.setFill(Color.BLACK);
        gc.fillText(String.format("%.1f", elevator.getCabinAmount()), 2 * CABIN_MARGIN, y + CABIN_HEIGHT / 2);
    }

    private void drawSurfaceStock() {
        double y = SURFACE_HEIGHT - ROOM_MARGIN - scrollY;
        drawLabel(String.format("Stock: %.1f", mine.getSurfaceAmount()), getRoomX(), y);
    }

    /**
     * Draws the Smoothie Bar standing on the surface ground, with the quantity being sold
     * and a progress bar for the current sale.
     */
    private void drawSellStation() {
        SellStation sellStation = mine.getSellStation();
        double y = getBarWorldY() - scrollY;
        gc.setFill(BAR_COLOR);
        gc.fillRect(getBarX(), y, BAR_SIZE, BAR_SIZE);
        gc.setFill(PROGRESS_COLOR);
        gc.fillRect(getBarX(), y + BAR_SIZE - CABIN_MARGIN, BAR_SIZE * sellStation.getProgress(), CABIN_MARGIN);
        gc.setFill(Color.WHITE);
        gc.fillText("BAR", getBarX() + CABIN_MARGIN, y + 2 * CABIN_MARGIN);
        drawLabel(String.format("%.1f", sellStation.getOnSaleAmount()), getBarX(), y - CABIN_MARGIN);
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

    private double getBarX() {
        return canvas.getWidth() - BAR_SIZE;
    }

    private double getBarWorldY() {
        return SURFACE_HEIGHT - BAR_SIZE;
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
