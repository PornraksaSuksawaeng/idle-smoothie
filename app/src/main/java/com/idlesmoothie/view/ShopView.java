package com.idlesmoothie.view;

import java.util.Locale;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.manager.Rarity;
import com.idlesmoothie.model.station.Station;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Popup in the middle of the screen to hire the manager of one station, over a darkened background.
 * It has the same wooden frame, paper sheet, title ribbon and round close button as {@link SettingsView}.
 * Shows the current manager with the color of its rarity and its bonus, the chance of each rarity,
 * and reveals the rolled rarity with a short animation after a purchase.
 * Hiring again replaces the current manager, for a higher price each time.
 * The look comes from the {@code game-ui.css} stylesheet added to the scene.
 */
public class ShopView implements Renderable {

    private static final double WIDTH = 400;
    private static final double SPACING = 14;
    private static final double SMALL_SPACING = 6;
    private static final double PILL_NAME_GAP = 2;
    private static final double RIBBON_RISE = 26;
    private static final double CLOSE_OFFSET = 14;
    private static final double NAIL_RADIUS = 5;
    private static final double NAIL_INSET = 6;
    private static final double COIN_SIZE = 18;
    private static final double PERCENT = 100;
    // Below this gap a chance is shown as a whole number, such as 60% instead of 60.0%.
    private static final double WHOLE_PERCENT_TOLERANCE = 0.05;

    private static final double POP_SCALE = 1.15;
    private static final Duration POP_GROW = Duration.millis(220);
    private static final Duration POP_SETTLE = Duration.millis(140);
    private static final double GLOW_RADIUS = 18;
    private static final double GLOW_SPREAD = 0.35;
    private static final double STRONG_GLOW_RADIUS = 34;
    private static final double STRONG_GLOW_SPREAD = 0.6;

    // The inside of the 76px portrait, within its 5px border.
    private static final double PORTRAIT_SPRITE_HEIGHT = 66;

    private static final String QUESTION_MARK = "?";
    private static final String NO_MANAGER_HINT = "No manager: this station only works when you tap it.";
    private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");

    private final Game game;
    private final UpgradeController controller;
    private final StackPane root;
    private final Label stationLabel = new Label();
    private final StackPane portrait = new StackPane();
    private final Label portraitText = new Label();
    private final ImageView portraitImage = new ImageView();
    private final ManagerSprites managerSprites = ManagerSprites.of(PORTRAIT_SPRITE_HEIGHT);
    private final Label rarityBadge = new Label();
    private final Label bonusLabel = new Label();
    private final Label noManagerHint = hint(NO_MANAGER_HINT);
    private final Button hireButton = new Button();
    private final Label replaceHint = hint("Replaces your current manager.");
    private final Label revealBadge = new Label();
    private final VBox reveal;
    private final SequentialTransition pop;
    private Station selected;

    /**
     * Builds the manager portraits at the size the popup shows them, so the constructor finds them ready.
     * Safe to call off the JavaFX thread.
     */
    public static void preloadSprites() {
        ManagerSprites.of(PORTRAIT_SPRITE_HEIGHT);
    }

    /**
     * Creates the popup, hidden until {@link #show(Station)} is called.
     *
     * @param game       the game read to display the manager and the prices
     * @param controller receives the player's choices
     */
    public ShopView(Game game, UpgradeController controller) {
        this.game = game;
        this.controller = controller;

        stationLabel.getStyleClass().add("row-label");
        hireButton.getStyleClass().addAll("game-button", "green");
        hireButton.setOnAction(e -> hire());
        CoinIcon.addTo(hireButton, COIN_SIZE);
        reveal = createReveal();
        pop = createPop(reveal);

        VBox paper = new VBox(stationLabel, createManagerCard(), createOdds(), hireButton, replaceHint, reveal);
        paper.getStyleClass().add("paper");
        StackPane frame = new StackPane(paper);
        frame.getStyleClass().add("wood-frame");
        addNails(frame);

        Text title = new Text("MANAGER");
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
     * Opens the popup for the given station, without the result of a previous purchase.
     *
     * @param station the station whose manager is shown
     */
    public void show(Station station) {
        selected = station;
        pop.stop();
        setShown(reveal, false);
        root.setVisible(true);
    }

    /**
     * Refreshes the current manager and the price, and greys out the hire button when the player cannot pay.
     * Only changes texts, styles and states, so it can run every frame. Does nothing while the popup is hidden.
     */
    @Override
    public void render() {
        if (selected == null) {
            return;
        }
        stationLabel.setText(UpgradePanel.nameOf(selected));
        Manager manager = selected.getManager();
        boolean hired = manager != null;
        portrait.pseudoClassStateChanged(EMPTY, !hired);
        setShown(rarityBadge, hired);
        setShown(bonusLabel, hired);
        setShown(noManagerHint, !hired);
        setShown(replaceHint, hired);
        if (manager != null) {
            Rarity rarity = manager.getRarity();
            portrait.setStyle("-fx-border-color: " + rarity.getColorHex() + ";");
            portraitText.setText(rarity.name().substring(0, 1));
            portraitText.setStyle("-fx-text-fill: " + rarity.getColorHex() + ";");
            portraitImage.setImage(managerSprites.get(rarity));
            showRarity(rarityBadge, rarity);
            bonusLabel.setText(manager.getAbility().describe());
        } else {
            portrait.setStyle("");
            portraitText.setText(QUESTION_MARK);
            portraitText.setStyle("");
            portraitImage.setImage(null);
        }
        // the sprite replaces the letter; without a manager or a sheet, the letter or "?" stays
        portraitText.setVisible(portraitImage.getImage() == null);
        String action = hired ? "REPLACE " : "HIRE ";
        hireButton.setText(action + NumberFormatter.format(game.getManagerCost(selected)));
        hireButton.setDisable(!game.canHireManager(selected));
    }

    /**
     * @return the node to add on top of the scene, filling the whole window
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * The current manager: a portrait framed in the rarity color, then its rarity badge and its bonus.
     */
    private HBox createManagerCard() {
        portraitText.getStyleClass().add("portrait-text");
        portrait.getChildren().addAll(portraitImage, portraitText);
        portrait.getStyleClass().add("portrait");
        rarityBadge.getStyleClass().add("rarity-badge");
        bonusLabel.getStyleClass().add("bonus-text");
        noManagerHint.getStyleClass().add("card-hint");

        VBox info = new VBox(SMALL_SPACING, rarityBadge, bonusLabel, noManagerHint);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox card = new HBox(SPACING, portrait, info);
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    /**
     * One pill per rarity with its chance of being rolled, computed from the weights.
     */
    private static VBox createOdds() {
        double totalWeight = 0;
        for (Rarity rarity : Rarity.values()) {
            totalWeight += rarity.getWeight();
        }
        HBox pills = new HBox(SMALL_SPACING);
        pills.setAlignment(Pos.CENTER);
        for (Rarity rarity : Rarity.values()) {
            Label chance = new Label(formatPercent(rarity.getWeight() / totalWeight * PERCENT));
            chance.getStyleClass().add("odds-pill");
            chance.setStyle("-fx-background-color: " + rarity.getColorHex() + ";");
            Label name = new Label(rarity.name());
            name.getStyleClass().add("odds-name");
            VBox column = new VBox(PILL_NAME_GAP, chance, name);
            column.setAlignment(Pos.CENTER);
            pills.getChildren().add(column);
        }
        VBox odds = new VBox(SMALL_SPACING, hint("Chances"), pills);
        odds.setAlignment(Pos.CENTER);
        return odds;
    }

    /**
     * The "YOU GOT" block shown after a purchase, hidden until then.
     */
    private VBox createReveal() {
        Label youGot = new Label("YOU GOT");
        youGot.getStyleClass().add("row-label");
        revealBadge.getStyleClass().addAll("rarity-badge", "big");
        VBox box = new VBox(SMALL_SPACING, youGot, revealBadge);
        box.setAlignment(Pos.CENTER);
        setShown(box, false);
        return box;
    }

    /**
     * Pops the node from nothing to a bit too big, then back to its real size.
     */
    private static SequentialTransition createPop(Region node) {
        ScaleTransition grow = new ScaleTransition(POP_GROW, node);
        grow.setFromX(0);
        grow.setFromY(0);
        grow.setToX(POP_SCALE);
        grow.setToY(POP_SCALE);
        ScaleTransition settle = new ScaleTransition(POP_SETTLE, node);
        settle.setToX(1);
        settle.setToY(1);
        return new SequentialTransition(grow, settle);
    }

    /**
     * Buys a manager through the controller and reveals its rarity once.
     */
    private void hire() {
        Manager manager = controller.onHireManager(selected);
        if (manager == null) {
            return;
        }
        Rarity rarity = manager.getRarity();
        showRarity(revealBadge, rarity);
        boolean strong = rarity.compareTo(Rarity.LEGENDARY) >= 0;
        DropShadow glow = new DropShadow(strong ? STRONG_GLOW_RADIUS : GLOW_RADIUS, Color.web(rarity.getColorHex()));
        glow.setSpread(strong ? STRONG_GLOW_SPREAD : GLOW_SPREAD);
        revealBadge.setEffect(glow);
        setShown(reveal, true);
        pop.playFromStart();
    }

    /** Closes the popup and stops a reveal still playing. */
    private void hide() {
        selected = null;
        pop.stop();
        root.setVisible(false);
    }

    /** Writes the rarity name on a badge filled with its color. */
    private static void showRarity(Label badge, Rarity rarity) {
        badge.setText(rarity.name());
        badge.setStyle("-fx-background-color: " + rarity.getColorHex() + ";");
    }

    /** Shows a chance such as 60% or 0.5%. */
    private static String formatPercent(double percent) {
        boolean whole = Math.abs(percent - Math.rint(percent)) < WHOLE_PERCENT_TOLERANCE;
        return String.format(Locale.ROOT, whole ? "%.0f%%" : "%.1f%%", percent);
    }

    /** Shows or hides a node, and frees its space while hidden. */
    private static void setShown(Region node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /** Small centered grey text, as in the settings popup. */
    private static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
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
}
