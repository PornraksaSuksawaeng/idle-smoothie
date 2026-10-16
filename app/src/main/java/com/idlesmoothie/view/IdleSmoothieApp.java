package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application: builds the main window.
 */
public class IdleSmoothieApp extends Application {

    private static final double HUD_MARGIN = 12;

    /**
     * Creates a new game, shows the main window and starts the game loop.
     *
     * @param stage the primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        Game game = new Game();
        UpgradeController controller = new UpgradeController(game);
        UpgradePanel upgradePanel = new UpgradePanel(game, controller);
        MineView mineView = new MineView(540, 960, game, controller, upgradePanel);
        HudView hudView = new HudView(game);

        StackPane root = new StackPane(mineView.getCanvas(), hudView.getRoot(), upgradePanel.getRoot());
        StackPane.setAlignment(hudView.getRoot(), Pos.TOP_CENTER);
        StackPane.setMargin(hudView.getRoot(), new Insets(HUD_MARGIN));
        stage.setTitle("Idle Smoothie");
        stage.setScene(new Scene(root, 540, 960));
        stage.setResizable(false);
        stage.show();

        new GameLoop(game, List.of(mineView, hudView, upgradePanel)).start();
    }
}
