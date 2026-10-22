package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.station.Station;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Popup in the middle of the screen to hire the manager of one station, over a darkened background.
 * Shows the current manager with the color of its rarity and its bonus, and the result of the last roll.
 * Hiring again replaces the current manager, for a higher price each time.
 */
public class ShopView implements Renderable {

    private static final double SPACING = 10;
    private static final String OVERLAY_STYLE = "-fx-background-color: rgba(0, 0, 0, 0.6);";
    private static final String CARD_STYLE = "-fx-background-color: white; -fx-background-radius: 12;"
            + " -fx-padding: 20;";
    private static final String TITLE_STYLE = "-fx-font-size: 20px; -fx-font-weight: bold;";
    private static final String RARITY_STYLE = "-fx-font-weight: bold; -fx-text-fill: ";

    private final Game game;
    private final UpgradeController controller;
    private final StackPane root;
    private final Label titleLabel = new Label();
    private final Label managerLabel = new Label();
    private final Label rollLabel = new Label();
    private final Button hireButton = new Button();
    private Station selected;

    /**
     * Creates the popup, hidden until {@link #show(Station)} is called.
     *
     * @param game       the game read to display the prices
     * @param controller receives the player's choices
     */
    public ShopView(Game game, UpgradeController controller) {
        this.game = game;
        this.controller = controller;

        titleLabel.setStyle(TITLE_STYLE);
        hireButton.setOnAction(e -> hire());
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> hide());

        VBox card = new VBox(SPACING, titleLabel, managerLabel, hireButton, rollLabel, closeButton);
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
     * @param station the station whose manager is shown
     */
    public void show(Station station) {
        selected = station;
        rollLabel.setText("");
        root.setVisible(true);
    }

    /**
     * Refreshes the current manager and the price, and greys out the hire button when the player cannot pay.
     * Does nothing while the popup is hidden.
     */
    @Override
    public void render() {
        if (selected == null) {
            return;
        }
        titleLabel.setText(UpgradePanel.nameOf(selected) + " manager");
        Manager manager = selected.getManager();
        if (manager == null) {
            managerLabel.setText("No manager");
            managerLabel.setStyle("");
        } else {
            showManager(managerLabel, "Current: ", manager);
        }
        String replace = manager == null ? "" : " (replaces current)";
        hireButton.setText("Hire: " + NumberFormatter.format(game.getManagerCost(selected)) + " coins" + replace);
        hireButton.setDisable(!game.canHireManager(selected));
    }

    /**
     * @return the node to add on top of the scene, filling the whole window
     */
    public Parent getRoot() {
        return root;
    }

    private void hire() {
        Manager manager = controller.onHireManager(selected);
        if (manager != null) {
            showManager(rollLabel, "You got: ", manager);
        }
    }

    private static void showManager(Label label, String prefix, Manager manager) {
        label.setText(prefix + manager.getRarity() + " - " + manager.getAbility().describe());
        label.setStyle(RARITY_STYLE + manager.getRarity().getColorHex() + ";");
    }

    private void hide() {
        selected = null;
        root.setVisible(false);
    }
}
