package com.idlesmoothie.view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * White ring that grows and fades where the player clicks, drawn over the whole window.
 * It never catches the clicks itself, so everything below still receives them.
 */
public class ClickPulse {

    private static final double RADIUS = 8;
    private static final double STROKE_WIDTH = 3;
    private static final double END_SCALE = 3.5;
    private static final double START_OPACITY = 0.9;
    private static final Duration DURATION = Duration.millis(350);

    private final Pane root = new Pane();

    /**
     * Creates the layer, empty until {@link #play(double, double)} is called.
     */
    public ClickPulse() {
        root.setMouseTransparent(true);
    }

    /**
     * Plays one pulse centered on a point of the window. The ring is removed once it has faded.
     *
     * @param x the x of the click in the window
     * @param y the y of the click in the window
     */
    public void play(double x, double y) {
        Circle ring = new Circle(x, y, RADIUS, Color.TRANSPARENT);
        ring.setStroke(Color.WHITE);
        ring.setStrokeWidth(STROKE_WIDTH);
        ring.setOpacity(START_OPACITY);
        root.getChildren().add(ring);

        ScaleTransition grow = new ScaleTransition(DURATION, ring);
        grow.setToX(END_SCALE);
        grow.setToY(END_SCALE);
        FadeTransition fade = new FadeTransition(DURATION, ring);
        fade.setToValue(0);
        ParallelTransition pulse = new ParallelTransition(grow, fade);
        pulse.setOnFinished(e -> root.getChildren().remove(ring));
        pulse.play();
    }

    /**
     * @return the node to add on top of the scene, filling the whole window
     */
    public Parent getRoot() {
        return root;
    }
}
