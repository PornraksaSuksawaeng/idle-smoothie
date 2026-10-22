package com.idlesmoothie.view;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Settings popup in the middle of the screen, over a darkened background.
 * For now it only resets the progress, after asking the player to confirm since it cannot be undone.
 */
public class SettingsView {

    private static final double SPACING = 10;
    private static final String OVERLAY_STYLE = "-fx-background-color: rgba(0, 0, 0, 0.6);";
    private static final String CARD_STYLE = "-fx-background-color: white; -fx-background-radius: 12;"
            + " -fx-padding: 20;";
    private static final String TITLE_STYLE = "-fx-font-size: 20px; -fx-font-weight: bold;";
    private static final String DANGER_STYLE = "-fx-base: #D9534F;";

    private final Runnable onReset;
    private final StackPane root;
    private final VBox card = new VBox(SPACING);
    private final VBox menu;
    private final VBox confirmation;

    /**
     * Creates the popup, hidden until {@link #show()} is called.
     *
     * @param onReset what to do once the player confirmed the reset
     */
    public SettingsView(Runnable onReset) {
        this.onReset = onReset;

        Label title = new Label("Settings");
        title.setStyle(TITLE_STYLE);
        Button resetButton = new Button("Reset progress");
        resetButton.setStyle(DANGER_STYLE);
        resetButton.setOnAction(e -> showConfirmation(true));
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> hide());
        menu = new VBox(SPACING, title, resetButton, closeButton);
        menu.setAlignment(Pos.CENTER);

        Label warning = new Label("All your progress will be lost.\nAre you sure?");
        warning.setTextAlignment(TextAlignment.CENTER);
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> showConfirmation(false));
        Button confirmButton = new Button("Yes, reset");
        confirmButton.setStyle(DANGER_STYLE);
        confirmButton.setOnAction(e -> reset());
        HBox choices = new HBox(SPACING, cancelButton, confirmButton);
        choices.setAlignment(Pos.CENTER);
        confirmation = new VBox(SPACING, warning, choices);
        confirmation.setAlignment(Pos.CENTER);

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
     * Opens the popup on the settings menu.
     */
    public void show() {
        showConfirmation(false);
        root.setVisible(true);
    }

    /**
     * @return the node to add on top of the scene, filling the whole window
     */
    public Parent getRoot() {
        return root;
    }

    private void showConfirmation(boolean shown) {
        card.getChildren().setAll(shown ? confirmation : menu);
    }

    private void reset() {
        onReset.run();
        hide();
    }

    private void hide() {
        root.setVisible(false);
    }
}