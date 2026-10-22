package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Heads-up display at the top of the screen: the player's coins and the station that limits the income.
 * It does not scroll with the mine and lets clicks go through to the mine below.
 */
public class HudView implements Renderable {

    private static final double SPACING = 4;
    private static final String STYLE = "-fx-background-color: rgba(0, 0, 0, 0.6);"
            + " -fx-background-radius: 8; -fx-padding: 8 16;";
    private static final String TEXT_STYLE = "-fx-text-fill: white; -fx-font-size: 14px;";

    private final Game game;
    private final Label coinsLabel = new Label();
    private final Label bottleneckLabel = new Label();
    private final VBox root;

    /**
     * Creates the HUD.
     *
     * @param game the game whose coins and bottleneck are displayed
     */
    public HudView(Game game) {
        this.game = game;
        coinsLabel.setStyle(TEXT_STYLE + " -fx-font-size: 20px; -fx-font-weight: bold;");
        bottleneckLabel.setStyle(TEXT_STYLE);
        root = new VBox(SPACING, coinsLabel, bottleneckLabel);
        root.setAlignment(Pos.CENTER);
        root.setStyle(STYLE);
        root.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        root.setMouseTransparent(true);
    }

    /**
     * Updates the labels from the game.
     */
    @Override
    public void render() {
        coinsLabel.setText(NumberFormatter.format(game.getCoins()) + " coins");
        bottleneckLabel.setText("Bottleneck: " + game.getBottleneck());
    }

    /**
     * @return the node to add to the scene, on top of the mine
     */
    public Parent getRoot() {
        return root;
    }
}
