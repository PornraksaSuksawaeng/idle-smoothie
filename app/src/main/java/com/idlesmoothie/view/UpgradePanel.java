package com.idlesmoothie.view;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.DoubleFunction;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.economy.BuyMode;
import com.idlesmoothie.model.economy.UpgradeReport;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.SellStation;
import com.idlesmoothie.model.station.Station;
import com.idlesmoothie.model.station.StationStats;
import com.idlesmoothie.model.station.StationType;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Popup in the middle of the screen to upgrade one station, over a darkened background.
 * It has the same wooden frame, paper sheet, title ribbon and round close button as {@link SettingsView}.
 * It shows what the purchase gives before buying: the target level, the progress toward the next
 * milestone, and each stat now and after the purchase. The player picks x1 / x10 / MAX and buys;
 * holding the button keeps buying, faster and faster.
 * The look comes from the {@code game-ui.css} stylesheet added to the scene.
 */
public class UpgradePanel implements Renderable {

    private static final double WIDTH = 420;
    private static final double SPACING = 8;
    private static final double SMALL_SPACING = 4;
    private static final double RIBBON_RISE = 26;
    private static final double CLOSE_OFFSET = 14;
    private static final double NAIL_RADIUS = 5;
    private static final double NAIL_INSET = 6;
    private static final double COIN_SIZE = 22;
    private static final double BAR_WIDTH = 320;
    private static final double PERCENT = 100;

    private static final double POP_SCALE = 1.2;
    private static final Duration POP_GROW = Duration.millis(90);
    private static final Duration POP_SETTLE = Duration.millis(140);
    // The "+10 LV" text starts just above the level badge, then rises.
    private static final double FLOAT_START = 34;
    private static final double FLOAT_RISE = 40;
    private static final Duration FLOAT_DURATION = Duration.millis(700);

    // Holding the buy button: a first pause, then purchases closer and closer together.
    private static final Duration HOLD_TICK = Duration.millis(20);
    private static final double HOLD_FIRST_DELAY = 0.4;
    private static final double HOLD_START_INTERVAL = 0.25;
    private static final double HOLD_MIN_INTERVAL = 0.05;
    private static final double HOLD_ACCELERATION = 0.85;

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass UNLOCKED = PseudoClass.getPseudoClass("unlocked");

    private final Game game;
    private final UpgradeController controller;
    private final StackPane root;
    private final Text title = new Text();
    private final Label levelBadge = new Label();
    private final Label targetLabel = new Label();
    private final Text floatText = new Text();
    private final Label bottleneckBadge = new Label("BOTTLENECK");
    private final Label milestoneTitle = new Label();
    private final Label rewardLabel = new Label();
    private final StackPane milestoneTrack = new StackPane();
    private final Region milestoneFill = new Region();
    private final Region milestoneGhost = new Region();
    private final StatRow speedRow = new StatRow();
    private final StatRow capacityRow = new StatRow();
    private final StatRow pulpiesRow = new StatRow();
    private final StatRow outputRow = new StatRow();
    private final HBox modeSwitch = new HBox();
    private final Button maxButton = new Button();
    private final Button buyButton = new Button();
    private final SequentialTransition levelPop;
    private final SequentialTransition targetPop;
    private final ParallelTransition floatUp;
    private final Timeline holdTimeline = new Timeline(new KeyFrame(HOLD_TICK, e -> holdTick()));
    private double holdInterval;
    private double untilNextBuy;
    private Station selected;

    /**
     * Creates the popup, hidden until {@link #show(Station)} is called.
     *
     * @param game       the game read to display the levels, stats and costs
     * @param controller receives the player's choices
     */
    public UpgradePanel(Game game, UpgradeController controller) {
        this.game = game;
        this.controller = controller;

        levelPop = createPop(levelBadge);
        targetPop = createPop(targetLabel);
        floatUp = createFloatUp(floatText);
        holdTimeline.setCycleCount(Animation.INDEFINITE);
        bottleneckBadge.getStyleClass().add("bottleneck-badge");

        VBox paper = new VBox(createLevelArea(), bottleneckBadge, createMilestone(), createStats(),
                createModeSwitch(), createBuyButton());
        paper.getStyleClass().add("paper");
        StackPane frame = new StackPane(paper);
        frame.getStyleClass().add("wood-frame");
        addNails(frame);

        title.getStyleClass().add("title-text");
        StackPane ribbon = new StackPane(title);
        ribbon.getStyleClass().add("title-ribbon");
        ribbon.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(ribbon, Pos.TOP_CENTER);
        StackPane.setMargin(ribbon, new Insets(-RIBBON_RISE, 0, 0, 0));

        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> hide());
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(-CLOSE_OFFSET, -CLOSE_OFFSET, 0, 0));

        StackPane card = new StackPane(frame, ribbon, closeButton);
        card.setPrefWidth(WIDTH);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        root = new StackPane(card);
        root.getStyleClass().add("overlay");
        root.setOnMouseClicked(e -> {
            if (e.getTarget() == root) {
                hide();
            }
        });
        root.setVisible(false);
    }

    /**
     * Opens the popup for the given station.
     *
     * @param station the station to upgrade
     */
    public void show(Station station) {
        selected = station;
        root.setVisible(true);
    }

    /**
     * Refreshes the level, the milestone bar, the stats before and after the purchase, the modes and the price.
     * Only changes texts, widths, styles and states, so it can run every frame.
     * Does nothing while the popup is hidden.
     */
    @Override
    public void render() {
        if (selected == null) {
            return;
        }
        BuyMode mode = controller.getMode();
        int levels = game.getLevelsToBuy(selected, mode);
        int level = selected.getLevel();
        title.setText(nameOf(selected).toUpperCase(Locale.ROOT));
        levelBadge.setText("LV " + NumberFormatter.format(level));
        targetLabel.setText("→ " + NumberFormatter.format(level + levels));
        setShown(targetLabel, levels > 0);
        setShown(bottleneckBadge, game.getBottleneck() == selected.getType());
        renderMilestone(level, levels);
        renderStats(levels);
        renderModes(mode);
        renderBuyButton(mode, levels);
    }

    /**
     * @return the node to add on top of the scene, filling the whole window
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * Returns the name shown to the player for a station, such as "Floor 3" or "Smoothie Bar".
     *
     * @param station the station to name
     * @return its display name
     */
    static String nameOf(Station station) {
        return switch (station.getType()) {
            case FLOOR -> "Floor " + (((Floor) station).getIndex() + 1);
            case ELEVATOR -> "Elevator";
            case SELL -> "Smoothie Bar";
        };
    }

    /**
     * The level badge, the target level of the purchase, and the "+10 LV" text floating over them.
     */
    private StackPane createLevelArea() {
        levelBadge.getStyleClass().add("level-badge");
        targetLabel.getStyleClass().add("level-target");
        HBox levels = new HBox(SPACING, levelBadge, targetLabel);
        levels.setAlignment(Pos.CENTER);
        floatText.getStyleClass().add("float-text");
        floatText.setOpacity(0);
        floatText.setMouseTransparent(true);
        return new StackPane(levels, floatText);
    }

    /**
     * The progress bar toward the next milestone, with the reward written above its end.
     * The part the purchase would add is drawn as a ghost behind the part already reached.
     */
    private VBox createMilestone() {
        milestoneTitle.getStyleClass().add("milestone-title");
        rewardLabel.getStyleClass().add("milestone-reward");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox header = new HBox(milestoneTitle, gap, rewardLabel);
        header.setAlignment(Pos.BOTTOM_LEFT);

        milestoneGhost.getStyleClass().add("milestone-ghost");
        milestoneFill.getStyleClass().add("milestone-fill");
        for (Region part : new Region[] {milestoneGhost, milestoneFill}) {
            part.setMinWidth(0);
            part.setMaxHeight(Double.MAX_VALUE);
        }
        milestoneTrack.getChildren().addAll(milestoneGhost, milestoneFill);
        milestoneTrack.getStyleClass().add("milestone-track");
        milestoneTrack.setAlignment(Pos.CENTER_LEFT);
        milestoneTrack.setMinWidth(BAR_WIDTH);
        milestoneTrack.setMaxWidth(BAR_WIDTH);

        VBox box = new VBox(SMALL_SPACING, header, milestoneTrack);
        box.setMaxWidth(BAR_WIDTH);
        return box;
    }

    /**
     * The stat lines, each one only shown for the stations it matters to.
     */
    private VBox createStats() {
        outputRow.box.getStyleClass().add("output-row");
        VBox box = new VBox(SMALL_SPACING, speedRow.box, capacityRow.box, pulpiesRow.box, outputRow.box);
        box.getStyleClass().add("stats-box");
        return box;
    }

    /**
     * The x1 / x10 / MAX pill: a click picks the mode and pops the target level.
     */
    private HBox createModeSwitch() {
        for (BuyMode mode : BuyMode.values()) {
            Button segment = mode == BuyMode.MAX ? maxButton : new Button(mode.name());
            segment.getStyleClass().add("mode-segment");
            segment.setUserData(mode);
            segment.setOnAction(e -> {
                controller.setMode(mode);
                targetPop.playFromStart();
            });
            modeSwitch.getChildren().add(segment);
        }
        modeSwitch.getStyleClass().add("mode-switch");
        modeSwitch.setMaxWidth(Region.USE_PREF_SIZE);
        return modeSwitch;
    }

    /**
     * The big green button: a press buys once, and holding it keeps buying.
     */
    private Button createBuyButton() {
        buyButton.getStyleClass().addAll("game-button", "green", "buy-button");
        CoinIcon.addTo(buyButton, COIN_SIZE);
        buyButton.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                startHold();
            }
        });
        buyButton.setOnMouseReleased(e -> holdTimeline.stop());
        return buyButton;
    }

    /**
     * Fills the bar up to the current level, and the ghost up to the level the purchase reaches.
     */
    private void renderMilestone(int level, int levels) {
        OptionalInt next = selected.getNextMilestone();
        if (next.isEmpty()) {
            milestoneTitle.setText("ALL MILESTONES");
            setShown(rewardLabel, false);
            setWidth(milestoneFill, BAR_WIDTH);
            setWidth(milestoneGhost, 0);
            milestoneTrack.pseudoClassStateChanged(UNLOCKED, false);
            return;
        }
        int goal = next.getAsInt();
        int start = previousMilestone(level);
        int target = level + levels;
        boolean crossed = levels > 0 && target >= goal;
        milestoneTitle.setText("NEXT  LV " + NumberFormatter.format(goal));
        setShown(rewardLabel, true);
        String reward = milestoneReward(goal - level);
        rewardLabel.setText(crossed ? "UNLOCKED! " + reward : reward);
        rewardLabel.pseudoClassStateChanged(UNLOCKED, crossed);
        milestoneTrack.pseudoClassStateChanged(UNLOCKED, crossed);
        double span = goal - start;
        setWidth(milestoneFill, BAR_WIDTH * (level - start) / span);
        setWidth(milestoneGhost, BAR_WIDTH * (Math.min(target, goal) - start) / span);
    }

    /**
     * Shows each stat of this kind of station now and after the purchase, with what it gains.
     */
    private void renderStats(int levels) {
        StationStats now = selected.previewStats(0);
        StationStats after = selected.previewStats(levels);
        StationType type = selected.getType();
        boolean elevator = type == StationType.ELEVATOR;
        setShown(speedRow.box, elevator);
        setShown(pulpiesRow.box, !elevator);
        if (elevator) {
            speedRow.update("SPEED", now.getSpeed(), after.getSpeed(), NumberFormatter::formatDecimal);
        } else {
            pulpiesRow.update("PULPIES", pulpiesAfter(0), pulpiesAfter(levels), NumberFormatter::format);
        }
        String capacityName = switch (type) {
            case FLOOR -> "CARRY";
            case ELEVATOR -> "CABIN";
            case SELL -> "SALE SIZE";
        };
        capacityRow.update(capacityName, now.getCapacity(), after.getCapacity(), NumberFormatter::format);
        outputRow.update("OUTPUT /s", selected.previewThroughput(0), selected.previewThroughput(levels),
                NumberFormatter::format);
    }

    /**
     * Highlights the chosen mode and writes how many levels MAX would buy.
     */
    private void renderModes(BuyMode mode) {
        for (Node segment : modeSwitch.getChildren()) {
            segment.pseudoClassStateChanged(SELECTED, segment.getUserData() == mode);
        }
        int maxLevels = game.getLevelsToBuy(selected, BuyMode.MAX);
        maxButton.setText(maxLevels > 0 ? "MAX ×" + NumberFormatter.format(maxLevels) : "MAX");
    }

    /**
     * Writes the price and greys out the button when the player cannot pay.
     */
    private void renderBuyButton(BuyMode mode, int levels) {
        if (levels == 0) {
            buyButton.setText("MAX LEVEL");
            buyButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
            buyButton.setDisable(true);
            return;
        }
        buyButton.setText("UPGRADE " + NumberFormatter.format(game.getUpgradeCost(selected, mode)));
        buyButton.setContentDisplay(ContentDisplay.RIGHT);
        buyButton.setDisable(!game.canUpgrade(selected, mode));
    }

    /**
     * Buys once, then keeps buying while the button is held, if the first purchase went through.
     */
    private void startHold() {
        if (!buyOnce()) {
            return;
        }
        holdInterval = HOLD_START_INTERVAL;
        untilNextBuy = HOLD_FIRST_DELAY;
        holdTimeline.playFromStart();
    }

    /**
     * Buys again when the wait is over, a bit sooner each time, and stops once the player cannot pay.
     */
    private void holdTick() {
        untilNextBuy -= HOLD_TICK.toSeconds();
        if (untilNextBuy > 0) {
            return;
        }
        if (selected == null || !buyOnce()) {
            holdTimeline.stop();
            return;
        }
        holdInterval = Math.max(HOLD_MIN_INTERVAL, holdInterval * HOLD_ACCELERATION);
        untilNextBuy = holdInterval;
    }

    /**
     * Buys the levels of the current mode through the controller and plays the feedback.
     */
    private boolean buyOnce() {
        UpgradeReport report = controller.onUpgrade(selected);
        if (report.levelsGained() == 0) {
            return false;
        }
        floatText.setText("+" + NumberFormatter.format(report.levelsGained()) + " LV");
        levelPop.playFromStart();
        floatUp.playFromStart();
        return true;
    }

    /** Closes the popup and stops buying. */
    private void hide() {
        holdTimeline.stop();
        selected = null;
        root.setVisible(false);
    }

    /** Returns the Pulpies the selected station would have after the purchase, 0 for the elevator. */
    private int pulpiesAfter(int levels) {
        return switch (selected.getType()) {
            case FLOOR -> ((Floor) selected).previewPulpieCount(levels);
            case SELL -> ((SellStation) selected).previewPulpieCount(levels);
            case ELEVATOR -> 0;
        };
    }

    /** Returns the last milestone reached, or level 1 before the first one, where the bar starts. */
    private int previousMilestone(int level) {
        int start = 1;
        for (int milestone : selected.getMilestoneLevels()) {
            if (milestone <= level) {
                start = milestone;
            }
        }
        return start;
    }

    /** Describes what the next milestone gives, such as "x2.16 CAPACITY +1 PULPIE". */
    private String milestoneReward(int levelsToGoal) {
        String reward = "x" + NumberFormatter.formatDecimal(selected.getMilestoneMultiplier()) + " CAPACITY";
        if (selected.getType() == StationType.ELEVATOR || levelsToGoal > selected.getLevelsLeft()) {
            return reward;
        }
        int gained = pulpiesAfter(levelsToGoal) - pulpiesAfter(0);
        return gained > 0 ? reward + " +" + NumberFormatter.format(gained) + " PULPIE" : reward;
    }

    /** Pops the node a bit bigger, then back to its real size. */
    private static SequentialTransition createPop(Node node) {
        ScaleTransition grow = new ScaleTransition(POP_GROW, node);
        grow.setFromX(1);
        grow.setFromY(1);
        grow.setToX(POP_SCALE);
        grow.setToY(POP_SCALE);
        ScaleTransition settle = new ScaleTransition(POP_SETTLE, node);
        settle.setToX(1);
        settle.setToY(1);
        return new SequentialTransition(grow, settle);
    }

    /** Moves the text up while it fades out. */
    private static ParallelTransition createFloatUp(Node node) {
        TranslateTransition rise = new TranslateTransition(FLOAT_DURATION, node);
        rise.setFromY(-FLOAT_START);
        rise.setToY(-FLOAT_START - FLOAT_RISE);
        FadeTransition fade = new FadeTransition(FLOAT_DURATION, node);
        fade.setFromValue(1);
        fade.setToValue(0);
        return new ParallelTransition(rise, fade);
    }

    /** Gives a bar part a fixed width. */
    private static void setWidth(Region part, double width) {
        part.setPrefWidth(width);
        part.setMaxWidth(width);
    }

    /** Shows or hides a node, and frees its space while hidden. */
    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /**
     * Pins the paper to the wood with a nail in each corner, like on a notice board.
     */
    private static void addNails(StackPane frame) {
        for (Pos corner : new Pos[] {Pos.TOP_LEFT, Pos.TOP_RIGHT, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT}) {
            Circle nail = new Circle(NAIL_RADIUS);
            nail.getStyleClass().add("nail");
            StackPane.setAlignment(nail, corner);
            StackPane.setMargin(nail, new Insets(NAIL_INSET));
            frame.getChildren().add(nail);
        }
    }

    /**
     * One stat line: its name, the value now, the value after the purchase, and a pill with the gain.
     * The value after and the pill are hidden when the purchase does not change the stat.
     */
    private static final class StatRow {

        private final HBox box;
        private final Label name = new Label();
        private final Label current = new Label();
        private final Label arrow = new Label("→");
        private final Label next = new Label();
        private final Label delta = new Label();

        /** Builds the empty line. */
        StatRow() {
            name.getStyleClass().add("stat-name");
            current.getStyleClass().add("stat-value");
            arrow.getStyleClass().add("stat-arrow");
            next.getStyleClass().add("stat-next");
            delta.getStyleClass().add("delta-pill");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            box = new HBox(SMALL_SPACING, name, current, arrow, next, gap, delta);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        /** Writes the stat now and after, such as "12.5K → 13.6K" with a "+1.12K (+9%)" pill. */
        void update(String label, double now, double after, DoubleFunction<String> format) {
            name.setText(label);
            current.setText(format.apply(now));
            boolean grows = after > now;
            for (Node node : new Node[] {arrow, next, delta}) {
                setShown(node, grows);
            }
            if (!grows) {
                return;
            }
            next.setText(format.apply(after));
            String gain = "+" + format.apply(after - now);
            delta.setText(now > 0
                    ? gain + " (+" + NumberFormatter.format((after / now - 1) * PERCENT) + "%)"
                    : gain);
        }
    }
}
