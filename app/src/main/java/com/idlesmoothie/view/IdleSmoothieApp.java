package com.idlesmoothie.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application: builds the main window.
 */
public class IdleSmoothieApp extends Application {

    /**
     * Builds and shows the main window.
     *
     * @param stage the primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(new Label("Idle Smoothie"));
        stage.setTitle("Idle Smoothie");
        stage.setScene(new Scene(root, 540, 960));
        stage.show();
    }
}