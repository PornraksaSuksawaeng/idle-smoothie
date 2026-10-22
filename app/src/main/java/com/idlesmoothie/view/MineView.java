package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.creature.CarryingState;
import com.idlesmoothie.model.creature.FillingState;
import com.idlesmoothie.model.creature.GoingToExtractorState;
import com.idlesmoothie.model.creature.Pulpie;
import com.idlesmoothie.model.creature.PulpieState;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.station.Elevator;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Mine;
import com.idlesmoothie.model.station.SellStation;
import com.idlesmoothie.model.station.Station;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Draws the underground mine on a canvas and lets the player scroll it vertically.
 * <p>
 * The world is taller than the window: every element has a "world" y coordinate,
 * and is drawn at {@code worldY - scrollY}.
 * Clicking a floor sends its Pulpies on a trip, clicking the shaft calls the elevator
 * and clicking the bar on the surface sells the surface stock.
 * Every station has a small "Lv" button that opens its upgrade popup
 * and a "Mgr" button that opens its manager shop.
 * A hired manager stands next to its station, in the color of its rarity.
 */
public class MineView implements Renderable {

    private static final double SURFACE_HEIGHT = 400;
    private static final double TOP_DIRT_HEIGHT = 167;
    // Measured on the room images, in image pixels: the room floor is at y = 245, the dirt ends at y = 385
    // where the next floor starts, and its jagged edge goes down to y = 440.
    private static final double ROOM_IMAGE_WIDTH = 920;
    private static final double ROOM_IMAGE_HEIGHT = 520;
    private static final double ROOM_IMAGE_GROUND = 245;
    private static final double ROOM_IMAGE_FLOOR_END = 385;
    private static final double EDGE_HEIGHT = 20;
    private static final double GRASS_HEIGHT = 16;
    private static final double GROUND_RAISE = 7;
    private static final double BARRIER_HEIGHT = 104;
    private static final double SHAFT_WIDTH = 80;
    private static final double ROOM_MARGIN = 20;
    private static final double BOX_SIZE = 40;
    private static final double PULPIE_SIZE = 24;
    private static final double MANAGER_WIDTH = 24;
    private static final double MANAGER_HEIGHT = 48;
    private static final double MANAGER_GAP = 2;
    private static final double CABIN_MARGIN = 8;
    private static final double CABIN_HEIGHT = 60;
    private static final double BAR_SIZE = 100;
    private static final double LEVEL_WIDTH = 64;
    private static final double LEVEL_HEIGHT = 24;
    private static final double COIN_SIZE = 14;
    private static final double COIN_GAP = 4;
    private static final Color SHAFT_COLOR = Color.web("#3B2A1E");
    private static final Color LOCKED_COLOR = Color.web("#7A7A7A");
    private static final Color BUYABLE_COLOR = Color.web("#3E8E41");
    private static final Color CRATE_COLOR = Color.web("#8B5A2B");
    private static final Color EXTRACTOR_COLOR = Color.web("#4D4D4D");
    private static final Color CABIN_COLOR = Color.web("#C9A227");
    private static final Color BAR_COLOR = Color.web("#E85D9A");
    private static final Color PROGRESS_COLOR = Color.web("#6CC644");
    private static final Color LABEL_BACKGROUND = Color.rgb(0, 0, 0, 0.6);
    private static final Color GRASS_COLOR = Color.web("#5DAA3C");
    private static final Color TOP_DIRT_COLOR = Color.web("#B5845A");
    private static final Color BARRIER_COLOR = Color.web("#6B6B6B");
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
    private final Image[] roomImages;
    private final Image groundImage;
    private final Image barrierImage;
    private final Game game;
    private final Mine mine;
    private final UpgradeController controller;
    private final UpgradePanel upgradePanel;
    private final ShopView shopView;
    // The room images fill the canvas width, their left part hidden under the shaft,
    // so the floor sizes follow the canvas width.
    private final double floorHeight;
    private final double roomHeight;
    private final double roomImageHeight;
    private double scrollY;

    /**
     * Creates the mine view.
     *
     * @param width  canvas width in pixels
     * @param height canvas height in pixels
     * @param game         the game read to draw the mine
     * @param controller   receives the player's clicks
     * @param upgradePanel popup opened by the "Lv" buttons
     * @param shopView     popup opened by the "Mgr" buttons
     */
    public MineView(double width, double height, Game game, UpgradeController controller,
            UpgradePanel upgradePanel, ShopView shopView) {
        this.canvas = new Canvas(width, height);
        double roomScale = width / ROOM_IMAGE_WIDTH;
        this.floorHeight = ROOM_IMAGE_FLOOR_END * roomScale;
        this.roomHeight = ROOM_IMAGE_GROUND * roomScale;
        this.roomImageHeight = ROOM_IMAGE_HEIGHT * roomScale;
        this.gc = canvas.getGraphicsContext2D();
        this.game = game;
        this.mine = game.getMine();
        this.controller = controller;
        this.upgradePanel = upgradePanel;
        this.shopView = shopView;
        canvas.setOnScroll(e -> onScroll(e.getDeltaY()));
        canvas.setOnMouseClicked(e -> onClick(e.getX(), e.getY()));

        this.surfaceImage = new Image(getClass().getResourceAsStream("/images/mine/surface.png"));
        this.soilImages = loadFruitImages("soil_");
        this.roomImages = loadFruitImages("room_");
        this.groundImage = loadImage("/images/mine/ground.png");
        this.barrierImage = loadImage("/images/mine/barrier.png");
    }

    /**
     * Redraws the whole visible part of the mine.
     */
    @Override
    public void render() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawSurface();
        drawSoilZones();
        if (mine.hasNextFloor()) {
            drawLockedFloor(mine.getFloors().size());
        }
        List<Floor> floors = mine.getFloors();
        for (int i = floors.size() - 1; i >= 0; i--) {
            drawFloor(floors.get(i));
        }
        drawGround();
        for (int i = floors.size(); i < mine.getMaxFloors(); i++) {
            if (mine.isBehindBarrier(i)) {
                drawBarrier(i);
            }
        }
        drawShaft();
        drawElevator();
        drawSurfaceStock();
        drawSellStation();
        for (Station station : getStations()) {
            drawManager(station);
            drawLevelButton(station);
            drawManagerButton(station);
        }
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

    /**
     * Finds what was clicked, the "Lv" buttons first since they lie on top of the stations.
     */
    private void onClick(double x, double screenY) {
        double worldY = screenY + scrollY;
        for (Station station : getStations()) {
            if (getLevelButton(station).contains(x, worldY)) {
                upgradePanel.show(station);
                return;
            }
            if (getManagerButton(station).contains(x, worldY)) {
                shopView.show(station);
                return;
            }
        }
        if (getBar().contains(x, worldY)) {
            controller.onTap(mine.getSellStation());
            return;
        }
        if (x < SHAFT_WIDTH) {
            controller.onTap(mine.getElevator());
            return;
        }
        for (Floor floor : mine.getFloors()) {
            double top = floorY(floor.getIndex());
            if (worldY >= top && worldY < top + floorHeight) {
                controller.onTap(floor);
            }
        }
        double lockedTop = floorY(mine.getFloors().size());
        if (mine.hasNextFloor() && worldY >= lockedTop && worldY < lockedTop + floorHeight) {
            controller.onUnlockFloor();
        }
    }

    private List<Station> getStations() {
        List<Station> stations = new ArrayList<>(mine.getFloors());
        stations.add(mine.getElevator());
        stations.add(mine.getSellStation());
        return stations;
    }

    private double getMaxScroll() {
        return Math.max(0, floorY(mine.getMaxFloors()) - canvas.getHeight());
    }

    /**
     * A floor is a room with its own dirt below it; its jagged bottom edge covers the top of the next room.
     *
     * @return the world y of the top of the floor, the first floor having index 0
     */
    private double floorY(int index) {
        return SURFACE_HEIGHT + TOP_DIRT_HEIGHT + index * floorHeight;
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
        double height = floorCount * floorHeight;
        Image image = soilImages[fruit.ordinal()];
        if (image != null) {
            gc.drawImage(image, 0, y, canvas.getWidth(), height);
        } else {
            gc.setFill(SOIL_COLORS[fruit.ordinal()]);
            gc.fillRect(0, y, canvas.getWidth(), height);
        }
    }

    /**
     * Draws the grass, the pebbles and the dirt between the surface and the first floor.
     * Its jagged bottom edge covers the top of the first floor.
     * The image is raised so that the transparent space between its grass blades shows the surface.
     */
    private void drawGround() {
        double y = SURFACE_HEIGHT - scrollY;
        if (groundImage != null) {
            gc.drawImage(groundImage, 0, y - GROUND_RAISE, canvas.getWidth(), TOP_DIRT_HEIGHT + EDGE_HEIGHT);
            return;
        }
        gc.setFill(TOP_DIRT_COLOR);
        gc.fillRect(0, y, canvas.getWidth(), TOP_DIRT_HEIGHT);
        gc.setFill(GRASS_COLOR);
        gc.fillRect(0, y, canvas.getWidth(), GRASS_HEIGHT);
        drawDirtEdge(TOP_DIRT_COLOR, 0, y + TOP_DIRT_HEIGHT, canvas.getWidth());
    }

    /**
     * Draws the room (top part) and its dirt (bottom part), from the shaft to the right edge.
     */
    private void drawFloor(Floor floor) {
        double y = floorY(floor.getIndex()) - scrollY;
        Fruit fruit = floor.getFruit();
        Image image = roomImages[fruit.ordinal()];
        if (image != null) {
            gc.drawImage(image, 0, y, canvas.getWidth(), roomImageHeight);
        } else {
            gc.setFill(Color.web(fruit.getColorHex()));
            gc.fillRect(getRoomX(), y, getRoomWidth(), roomHeight);
            gc.setFill(SOIL_COLORS[fruit.ordinal()]);
            gc.fillRect(getRoomX(), y + roomHeight, getRoomWidth(), floorHeight - roomHeight);
            drawDirtEdge(SOIL_COLORS[fruit.ordinal()], getRoomX(), y + floorHeight, getRoomWidth());
        }
        gc.setFill(Color.WHITE);
        gc.fillText("Floor " + (floor.getIndex() + 1), getCrateX(), y + EDGE_HEIGHT + ROOM_MARGIN);

        double boxY = getGroundY(y) - BOX_SIZE;
        gc.setFill(CRATE_COLOR);
        gc.fillRect(getCrateX(), boxY, BOX_SIZE, BOX_SIZE);
        if (image == null) {
            gc.setFill(EXTRACTOR_COLOR);
            gc.fillRect(getExtractorX(), boxY, BOX_SIZE, BOX_SIZE);
        }
        drawLabel(NumberFormatter.format(floor.getStoredAmount()), getCrateX(), boxY - ROOM_MARGIN);

        for (Pulpie pulpie : floor.getPulpies()) {
            drawPulpie(pulpie, y);
        }
    }

    private void drawPulpie(Pulpie pulpie, double floorScreenY) {
        double start = getManagerX() + MANAGER_WIDTH + MANAGER_GAP;
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
        gc.fillRect(getRoomX(), y, getRoomWidth(), roomHeight);
        gc.setFill(Color.WHITE);
        double textY = y + roomHeight / 2;
        gc.fillText("Unlock floor " + (index + 1), getCrateX(), textY);
        double detailY = textY + ROOM_MARGIN;
        if (!mine.canUnlockNextFloor()) {
            gc.fillText("Barrier: upgrade floor " + index + " first", getCrateX(), detailY);
            return;
        }
        String cost = "Cost: " + NumberFormatter.format(mine.getNextFloorCost());
        gc.fillText(cost, getCrateX(), detailY);
        drawCoin(getCrateX() + textWidth(cost) + COIN_GAP, detailY);
    }

    /**
     * Draws the coin icon after a text, its bottom on the text baseline.
     */
    private void drawCoin(double x, double baselineY) {
        gc.drawImage(CoinIcon.IMAGE, x, baselineY - COIN_SIZE + 2, COIN_SIZE, COIN_SIZE);
    }

    private double textWidth(String text) {
        Text measured = new Text(text);
        measured.setFont(gc.getFont());
        return measured.getLayoutBounds().getWidth();
    }

    /**
     * Draws the barrier half over the junction between two fruit soils,
     * so that once it is gone the spacing between the rooms stays the same.
     */
    private void drawBarrier(int index) {
        double y = floorY(index) - BARRIER_HEIGHT / 2 - scrollY;
        if (barrierImage != null) {
            gc.drawImage(barrierImage, 0, y, canvas.getWidth(), BARRIER_HEIGHT);
            return;
        }
        gc.setFill(BARRIER_COLOR);
        gc.fillRect(0, y, canvas.getWidth(), BARRIER_HEIGHT);
        gc.setFill(Color.WHITE);
        gc.fillText("BARRIER", canvas.getWidth() / 2 - 2 * ROOM_MARGIN, y + BARRIER_HEIGHT / 2);
    }

    /**
     * Draws a row of dirt teeth hanging below y, a placeholder for the irregular edge of the images.
     */
    private void drawDirtEdge(Color color, double x, double y, double width) {
        gc.setFill(color);
        for (double toothX = x; toothX < x + width; toothX += EDGE_HEIGHT) {
            gc.fillPolygon(new double[] {toothX, toothX + EDGE_HEIGHT, toothX + EDGE_HEIGHT / 2},
                    new double[] {y, y, y + EDGE_HEIGHT}, 3);
        }
    }

    private void drawShaft() {
        double bottom = floorY(mine.getFloors().size());
        gc.setFill(SHAFT_COLOR);
        gc.fillRect(0, SURFACE_HEIGHT - scrollY, SHAFT_WIDTH, bottom - SURFACE_HEIGHT);
    }

    /**
     * Draws the cabin so that its bottom matches the ground of the floor it is at:
     * at position 0 it stands on the surface ground.
     */
    private void drawElevator() {
        Elevator elevator = mine.getElevator();
        double y = getCabinBottomY(elevator.getPosition()) - CABIN_HEIGHT - scrollY;
        gc.setFill(CABIN_COLOR);
        gc.fillRect(CABIN_MARGIN, y, SHAFT_WIDTH - 2 * CABIN_MARGIN, CABIN_HEIGHT);
        gc.setFill(Color.BLACK);
        gc.fillText(NumberFormatter.format(elevator.getCabinAmount()), 2 * CABIN_MARGIN, y + CABIN_HEIGHT / 2);
    }

    /**
     * Between the surface and the first floor the cabin also crosses the ground strip.
     *
     * @param position the elevator position, 0 at the surface and 1 at the first floor
     * @return the world y of the bottom of the cabin
     */
    private double getCabinBottomY(double position) {
        double firstGround = floorY(0) + roomHeight;
        if (position <= 1) {
            return SURFACE_HEIGHT + position * (firstGround - SURFACE_HEIGHT);
        }
        return firstGround + (position - 1) * floorHeight;
    }

    private void drawSurfaceStock() {
        double y = SURFACE_HEIGHT - ROOM_MARGIN - scrollY;
        drawLabel("Stock: " + NumberFormatter.format(mine.getSurfaceAmount()), getRoomX(), y);
    }

    /**
     * Draws the Smoothie Bar standing on the surface ground, with the quantity being sold
     * and a progress bar for the current sale.
     */
    private void drawSellStation() {
        SellStation sellStation = mine.getSellStation();
        double y = getBar().y() - scrollY;
        gc.setFill(BAR_COLOR);
        gc.fillRect(getBarX(), y, BAR_SIZE, BAR_SIZE);
        gc.setFill(PROGRESS_COLOR);
        gc.fillRect(getBarX(), y + BAR_SIZE - CABIN_MARGIN, BAR_SIZE * sellStation.getProgress(), CABIN_MARGIN);
        gc.setFill(Color.WHITE);
        gc.fillText("BAR", getBarX() + CABIN_MARGIN, y + 2 * CABIN_MARGIN);
        drawLabel(NumberFormatter.format(sellStation.getOnSaleAmount()), getBarX(), y - CABIN_MARGIN);
    }

    /**
     * Draws the "Lv" button of a station, green when the player can afford the next upgrade.
     */
    private void drawLevelButton(Station station) {
        Box box = getLevelButton(station);
        double y = box.y() - scrollY;
        gc.setFill(game.canUpgrade(station, controller.getMode()) ? BUYABLE_COLOR : LOCKED_COLOR);
        gc.fillRect(box.x(), y, box.width(), box.height());
        gc.setFill(Color.WHITE);
        gc.fillText("Lv " + station.getLevel() + " ^", box.x() + CABIN_MARGIN, y + LEVEL_HEIGHT - CABIN_MARGIN);
    }

    /**
     * @return where the "Lv" button of the station is, in world coordinates
     */
    private Box getLevelButton(Station station) {
        return switch (station.getType()) {
            case FLOOR -> new Box(getExtractorX() + BOX_SIZE - LEVEL_WIDTH,
                    floorY(((Floor) station).getIndex()) + EDGE_HEIGHT + CABIN_MARGIN, LEVEL_WIDTH, LEVEL_HEIGHT);
            case ELEVATOR -> new Box(CABIN_MARGIN,
                    SURFACE_HEIGHT - CABIN_HEIGHT - LEVEL_HEIGHT - CABIN_MARGIN, LEVEL_WIDTH, LEVEL_HEIGHT);
            case SELL -> new Box(getBarX() + (BAR_SIZE - LEVEL_WIDTH) / 2,
                    getBarWorldY() + (BAR_SIZE - LEVEL_HEIGHT) / 2, LEVEL_WIDTH, LEVEL_HEIGHT);
        };
    }

    /**
     * Draws the "Mgr" button of a station, green when the player can afford a manager.
     */
    private void drawManagerButton(Station station) {
        Box box = getManagerButton(station);
        double y = box.y() - scrollY;
        gc.setFill(game.canHireManager(station) ? BUYABLE_COLOR : LOCKED_COLOR);
        gc.fillRect(box.x(), y, box.width(), box.height());
        gc.setFill(Color.WHITE);
        gc.fillText("Mgr", box.x() + CABIN_MARGIN, y + LEVEL_HEIGHT - CABIN_MARGIN);
    }

    /**
     * @return where the "Mgr" button of the station is, in world coordinates: next to its "Lv" button
     */
    private Box getManagerButton(Station station) {
        Box level = getLevelButton(station);
        return switch (station.getType()) {
            case FLOOR -> new Box(level.x() - LEVEL_WIDTH - CABIN_MARGIN, level.y(), LEVEL_WIDTH, LEVEL_HEIGHT);
            case ELEVATOR -> new Box(level.x(), level.y() - LEVEL_HEIGHT - CABIN_MARGIN, LEVEL_WIDTH, LEVEL_HEIGHT);
            case SELL -> new Box(level.x(), level.y() + LEVEL_HEIGHT + CABIN_MARGIN / 2, LEVEL_WIDTH, LEVEL_HEIGHT);
        };
    }

    /**
     * Draws the manager of the station, if any, as a person-sized rectangle in the color of its rarity:
     * in front of the crate on a floor, next to the shaft or to the bar on the surface.
     */
    private void drawManager(Station station) {
        Manager manager = station.getManager();
        if (manager == null) {
            return;
        }
        double x = switch (station.getType()) {
            case FLOOR -> getManagerX();
            case ELEVATOR -> SHAFT_WIDTH + MANAGER_GAP;
            case SELL -> getBarX() - MANAGER_WIDTH - MANAGER_GAP;
        };
        double groundY = station instanceof Floor floor ? floorY(floor.getIndex()) + roomHeight : SURFACE_HEIGHT;
        gc.setFill(Color.web(manager.getRarity().getColorHex()));
        gc.fillRect(x, groundY - MANAGER_HEIGHT - scrollY, MANAGER_WIDTH, MANAGER_HEIGHT);
    }

    /**
     * @return where the Smoothie Bar is, in world coordinates
     */
    private Box getBar() {
        return new Box(getBarX(), getBarWorldY(), BAR_SIZE, BAR_SIZE);
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
        return floorScreenY + roomHeight;
    }

    private double getRoomX() {
        return SHAFT_WIDTH;
    }

    private double getRoomWidth() {
        return canvas.getWidth() - SHAFT_WIDTH;
    }

    private double getCrateX() {
        return getRoomX() + CABIN_MARGIN;
    }

    /**
     * @return the x of the floor manager, standing just in front of the crate
     */
    private double getManagerX() {
        return getCrateX() + BOX_SIZE + MANAGER_GAP;
    }

    private double getExtractorX() {
        return getRoomX() + getRoomWidth() - ROOM_MARGIN - BOX_SIZE;
    }

    /**
     * A clickable rectangle in world coordinates, shared by the drawing and the click detection.
     */
    private record Box(double x, double y, double width, double height) {

        boolean contains(double pointX, double pointY) {
            return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
        }
    }

    /**
     * @return one image per fruit, indexed by ordinal, null where the file does not exist yet
     */
    private Image[] loadFruitImages(String prefix) {
        Image[] images = new Image[Fruit.values().length];
        for (Fruit fruit : Fruit.values()) {
            images[fruit.ordinal()] = loadImage("/images/mine/" + prefix + fruit.name().toLowerCase() + ".png");
        }
        return images;
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
