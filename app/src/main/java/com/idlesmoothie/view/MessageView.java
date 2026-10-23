package com.idlesmoothie.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Simple message popup in the middle of the screen, over a darkened background:
 * the same wooden frame and title ribbon as the settings, with a text and an "OK" button that closes it.
 * The look comes from the {@code game-ui.css} stylesheet added to the scene.
 */
public class MessageView {

    private static final double WIDTH = 360;
    private static final double SPACING = 14;
    private static final double RIBBON_RISE = 26;
    private static final double NAIL_RADIUS = 5;
    private static final double NAIL_INSET = 6;

    private final StackPane root;
    private final Text title = new Text();
    private final Label message = new Label();

    /**
     * Creates the popup, hidden until {@link #show(String, String)} is called.
     */
    public MessageView() {
        message.getStyleClass().add("row-label");
        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        message.setAlignment(Pos.CENTER);
        message.setTextAlignment(TextAlignment.CENTER);
        Button okButton = new Button("OK");
        okButton.getStyleClass().addAll("game-button", "green");
        okButton.setOnAction(e -> hide());

        VBox paper = new VBox(SPACING, message, okButton);
        paper.setAlignment(Pos.TOP_CENTER);
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

        StackPane card = new StackPane(frame, ribbon);
        card.setPrefWidth(WIDTH);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        root = new StackPane(card);
        root.getStyleClass().add("overlay");
        root.setVisible(false);
    }

    /**
     * Opens the popup with the given title and message.
     *
     * @param titleText   the text of the ribbon
     * @param messageText the text shown on the paper
     */
    public void show(String titleText, String messageText) {
        title.setText(titleText);
        message.setText(messageText);
        root.setVisible(true);
    }

    /**
     * @return the node to add on top of the scene, filling the whole window
     */
    public Parent getRoot() {
        return root;
    }

    private void hide() {
        root.setVisible(false);
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
