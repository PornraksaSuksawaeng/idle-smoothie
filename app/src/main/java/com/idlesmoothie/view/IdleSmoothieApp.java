package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application: builds the main window.
 */
public class IdleSmoothieApp extends Application {

    /**
     * Creates a new game, shows the main window and starts the game loop.
     *
     * @param stage the primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        Game game = new Game();
        MineView mineView = new MineView(540, 960, game);
        StackPane root = new StackPane(mineView.getCanvas());
        stage.setTitle("Idle Smoothie");
        stage.setScene(new Scene(root, 540, 960));
        stage.setResizable(false);
        stage.show();

        new GameLoop(game, List.of(mineView)).start();
    }
}
