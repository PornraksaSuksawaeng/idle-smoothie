package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.economy.BuyMode;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Station;
import com.idlesmoothie.model.station.StationStats;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Popup in the middle of the screen to upgrade one station, over a darkened background.
 * Shows the level and stats of the station, lets the player pick x1 / x10 / MAX and buy.
 * Clicking the dark background or the close button hides it.
 */
public class UpgradePanel implements Renderable {

    private static final double SPACING = 10;
    private static final String OVERLAY_STYLE = "-fx-background-color: rgba(0, 0, 0, 0.6);";
    private static final String CARD_STYLE = "-fx-background-color: white; -fx-background-radius: 12;"
            + " -fx-padding: 20;";
    private static final String TITLE_STYLE = "-fx-font-size: 20px; -fx-font-weight: bold;";
    private static final String SELECTED_MODE_STYLE = "-fx-base: #3E8E41;";

    private final Game game;
    private final UpgradeController controller;
    private final StackPane root;
    private final Label titleLabel = new Label();
    private final Label levelLabel = new Label();
    private final Label statsLabel = new Label();
    private final Button upgradeButton = new Button();
    private final HBox modeButtons = new HBox(SPACING);
    private Station selected;

    /**
     * Creates the popup, hidden until {@link #show(Station)} is called.
     *
     * @param game       the game read to display the costs
     * @param controller receives the player's choices
     */
    public UpgradePanel(Game game, UpgradeController controller) {
        this.game = game;
        this.controller = controller;

        titleLabel.setStyle(TITLE_STYLE);
        for (BuyMode mode : BuyMode.values()) {
            Button button = new Button(mode.name());
            button.setOnAction(e -> controller.setMode(mode));
            button.setUserData(mode);
            modeButtons.getChildren().add(button);
        }
        modeButtons.setAlignment(Pos.CENTER);
        upgradeButton.setOnAction(e -> controller.onUpgrade(selected));
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> hide());

        VBox card = new VBox(SPACING, titleLabel, levelLabel, statsLabel, modeButtons, upgradeButton, closeButton);
        card.setAlignment(Pos.CENTER);
        card.setStyle(CARD_STYLE);
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);

        root = new StackPane(card);
        root.setStyle(OVERLAY_STYLE);
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
     * Refreshes the texts and greys out the upgrade button when the player cannot pay.
     * Does nothing while the popup is hidden.
     */
    @Override
    public void render() {
        if (selected == null) {
            return;
        }
        BuyMode mode = controller.getMode();
        StationStats stats = selected.getStats();
        titleLabel.setText(nameOf(selected));
        levelLabel.setText("Level " + selected.getLevel());
        statsLabel.setText(String.format("Speed: %.2f   Capacity: %.1f%nThroughput: %.1f / s",
                stats.getSpeed(), stats.getCapacity(), selected.getThroughput()));
        upgradeButton.setText(String.format("Upgrade %s: %.0f coins", mode.name(), game.getUpgradeCost(selected, mode)));
        upgradeButton.setDisable(!game.canUpgrade(selected, mode));
        for (var node : modeButtons.getChildren()) {
            node.setStyle(node.getUserData() == mode ? SELECTED_MODE_STYLE : "");
        }
    }

    /**
     * @return the node to add on top of the scene, filling the whole window
     */
    public Parent getRoot() {
        return root;
    }

    private void hide() {
        selected = null;
        root.setVisible(false);
    }

    private static String nameOf(Station station) {
        return switch (station.getType()) {
            case FLOOR -> "Floor " + (((Floor) station).getIndex() + 1);
            case ELEVATOR -> "Elevator";
            case SELL -> "Smoothie Bar";
        };
    }
}
