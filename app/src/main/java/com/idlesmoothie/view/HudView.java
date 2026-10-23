package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Heads-up display at the top of the screen: a wooden pill with the player's coins,
 * and next to it a grey pill with a lightning bolt and what the mine earns per second.
 * It does not scroll with the mine and lets clicks go through to the mine below.
 * The look comes from the {@code game-ui.css} stylesheet added to the scene.
 */
public class HudView implements Renderable {

    private static final double SPACING = 8;
    private static final double COIN_SIZE = 32;
    private static final double SPEED_SIZE = 30;
    private static final Image SPEED_IMAGE = Assets.image("/images/ui/speed.png");

    private final Game game;
    private final Label coinsLabel = new Label();
    private final Label rateLabel = new Label();
    private final HBox root;

    /**
     * Creates the HUD.
     *
     * @param game the game whose coins and income are displayed
     */
    public HudView(Game game) {
        this.game = game;

        coinsLabel.getStyleClass().add("hud-text");
        HBox coins = pill("hud-coins", icon(CoinIcon.IMAGE, COIN_SIZE), coinsLabel);
        rateLabel.getStyleClass().add("hud-text");
        HBox rate = pill("hud-rate", icon(SPEED_IMAGE, SPEED_SIZE), rateLabel);

        root = new HBox(SPACING, coins, rate);
        root.setAlignment(Pos.CENTER);
        root.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);
        root.setMouseTransparent(true);
    }

    /**
     * Updates the coins and the income per second from the game.
     */
    @Override
    public void render() {
        coinsLabel.setText(NumberFormatter.format(game.getCoins()));
        rateLabel.setText("+" + NumberFormatter.format(game.getIncomePerSecond()) + " /s");
    }

    /**
     * @return the node to add to the scene, on top of the mine
     */
    public Parent getRoot() {
        return root;
    }

    /** An icon on the left of a pill, then its text. */
    private static HBox pill(String styleClass, ImageView icon, Label text) {
        HBox pill = new HBox(SPACING, icon, text);
        pill.getStyleClass().addAll("hud-pill", styleClass);
        pill.setAlignment(Pos.CENTER_LEFT);
        return pill;
    }

    /** A smoothly scaled square icon. */
    private static ImageView icon(Image image, double size) {
        ImageView icon = new ImageView(image);
        icon.setFitWidth(size);
        icon.setFitHeight(size);
        icon.setSmooth(true);
        return icon;
    }
}
