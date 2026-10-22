package com.idlesmoothie.view;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.creature.CarryingState;
import com.idlesmoothie.model.creature.DepositingState;
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
import com.idlesmoothie.view.PulpieSprites.Animation;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Draws the underground mine on a canvas and lets the player scroll it vertically.
 * <p>
 * The world is taller than the window: every element has a "world" y coordinate,
 * and is drawn at {@code worldY - scrollY}.
 * Clicking a floor sends its Pulpies on a trip, clicking the shaft calls the elevator
 * and clicking the bar on the surface sells the surface stock.
 * Every station has a small "Lv" button that opens its upgrade popup.
 * A hired manager stands next to its station, in the color of its rarity, and a ghost waits there
 * while none is hired; clicking either one opens the manager shop.
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
    // Center of the drill body of the extractor in the room image, in image pixels: the floor "Lv" button sits on it.
    private static final double ROOM_IMAGE_EXTRACTOR_X = 820;
    private static final double ROOM_IMAGE_EXTRACTOR_Y = 140;
    // Where the floor sign hangs in the room image, in image pixels: between the shaft and the first torch,
    // from the bottom of the ceiling beam.
    private static final double ROOM_IMAGE_SIGN_X = 230;
    private static final double ROOM_IMAGE_CEILING_Y = 38;
    // The sign is as wide as its number plus this margin on each side, and never narrower than SIGN_MIN_WIDTH.
    private static final double SIGN_PADDING_X = 8;
    private static final double SIGN_MIN_WIDTH = 34;
    private static final double SIGN_HEIGHT = 24;
    private static final double SIGN_ROPE_LENGTH = 8;
    // Distance of each rope from the side of the sign.
    private static final double SIGN_ROPE_INSET = 7;
    private static final double SIGN_RADIUS = 7;
    private static final double SIGN_BORDER_WIDTH = 2;
    private static final double SIGN_NAIL_RADIUS = 1.5;
    private static final Color SIGN_WOOD_LIGHT = Color.web("#C98A4B");
    private static final Color SIGN_WOOD_DARK = Color.web("#9A6232");
    private static final Color SIGN_ROPE_COLOR = Color.web("#5C3A1E");
    private static final Color SIGN_NAIL_COLOR = Color.web("#D8D8D8");
    private static final double SIGN_FONT_SIZE = 16;
    private static final Font SIGN_FONT = loadSignFont();
    private static final double EDGE_HEIGHT = 20;
    private static final double GRASS_HEIGHT = 16;
    private static final double GROUND_RAISE = 7;
    private static final double BARRIER_HEIGHT = 104;
    private static final double SHAFT_WIDTH = 110;
    private static final double ROOM_MARGIN = 20;
    private static final double BOX_SIZE = 40;
    // The crate image is a bit wider than the box the Pulpies walk to, and centered on it.
    private static final double CRATE_WIDTH = 52;
    // Distance between the top of the crate and the middle of the quantity written above it.
    private static final double CRATE_TEXT_GAP = 13;
    private static final double PULPIE_SIZE = 24;
    private static final double PULPIE_SPRITE_SIZE = 80;
    /**
     * Distance walked during one frame of the walk and carry animations, in pixels.
     * The frame follows the distance, not the time, so the steps keep pace with the Pulpie at any speed.
     */
    private static final double WALK_STEP = 8;
    private static final long IDLE_FRAME_NANOS = 250_000_000L;
    private static final Map<Class<? extends PulpieState>, Animation> ANIMATIONS = Map.of(
            GoingToExtractorState.class, Animation.WALK,
            FillingState.class, Animation.FILL,
            CarryingState.class, Animation.CARRY,
            DepositingState.class, Animation.DEPOSIT);
    private static final double MANAGER_WIDTH = 24;
    private static final double MANAGER_HEIGHT = 48;
    private static final double MANAGER_GAP = 2;
    // Room kept for a manager, wide enough for its sprite; the placeholder rectangle is centered in it.
    private static final double MANAGER_SPACE = 60;
    // The manager cells are 256px like the Pulpie cells, so the same size keeps the same scale.
    private static final double MANAGER_SPRITE_HEIGHT = PULPIE_SPRITE_SIZE;
    private static final double BREATH_AMPLITUDE = 0.03;
    private static final double BREATH_PERIOD_SECONDS = 2.4;
    // The ghost shown where no manager is hired: see-through, brighter when the player can hire one.
    private static final double GHOST_OPACITY = 0.35;
    private static final double GHOST_HIREABLE_OPACITY = 0.6;
    private static final double GHOST_FLOAT = 4;
    private static final double GHOST_FLOAT_PERIOD_SECONDS = 3.2;
    private static final double CABIN_MARGIN = 8;
    private static final double CABIN_HEIGHT = 60;
    // Share of the shaft width that puts the guide clamps of the cabin image (at 26% and 74% of the cabin)
    // exactly on the rails of the shaft texture (at 34% and 66% of the shaft).
    private static final double CABIN_WIDTH_RATIO = (0.66 - 0.34) / (0.74 - 0.26);
    // Center of the metal plate on the cabin image, as a fraction of the image height.
    private static final double CABIN_SIGN_Y = 0.61;
    // Width of the headframe, as a share of the shaft width: a bit wider, so its base rests on the edges,
    // and its rails stay on the rails of the shaft since both are centered.
    private static final double HEADFRAME_WIDTH_RATIO = 1.1;
    // Top of the headframe platform, as a fraction of the image height: it stands at ground level.
    private static final double HEADFRAME_PLATFORM_Y = 0.89;
    // Center of the plate where the X beams of the headframe cross, as a fraction of the image height.
    private static final double HEADFRAME_BUTTON_Y = 0.38;
    private static final double LEVEL_BUTTON_SIZE = 44;
    private static final double BUTTON_RADIUS = 12;
    private static final double BUTTON_BORDER_WIDTH = 2.5;
    private static final Color BUTTON_BORDER_COLOR = Color.web("#3D2314");
    private static final Font LEVEL_CAPTION_FONT = Font.font("System", FontWeight.BOLD, 10);
    private static final Font LEVEL_NUMBER_FONT = Font.font("System", FontWeight.BOLD, 17);
    // Vertical places of "Lv" and of the level number in the square button, as fractions of its size.
    private static final double LEVEL_CAPTION_Y = 0.28;
    private static final double LEVEL_NUMBER_Y = 0.64;
    private static final Font CABIN_FONT = Font.font("System", FontWeight.BOLD, 12);
    private static final double TEXT_OUTLINE_WIDTH = 3;
    private static final double BAR_SIZE = 100;
    // The bar image is drawn this wide, standing on the surface ground, centered at this share of the window width.
    private static final double BAR_WIDTH = 175;
    private static final double BAR_CENTER_X = 0.69;
    // How far the feet of the bar go below the top of the ground, so they stand in the grass instead of above it.
    private static final double BAR_SINK = 5;
    // The sloped walkway between the headframe and the bar. It keeps its proportions: its deck starts on the
    // headframe, at this fraction of its height (level with its "Lv" button), and goes this far behind
    // the headframe and the bar, so there is no gap. Only its posts are made longer, so their feet stand
    // in the grass like the bar.
    private static final double BRIDGE_START_Y = 0.30;
    private static final double BRIDGE_OVERLAP = 4;
    // In the walkway image, in image pixels: the left and right ends of the deck, the straight part of the posts
    // that is stretched down, the iron foot under it, and the left and right sides of each post foot.
    private static final double BRIDGE_IMAGE_LEFT = 9;
    private static final double BRIDGE_IMAGE_RIGHT = 440;
    private static final double BRIDGE_IMAGE_TRUNK_TOP = 150;
    private static final double BRIDGE_IMAGE_FOOT_TOP = 195;
    private static final double BRIDGE_IMAGE_FOOT_BOTTOM = 211;
    private static final double[][] BRIDGE_IMAGE_POSTS = {{44, 68}, {206, 230}, {368, 392}};
    // Visible right edge of the headframe where the walkway starts, and visible left edge of the bar
    // (the post of its counter) where it ends, as fractions of their image widths.
    private static final double HEADFRAME_EDGE_X = 0.784;
    private static final double BAR_SIDE_X = 0.105;
    // The customers waiting at the right of the bar: smaller Pulpies of other fruits, in their idle animation,
    // facing the bar. The last one is cut by the window edge, as if the line went on.
    private static final double CUSTOMER_SIZE = 64;
    private static final double CUSTOMER_FIRST_GAP = 14;
    private static final double CUSTOMER_SPACING = 30;
    private static final Fruit[] FIRST_CUSTOMERS = {Fruit.KIWI, Fruit.MANGO, Fruit.BLUEBERRY};
    // After each sale, the line moves one place forward in this time, while the served customer
    // walks away to the right at this speed, in pixels per second.
    private static final double LINE_MOVE_SECONDS = 0.6;
    private static final double CUSTOMER_WALK_SPEED = 70;
    // The served customers walk a bit lower than the line, as if closer to the screen, so they pass in front of it.
    private static final double CUSTOMER_LEAVE_LOWER = 6;
    // Shifts the idle animation of each customer so they do not move all together.
    private static final int CUSTOMER_FRAME_SHIFT = 3;
    // Places on the bar image, as fractions of its width and height: the back wall between the juice machine
    // and the shelf, where the "Lv" button sits, and the metal strip of the counter, where the sale progress shows.
    private static final double BAR_WALL_X = 0.52;
    private static final double BAR_WALL_Y = 0.535;
    private static final double BAR_STRIP_LEFT = 0.19;
    private static final double BAR_STRIP_RIGHT = 0.955;
    private static final double BAR_STRIP_Y = 0.743;
    private static final double BAR_PROGRESS_HEIGHT = 6;
    private static final Color BAR_PROGRESS_TRACK = Color.rgb(20, 12, 6, 0.6);
    private static final double BAR_TEXT_GAP = 18;
    // Each sale makes its coins rise from the cash register of the bar image, then fade out.
    private static final double BAR_REGISTER_X = 0.85;
    private static final double BAR_REGISTER_Y = 0.6;
    private static final double SALE_FLOAT_SECONDS = 1;
    private static final double SALE_FLOAT_RISE = 50;
    private static final double NANOS_PER_SECOND = 1e9;
    private static final Color SALE_COLOR = Color.web("#7BE04A");
    private static final Font SALE_FONT = Font.font("System", FontWeight.BOLD, 15);
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
    private static final double STOCK_TEXT_GAP = 16;
    private static final double STOCK_ICON_SIZE = 26;
    private static final double STOCK_ICON_GAP = 2;
    // A soft pill behind the stock, in the dark wood color of the headframe, light enough to let the sky through.
    private static final Color STOCK_PILL_COLOR = Color.rgb(61, 35, 20, 0.35);
    private static final double STOCK_PILL_PADDING_X = 8;
    private static final double STOCK_PILL_PADDING_Y = 2;
    private static final Font STOCK_FONT = Font.font("System", FontWeight.BOLD, 15);
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
    private final Image shaftTopImage;
    private final Image shaftMiddleImage;
    private final Image shaftBottomImage;
    private final Image elevatorImage;
    private final Image headframeImage;
    private final Image smoothieIcon;
    private final Image emptyCrateImage;
    private final Image barImage;
    private final Image bridgeImage;
    // The "+coins" texts rising from the bar, one per sale, oldest first.
    private final List<SaleFloat> saleFloats = new ArrayList<>();
    private int salesSeen;
    // The customers waiting at the bar, the first one closest to it, and the ones walking away after their sale.
    private final List<Fruit> customers = new ArrayList<>(List.of(FIRST_CUSTOMERS));
    private final List<LeavingCustomer> leavingCustomers = new ArrayList<>();
    private int nextCustomerFruit;
    private long lineMoveStartNanos;
    // The full crate of each fruit, with bottles of its color, indexed by fruit.
    private final Image[] fullCrateImages;
    private final PulpieSprites pulpieSprites;
    private final PulpieSprites customerSprites;
    private final ManagerSprites managerSprites;
    private final Game game;
    private Mine mine;
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
     * @param shopView     popup opened by a click on a manager or its ghost
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
        this.shaftTopImage = loadImage("/images/mine/shaft_top.png");
        this.shaftMiddleImage = loadImage("/images/mine/shaft_middle.png");
        this.shaftBottomImage = loadImage("/images/mine/shaft_bottom.png");
        this.elevatorImage = loadImage("/images/mine/elevator.png");
        this.headframeImage = loadImage("/images/mine/headframe.png");
        this.smoothieIcon = loadImage("/images/ui/smoothie.png");
        this.emptyCrateImage = loadImage("/images/mine/crate_empty.png");
        this.barImage = loadImage("/images/mine/bar.png");
        this.bridgeImage = loadImage("/images/mine/bridge.png");
        this.fullCrateImages = loadFruitImages("crate_");
        this.pulpieSprites = new PulpieSprites(PULPIE_SPRITE_SIZE);
        this.customerSprites = new PulpieSprites(CUSTOMER_SIZE);
        this.managerSprites = new ManagerSprites(MANAGER_SPRITE_HEIGHT);
    }

    /**
     * Redraws the whole visible part of the mine.
     */
    @Override
    public void render() {
        if (mine != game.getMine()) {
            mine = game.getMine();
            scrollY = 0;
            saleFloats.clear();
            salesSeen = mine.getSellStation().getSalesMade();
        }
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
        // After every floor, since the dirt at the bottom of a room image covers the top of the room below.
        for (Floor floor : floors) {
            drawFloorSign(floor, floorY(floor.getIndex()) - scrollY);
        }
        drawGround();
        for (int i = floors.size(); i < mine.getMaxFloors(); i++) {
            if (mine.isBehindBarrier(i)) {
                drawBarrier(i);
            }
        }
        drawShaft();
        drawBridge();
        drawHeadframe();
        drawElevator();
        drawSurfaceStock();
        drawSellStation();
        for (Station station : getStations()) {
            if (!(station instanceof Floor)) {
                drawManager(station);
            }
            drawLevelButton(station);
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
     * Finds what was clicked: the "Lv" buttons first since they lie on top of the stations, then the
     * managers (or their empty place), which open the manager shop.
     */
    private void onClick(double x, double screenY) {
        double worldY = screenY + scrollY;
        for (Station station : getStations()) {
            if (getLevelButton(station).contains(x, worldY)) {
                upgradePanel.show(station);
                return;
            }
        }
        for (Station station : getStations()) {
            if (getManagerArea(station).contains(x, worldY)) {
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

        double boxY = getGroundY(y) - BOX_SIZE;
        if (image == null) {
            gc.setFill(EXTRACTOR_COLOR);
            gc.fillRect(getExtractorX(), boxY, BOX_SIZE, BOX_SIZE);
        }
        drawCrate(floor, getGroundY(y));

        // The manager stands behind the Pulpies that come to deposit in front of it.
        drawManager(floor);
        for (Pulpie pulpie : floor.getPulpies()) {
            drawPulpie(pulpie, fruit, y);
        }
    }

    /**
     * Draws the Pulpie with the animation of its state, or as a square if the sheet does not exist yet.
     */
    private void drawPulpie(Pulpie pulpie, Fruit fruit, double floorScreenY) {
        double start = getManagerX() + MANAGER_SPACE + MANAGER_GAP;
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
        Animation animation = state == null ? Animation.IDLE : ANIMATIONS.get(state.getClass());
        if (animation != null && pulpieSprites.has(fruit, animation)) {
            int frame = getFrame(animation, pulpieSprites.getFrameCount(fruit, animation), pulpie, end - start);
            pulpieSprites.draw(gc, fruit, animation, frame, x + PULPIE_SIZE / 2, getGroundY(floorScreenY),
                    isFacingLeft(animation));
            return;
        }
        Color color = Color.WHITE;
        if (pulpie.getBag() != null) {
            color = color.darker();
        }
        gc.setFill(color);
        gc.fillRect(x, getGroundY(floorScreenY) - PULPIE_SIZE, PULPIE_SIZE, PULPIE_SIZE);
    }

    /**
     * Walk and carry loop with the distance walked, fill and deposit play once over their state,
     * and idle loops with the clock.
     *
     * @param path distance between the start of the trip and the extractor, in pixels
     */
    private static int getFrame(Animation animation, int frameCount, Pulpie pulpie, double path) {
        return switch (animation) {
            case WALK, CARRY -> (int) (path * pulpie.getProgress() / WALK_STEP) % frameCount;
            case FILL, DEPOSIT -> Math.min(frameCount - 1, (int) (pulpie.getProgress() * frameCount));
            case IDLE -> (int) (System.nanoTime() / IDLE_FRAME_NANOS % frameCount);
        };
    }

    /**
     * The sprites face right. A Pulpie faces where it goes: the extractor when it walks there, fills
     * or waits for its next trip, and the crate when it carries the smoothie back or deposits it.
     */
    private boolean isFacingLeft(Animation animation) {
        boolean towardCrate = animation == Animation.CARRY || animation == Animation.DEPOSIT;
        boolean crateOnLeft = getCrateX() < getExtractorX();
        return towardCrate == crateOnLeft;
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

    /**
     * Draws the shaft from the surface down to the next floor to buy: the top beam, then the middle part
     * repeated as many times as needed, and the bottom beam. Falls back to a plain color if an image is missing.
     */
    private void drawShaft() {
        double top = SURFACE_HEIGHT;
        double bottom = floorY(mine.getFloors().size());
        if (shaftTopImage == null || shaftMiddleImage == null || shaftBottomImage == null) {
            gc.setFill(SHAFT_COLOR);
            gc.fillRect(0, top - scrollY, SHAFT_WIDTH, bottom - top);
            return;
        }
        double topHeight = scaledHeight(shaftTopImage);
        double bottomHeight = scaledHeight(shaftBottomImage);
        double tileHeight = scaledHeight(shaftMiddleImage);

        // The last tile rarely fits: only its upper part is drawn, cut from the source image.
        double tilesEnd = bottom - bottomHeight;
        for (double y = top + topHeight; y < tilesEnd; y += tileHeight) {
            double height = Math.min(tileHeight, tilesEnd - y);
            if (isVisible(y, height)) {
                double sourceHeight = shaftMiddleImage.getHeight() * height / tileHeight;
                gc.drawImage(shaftMiddleImage, 0, 0, shaftMiddleImage.getWidth(), sourceHeight,
                        0, y - scrollY, SHAFT_WIDTH, height);
            }
        }
        gc.drawImage(shaftTopImage, 0, top - scrollY, SHAFT_WIDTH, topHeight);
        gc.drawImage(shaftBottomImage, 0, tilesEnd - scrollY, SHAFT_WIDTH, bottomHeight);
    }

    /**
     * @return the height of the image once drawn as wide as the shaft, keeping its proportions
     */
    private double scaledHeight(Image image) {
        return image.getHeight() * SHAFT_WIDTH / image.getWidth();
    }

    /**
     * @return whether a band of the world, from y and of the given height, is on screen
     */
    private boolean isVisible(double worldY, double height) {
        return worldY + height > scrollY && worldY < scrollY + canvas.getHeight();
    }

    /**
     * Draws the tower above the shaft, its platform at ground level, where the cabin parks at the surface.
     * The cabin is drawn after it, so it passes in front of the tower.
     */
    private void drawHeadframe() {
        if (headframeImage == null) {
            return;
        }
        double width = headframeWidth();
        double height = headframeHeight();
        double x = (SHAFT_WIDTH - width) / 2;
        double y = SURFACE_HEIGHT - height * HEADFRAME_PLATFORM_Y - scrollY;
        gc.drawImage(headframeImage, x, y, width, height);
    }

    /**
     * Draws the walkway from the top of the headframe down to the bar, on its posts. It is drawn before both,
     * so its ends hide behind them. Nothing is drawn if the walkway, headframe or bar image is missing.
     */
    private void drawBridge() {
        if (bridgeImage == null || headframeImage == null || barImage == null) {
            return;
        }
        Box bar = getBar();
        double headframeX = (SHAFT_WIDTH - headframeWidth()) / 2;
        double deckLeft = headframeX + headframeWidth() * HEADFRAME_EDGE_X - BRIDGE_OVERLAP;
        double deckRight = bar.x() + bar.width() * BAR_SIDE_X + BRIDGE_OVERLAP;
        double top = SURFACE_HEIGHT - headframeHeight() * (HEADFRAME_PLATFORM_Y - BRIDGE_START_Y);
        double scale = (deckRight - deckLeft) / (BRIDGE_IMAGE_RIGHT - BRIDGE_IMAGE_LEFT);
        double imageX = deckLeft - BRIDGE_IMAGE_LEFT * scale;
        double y = top - scrollY;
        // The deck, its rail and the top of the posts, down to where the iron feet start.
        gc.drawImage(bridgeImage, 0, 0, bridgeImage.getWidth(), BRIDGE_IMAGE_FOOT_TOP,
                imageX, y, bridgeImage.getWidth() * scale, BRIDGE_IMAGE_FOOT_TOP * scale);
        // Each post goes on down to the grass with its straight part stretched, then its foot.
        double footHeight = (BRIDGE_IMAGE_FOOT_BOTTOM - BRIDGE_IMAGE_FOOT_TOP) * scale;
        double footY = SURFACE_HEIGHT + BAR_SINK - footHeight - scrollY;
        double trunkY = y + BRIDGE_IMAGE_FOOT_TOP * scale;
        for (double[] post : BRIDGE_IMAGE_POSTS) {
            double postX = imageX + post[0] * scale;
            double postWidth = (post[1] - post[0]) * scale;
            gc.drawImage(bridgeImage, post[0], BRIDGE_IMAGE_TRUNK_TOP, post[1] - post[0],
                    BRIDGE_IMAGE_FOOT_TOP - BRIDGE_IMAGE_TRUNK_TOP, postX, trunkY, postWidth, footY - trunkY);
            gc.drawImage(bridgeImage, post[0], BRIDGE_IMAGE_FOOT_TOP, post[1] - post[0],
                    BRIDGE_IMAGE_FOOT_BOTTOM - BRIDGE_IMAGE_FOOT_TOP, postX, footY, postWidth, footHeight);
        }
    }

    /**
     * @return the height the headframe is drawn at, keeping the image proportions
     */
    private double headframeHeight() {
        return headframeImage.getHeight() * headframeWidth() / headframeImage.getWidth();
    }

    /**
     * @return the width the headframe is drawn at
     */
    private double headframeWidth() {
        return SHAFT_WIDTH * HEADFRAME_WIDTH_RATIO;
    }

    /**
     * At the surface, from left to right: the headframe, the elevator manager, its buttons, then the stock.
     *
     * @return the x where the elevator manager stands, just right of the headframe
     */
    private double getElevatorManagerX() {
        return (SHAFT_WIDTH + headframeWidth()) / 2 + MANAGER_GAP;
    }

    /**
     * Draws the cabin so that its bottom matches the ground of the floor it is at:
     * at position 0 it stands on the surface ground. It is centered in the shaft, with its clamps on the rails,
     * and shows its load on its metal plate. Falls back to a plain color if the image is missing.
     */
    private void drawElevator() {
        Elevator elevator = mine.getElevator();
        double width = SHAFT_WIDTH * CABIN_WIDTH_RATIO;
        double x = (SHAFT_WIDTH - width) / 2;
        double height = cabinHeight();
        double y = getCabinBottomY(elevator.getPosition()) - height - scrollY;
        if (elevatorImage != null) {
            gc.drawImage(elevatorImage, x, y, width, height);
        } else {
            gc.setFill(CABIN_COLOR);
            gc.fillRect(x, y, width, height);
        }
        drawCenteredText(NumberFormatter.format(elevator.getCabinAmount()),
                SHAFT_WIDTH / 2, y + height * CABIN_SIGN_Y);
    }

    /**
     * @return the height of the cabin once drawn at its width, keeping the image proportions
     */
    private double cabinHeight() {
        if (elevatorImage == null) {
            return CABIN_HEIGHT;
        }
        return elevatorImage.getHeight() * SHAFT_WIDTH * CABIN_WIDTH_RATIO / elevatorImage.getWidth();
    }

    /**
     * Writes a small white text with a dark outline, centered on the given point, readable over any image.
     */
    private void drawCenteredText(String text, double centerX, double centerY) {
        drawCenteredText(text, centerX, centerY, CABIN_FONT);
    }

    /**
     * Writes a white text with a dark outline in the given font, centered on the given point.
     */
    private void drawCenteredText(String text, double centerX, double centerY, Font font) {
        drawCenteredText(text, centerX, centerY, font, Color.WHITE);
    }

    /**
     * Writes a text of the given color with a dark outline in the given font, centered on the given point.
     */
    private void drawCenteredText(String text, double centerX, double centerY, Font font, Color color) {
        gc.save();
        gc.setFont(font);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setLineWidth(TEXT_OUTLINE_WIDTH);
        gc.setStroke(Color.BLACK);
        gc.strokeText(text, centerX, centerY);
        gc.setFill(color);
        gc.fillText(text, centerX, centerY);
        gc.restore();
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

    /**
     * Writes the smoothie waiting at the surface, after a smoothie icon, just above the roof of the headframe,
     * or above the "Lv" button of the elevator if the image is missing.
     */
    private void drawSurfaceStock() {
        double top = headframeImage == null
                ? getHeadframeLevelButton().y()
                : SURFACE_HEIGHT - headframeHeight() * HEADFRAME_PLATFORM_Y;
        drawSmoothieBadge(mine.getSurfaceAmount(), SHAFT_WIDTH / 2, top - STOCK_TEXT_GAP - scrollY);
    }

    /**
     * Writes a quantity of smoothie after a smoothie icon, on a soft pill, centered on the given point
     * without leaving the screen. Used above the headframe and above the bar.
     */
    private void drawSmoothieBadge(double quantity, double centerX, double centerY) {
        String amount = NumberFormatter.format(quantity);
        gc.save();
        gc.setFont(STOCK_FONT);
        double amountWidth = textWidth(amount);
        gc.restore();
        double iconWidth = smoothieIcon == null ? 0 : STOCK_ICON_SIZE + STOCK_ICON_GAP;
        double contentWidth = iconWidth + amountWidth;
        double left = Math.min(Math.max(STOCK_PILL_PADDING_X, centerX - contentWidth / 2),
                canvas.getWidth() - STOCK_PILL_PADDING_X - contentWidth);
        double pillHeight = STOCK_ICON_SIZE + 2 * STOCK_PILL_PADDING_Y;
        gc.setFill(STOCK_PILL_COLOR);
        gc.fillRoundRect(left - STOCK_PILL_PADDING_X, centerY - pillHeight / 2,
                iconWidth + amountWidth + 2 * STOCK_PILL_PADDING_X, pillHeight, pillHeight, pillHeight);
        if (smoothieIcon != null) {
            gc.drawImage(smoothieIcon, left, centerY - STOCK_ICON_SIZE / 2, STOCK_ICON_SIZE, STOCK_ICON_SIZE);
        }
        drawCenteredText(amount, left + iconWidth + amountWidth / 2, centerY, STOCK_FONT);
    }

    /**
     * Draws the floor number on a small wooden sign hanging from the ceiling by two ropes.
     */
    private void drawFloorSign(Floor floor, double floorScreenY) {
        double centerX;
        double ceilingY;
        if (roomImages[floor.getFruit().ordinal()] != null) {
            double roomScale = canvas.getWidth() / ROOM_IMAGE_WIDTH;
            centerX = ROOM_IMAGE_SIGN_X * roomScale;
            ceilingY = floorScreenY + ROOM_IMAGE_CEILING_Y * roomScale;
        } else {
            centerX = getCrateX() + SIGN_MIN_WIDTH / 2;
            ceilingY = floorScreenY + EDGE_HEIGHT;
        }
        String number = String.valueOf(floor.getIndex() + 1);
        gc.save();
        gc.setFont(SIGN_FONT);
        double width = Math.max(SIGN_MIN_WIDTH, textWidth(number) + 2 * SIGN_PADDING_X);
        gc.restore();
        double left = centerX - width / 2;
        double top = ceilingY + SIGN_ROPE_LENGTH;
        double[] ropes = {left + SIGN_ROPE_INSET, left + width - SIGN_ROPE_INSET};

        gc.setStroke(SIGN_ROPE_COLOR);
        gc.setLineWidth(SIGN_BORDER_WIDTH);
        for (double ropeX : ropes) {
            gc.strokeLine(ropeX, ceilingY, ropeX, top + SIGN_NAIL_RADIUS);
        }
        gc.setFill(new LinearGradient(0, top, 0, top + SIGN_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, SIGN_WOOD_LIGHT), new Stop(1, SIGN_WOOD_DARK)));
        gc.fillRoundRect(left, top, width, SIGN_HEIGHT, SIGN_RADIUS, SIGN_RADIUS);
        gc.setStroke(BUTTON_BORDER_COLOR);
        gc.strokeRoundRect(left, top, width, SIGN_HEIGHT, SIGN_RADIUS, SIGN_RADIUS);
        gc.setFill(SIGN_NAIL_COLOR);
        for (double ropeX : ropes) {
            gc.fillOval(ropeX - SIGN_NAIL_RADIUS, top + SIGN_NAIL_RADIUS, 2 * SIGN_NAIL_RADIUS, 2 * SIGN_NAIL_RADIUS);
        }
        drawCenteredText(number, centerX, top + SIGN_HEIGHT / 2 + 1, SIGN_FONT);
    }

    /**
     * @return the Lilita One font of the popups for the floor signs, or a bold system font if the file is missing
     */
    private static Font loadSignFont() {
        InputStream stream = MineView.class.getResourceAsStream("/fonts/LilitaOne-Regular.ttf");
        Font font = stream == null ? null : Font.loadFont(stream, SIGN_FONT_SIZE);
        return font != null ? font : Font.font("System", FontWeight.BOLD, SIGN_FONT_SIZE);
    }

    /**
     * Draws the crate of the floor standing on the ground where the Pulpies drop their smoothie:
     * empty, or full of bottles of the floor fruit color, with the quantity it holds written just above it. Falls back to a plain box if the image is missing.
     */
    private void drawCrate(Floor floor, double groundScreenY) {
        String amount = NumberFormatter.format(floor.getStoredAmount());
        double centerX = getCrateX() + BOX_SIZE / 2;
        // Every crate image has the same size with the box at the same place, only the bottles stick out.
        Image crateImage = floor.isCrateEmpty() ? emptyCrateImage : fullCrateImages[floor.getFruit().ordinal()];
        if (crateImage == null) {
            gc.setFill(CRATE_COLOR);
            gc.fillRect(getCrateX(), groundScreenY - BOX_SIZE, BOX_SIZE, BOX_SIZE);
            drawCenteredText(amount, centerX, groundScreenY - BOX_SIZE / 2);
            return;
        }
        double height = crateImage.getHeight() * CRATE_WIDTH / crateImage.getWidth();
        double top = groundScreenY - height;
        gc.drawImage(crateImage, centerX - CRATE_WIDTH / 2, top, CRATE_WIDTH, height);
        drawCenteredText(amount, centerX, top - CRATE_TEXT_GAP);
    }

    /**
     * @return where the square "Lv" button of the bar is: on the back wall of the bar image,
     *         or in the middle of the plain box if the image is missing, in world coordinates
     */
    private Box getBarLevelButton() {
        Box bar = getBar();
        double centerX = bar.x() + bar.width() * (barImage == null ? 0.5 : BAR_WALL_X);
        double centerY = bar.y() + bar.height() * (barImage == null ? 0.5 : BAR_WALL_Y);
        return new Box(centerX - LEVEL_BUTTON_SIZE / 2, centerY - LEVEL_BUTTON_SIZE / 2,
                LEVEL_BUTTON_SIZE, LEVEL_BUTTON_SIZE);
    }

    /**
     * @return where the square "Lv" button of the floor is: on the drill body of the extractor of the room image,
     *         or on the extractor box if the image is missing, in world coordinates
     */
    private Box getExtractorLevelButton(Floor floor) {
        double top = floorY(floor.getIndex());
        double centerX;
        double centerY;
        if (roomImages[floor.getFruit().ordinal()] != null) {
            double roomScale = canvas.getWidth() / ROOM_IMAGE_WIDTH;
            centerX = ROOM_IMAGE_EXTRACTOR_X * roomScale;
            centerY = top + ROOM_IMAGE_EXTRACTOR_Y * roomScale;
        } else {
            centerX = getExtractorX() + BOX_SIZE / 2;
            centerY = top + roomHeight - BOX_SIZE / 2;
        }
        return new Box(centerX - LEVEL_BUTTON_SIZE / 2, centerY - LEVEL_BUTTON_SIZE / 2,
                LEVEL_BUTTON_SIZE, LEVEL_BUTTON_SIZE);
    }

    /**
     * @return where the square "Lv" button of the elevator is: on the crossing of the X beams of the headframe,
     *         or above the parked cabin if the image is missing, in world coordinates
     */
    private Box getHeadframeLevelButton() {
        double centerY = headframeImage == null
                ? SURFACE_HEIGHT - cabinHeight() - LEVEL_BUTTON_SIZE
                : SURFACE_HEIGHT - headframeHeight() * (HEADFRAME_PLATFORM_Y - HEADFRAME_BUTTON_Y);
        return new Box((SHAFT_WIDTH - LEVEL_BUTTON_SIZE) / 2, centerY - LEVEL_BUTTON_SIZE / 2,
                LEVEL_BUTTON_SIZE, LEVEL_BUTTON_SIZE);
    }

    /**
     * Draws a square button with round corners and a dark frame: "Lv" on top and the level below,
     * green when the player can afford the next upgrade.
     */
    private void drawSquareLevelButton(Station station, double x, double y, double size) {
        gc.setFill(game.canUpgrade(station, controller.getMode()) ? BUYABLE_COLOR : LOCKED_COLOR);
        gc.fillRoundRect(x, y, size, size, BUTTON_RADIUS, BUTTON_RADIUS);
        gc.setStroke(BUTTON_BORDER_COLOR);
        gc.setLineWidth(BUTTON_BORDER_WIDTH);
        gc.strokeRoundRect(x, y, size, size, BUTTON_RADIUS, BUTTON_RADIUS);
        gc.save();
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(LEVEL_CAPTION_FONT);
        gc.fillText("Lv", x + size / 2, y + size * LEVEL_CAPTION_Y);
        gc.setFont(LEVEL_NUMBER_FONT);
        gc.fillText(String.valueOf(station.getLevel()), x + size / 2, y + size * LEVEL_NUMBER_Y);
        gc.restore();
    }

    /**
     * Draws the Smoothie Bar standing on the surface ground, with the quantity being sold
     * and a progress bar for the current sale.
     */
    private void drawSellStation() {
        SellStation sellStation = mine.getSellStation();
        checkNewSale(sellStation);
        Box bar = getBar();
        double y = bar.y() - scrollY;
        if (barImage == null) {
            gc.setFill(BAR_COLOR);
            gc.fillRect(bar.x(), y, bar.width(), bar.height());
            gc.setFill(PROGRESS_COLOR);
            gc.fillRect(bar.x(), y + bar.height() - CABIN_MARGIN, bar.width() * sellStation.getProgress(), CABIN_MARGIN);
            drawSmoothieBadge(sellStation.getOnSaleAmount(), bar.x() + bar.width() / 2, y - BAR_TEXT_GAP);
            return;
        }
        gc.drawImage(barImage, bar.x(), y, bar.width(), bar.height());
        // The sale progress fills the metal strip of the counter, over a dark track.
        double stripX = bar.x() + bar.width() * BAR_STRIP_LEFT;
        double stripWidth = bar.width() * (BAR_STRIP_RIGHT - BAR_STRIP_LEFT);
        double stripY = y + bar.height() * BAR_STRIP_Y - BAR_PROGRESS_HEIGHT / 2;
        gc.setFill(BAR_PROGRESS_TRACK);
        gc.fillRoundRect(stripX, stripY, stripWidth, BAR_PROGRESS_HEIGHT, BAR_PROGRESS_HEIGHT, BAR_PROGRESS_HEIGHT);
        gc.setFill(PROGRESS_COLOR);
        gc.fillRoundRect(stripX, stripY, stripWidth * sellStation.getProgress(), BAR_PROGRESS_HEIGHT,
                BAR_PROGRESS_HEIGHT, BAR_PROGRESS_HEIGHT);
        drawSmoothieBadge(sellStation.getOnSaleAmount(), bar.x() + bar.width() / 2, y - BAR_TEXT_GAP);
        drawCustomers(bar, y);
        drawSaleFloats(bar, y);
    }

    /**
     * On a new sale, starts its "+coins" text, sends the first customer walking away and moves the line forward,
     * with a new customer coming in at the back.
     */
    private void checkNewSale(SellStation sellStation) {
        if (sellStation.getSalesMade() == salesSeen) {
            return;
        }
        salesSeen = sellStation.getSalesMade();
        long now = System.nanoTime();
        saleFloats.add(new SaleFloat("+" + NumberFormatter.format(sellStation.getLastSaleCoins()), now));
        leavingCustomers.add(new LeavingCustomer(customers.remove(0), now));
        customers.add(Fruit.values()[nextCustomerFruit]);
        nextCustomerFruit = (nextCustomerFruit + 1) % Fruit.values().length;
        lineMoveStartNanos = now;
    }

    /**
     * Draws the line of customers waiting at the right of the bar, the farthest first so the closest stands in front.
     */
    private void drawCustomers(Box bar, double barScreenY) {
        long now = System.nanoTime();
        double groundY = barScreenY + bar.height();
        double frontX = bar.x() + bar.width() + CUSTOMER_FIRST_GAP;
        // From 0 to 1 while the line moves one place forward after a sale, 1 once everyone stands still.
        double move = Math.min(1, (now - lineMoveStartNanos) / NANOS_PER_SECOND / LINE_MOVE_SECONDS);
        boolean moving = move < 1;
        for (int i = customers.size() - 1; i >= 0; i--) {
            Fruit fruit = customers.get(i);
            double x = frontX + (i + (1 - move)) * CUSTOMER_SPACING;
            Animation animation = moving ? Animation.WALK : Animation.IDLE;
            if (!customerSprites.has(fruit, animation)) {
                continue;
            }
            int frameCount = customerSprites.getFrameCount(fruit, animation);
            int frame = moving
                    ? (int) (CUSTOMER_SPACING * move / WALK_STEP) % frameCount
                    : (int) ((now / IDLE_FRAME_NANOS + i * CUSTOMER_FRAME_SHIFT) % frameCount);
            customerSprites.draw(gc, fruit, animation, frame, x, groundY, true);
        }
        drawLeavingCustomers(frontX, groundY + CUSTOMER_LEAVE_LOWER, now);
    }

    /**
     * The customers already served walk away to the right, in front of the line, until they leave the window.
     */
    private void drawLeavingCustomers(double frontX, double groundY, long now) {
        leavingCustomers.removeIf(leaving -> frontX + walkedAway(leaving, now) > canvas.getWidth() + CUSTOMER_SIZE);
        for (LeavingCustomer leaving : leavingCustomers) {
            if (!customerSprites.has(leaving.fruit(), Animation.WALK)) {
                continue;
            }
            double walked = walkedAway(leaving, now);
            int frame = (int) (walked / WALK_STEP) % customerSprites.getFrameCount(leaving.fruit(), Animation.WALK);
            customerSprites.draw(gc, leaving.fruit(), Animation.WALK, frame, frontX + walked, groundY, false);
        }
    }

    /**
     * @return how far the served customer has walked from the front of the line, in pixels
     */
    private static double walkedAway(LeavingCustomer leaving, long now) {
        return (now - leaving.startNanos()) / NANOS_PER_SECOND * CUSTOMER_WALK_SPEED;
    }

    /**
     * Makes every "+coins" text rise from the cash register and fade out.
     */
    private void drawSaleFloats(Box bar, double barScreenY) {
        long now = System.nanoTime();
        saleFloats.removeIf(sale -> (now - sale.startNanos()) / NANOS_PER_SECOND >= SALE_FLOAT_SECONDS);
        double startX = bar.x() + bar.width() * (barImage == null ? 0.5 : BAR_REGISTER_X);
        double startY = barScreenY + bar.height() * (barImage == null ? 0.5 : BAR_REGISTER_Y);
        for (SaleFloat sale : saleFloats) {
            double progress = (now - sale.startNanos()) / NANOS_PER_SECOND / SALE_FLOAT_SECONDS;
            double y = startY - SALE_FLOAT_RISE * progress;
            gc.save();
            gc.setGlobalAlpha(1 - progress);
            gc.setFont(SALE_FONT);
            double textWidth = textWidth(sale.text());
            double left = startX - (textWidth + COIN_GAP + COIN_SIZE) / 2;
            drawCenteredText(sale.text(), left + textWidth / 2, y, SALE_FONT, SALE_COLOR);
            gc.drawImage(CoinIcon.IMAGE, left + textWidth + COIN_GAP, y - COIN_SIZE / 2, COIN_SIZE, COIN_SIZE);
            gc.restore();
        }
    }

    /**
     * Draws the "Lv" button of a station, green when the player can afford the next upgrade.
     */
    private void drawLevelButton(Station station) {
        Box box = getLevelButton(station);
        drawSquareLevelButton(station, box.x(), box.y() - scrollY, box.width());
    }

    /**
     * @return where the "Lv" button of the station is, in world coordinates
     */
    private Box getLevelButton(Station station) {
        return switch (station.getType()) {
            case FLOOR -> getExtractorLevelButton((Floor) station);
            case ELEVATOR -> getHeadframeLevelButton();
            case SELL -> getBarLevelButton();
        };
    }

    /**
     * @return where the manager of the station stands, in world coordinates: in front of the crate on a
     *         floor, next to the shaft or to the bar on the surface. Clicking there opens the manager shop,
     *         whether a manager is hired or not.
     */
    private Box getManagerArea(Station station) {
        double x = switch (station.getType()) {
            case FLOOR -> getManagerX();
            case ELEVATOR -> getElevatorManagerX();
            case SELL -> getBarX() - MANAGER_SPACE - MANAGER_GAP;
        };
        double groundY = station instanceof Floor floor ? floorY(floor.getIndex()) + roomHeight : SURFACE_HEIGHT;
        return new Box(x, groundY - getManagerHeight(), MANAGER_SPACE, getManagerHeight());
    }

    /**
     * Draws the manager of the station: the sprite of its rarity, breathing slowly, or a person-sized
     * rectangle in the color of its rarity if the sheet does not exist yet. Without a manager, draws a
     * floating ghost, brighter when the player can hire one, or nothing if the ghost sheet does not exist.
     */
    private void drawManager(Station station) {
        Box area = getManagerArea(station);
        double bodyX = area.x() + MANAGER_SPACE / 2;
        double groundY = area.y() + area.height() - scrollY;
        Manager manager = station.getManager();
        if (manager == null) {
            if (managerSprites.hasGhost()) {
                double opacity = game.canHireManager(station) ? GHOST_HIREABLE_OPACITY : GHOST_OPACITY;
                double lift = GHOST_FLOAT * (1 + Math.sin(2 * Math.PI * (System.nanoTime() / 1e9
                        / GHOST_FLOAT_PERIOD_SECONDS + phase(station)))) / 2;
                managerSprites.drawGhost(gc, bodyX, groundY - lift, breath(station), opacity);
            }
            return;
        }
        if (managerSprites.isLoaded()) {
            managerSprites.draw(gc, manager.getRarity(), bodyX, groundY, breath(station));
            return;
        }
        gc.setFill(Color.web(manager.getRarity().getColorHex()));
        gc.fillRect(bodyX - MANAGER_WIDTH / 2, groundY - MANAGER_HEIGHT, MANAGER_WIDTH, MANAGER_HEIGHT);
    }

    /**
     * @return how high a standing manager rises above the ground: its sprite, or the placeholder rectangle
     */
    private double getManagerHeight() {
        return managerSprites.isLoaded() ? managerSprites.getStandingHeight() : MANAGER_HEIGHT;
    }

    /**
     * @return the vertical stretch of a breathing manager, a few percent around 1; each station
     *         has its own phase so the managers do not breathe together
     */
    private static double breath(Station station) {
        double seconds = System.nanoTime() / 1e9;
        return 1 + BREATH_AMPLITUDE * Math.sin(2 * Math.PI * (seconds / BREATH_PERIOD_SECONDS + phase(station)));
    }

    /**
     * @return a fixed offset between 0 and 1 for the station, so its manager does not move in step with the others
     */
    private static double phase(Station station) {
        return System.identityHashCode(station) % 1000 / 1000.0;
    }

    /**
     * @return where the Smoothie Bar is, in world coordinates
     */
    private Box getBar() {
        if (barImage == null) {
            return new Box(canvas.getWidth() - BAR_SIZE, SURFACE_HEIGHT - BAR_SIZE, BAR_SIZE, BAR_SIZE);
        }
        double height = barImage.getHeight() * BAR_WIDTH / barImage.getWidth();
        return new Box(canvas.getWidth() * BAR_CENTER_X - BAR_WIDTH / 2, SURFACE_HEIGHT + BAR_SINK - height,
                BAR_WIDTH, height);
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

    /**
     * @return the left side of the bar, in world coordinates
     */
    private double getBarX() {
        return getBar().x();
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
    /**
     * A "+coins" text rising from the bar after a sale.
     *
     * @param text       what is written, such as "+12.5K"
     * @param startNanos when the sale happened, from {@link System#nanoTime()}
     */
    private record SaleFloat(String text, long startNanos) {
    }

    /**
     * A served customer walking away from the bar.
     *
     * @param fruit      the fruit of the customer
     * @param startNanos when it was served, from {@link System#nanoTime()}
     */
    private record LeavingCustomer(Fruit fruit, long startNanos) {
    }

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
