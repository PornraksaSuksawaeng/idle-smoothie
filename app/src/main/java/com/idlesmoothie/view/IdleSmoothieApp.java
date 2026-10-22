package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.core.SaveManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application: builds the main window, loads the save at launch and saves on exit.
 */
public class IdleSmoothieApp extends Application {

    private static final double HUD_MARGIN = 12;

    private SaveManager saves;
    private Game game;

    /**
     * Creates the game from the save if there is one, shows the main window and starts the game loop.
     *
     * @param stage the primary stage provided by JavaFX
     * @throws IOException if the save folder cannot be created
     */
    @Override
    public void start(Stage stage) throws IOException {
        Path file = Path.of(System.getProperty("user.home"), ".idlesmoothie", "save.json");
        Files.createDirectories(file.getParent());
        saves = new SaveManager(file);
        game = new Game();
        try {
            saves.load().ifPresent(game::restore);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not load the save, starting a new game: " + e.getMessage());
        }

        UpgradeController controller = new UpgradeController(game);
        UpgradePanel upgradePanel = new UpgradePanel(game, controller);
        ShopView shopView = new ShopView(game, controller);
        MineView mineView = new MineView(540, 960, game, controller, upgradePanel, shopView);
        HudView hudView = new HudView(game);
        SettingsView settingsView = new SettingsView(this::resetProgress);
        Button settingsButton = new Button("⚙");
        settingsButton.setStyle("-fx-font-size: 18px;");
        settingsButton.setOnAction(e -> settingsView.show());

        StackPane root = new StackPane(mineView.getCanvas(), hudView.getRoot(), settingsButton,
                upgradePanel.getRoot(), shopView.getRoot(), settingsView.getRoot());
        StackPane.setAlignment(hudView.getRoot(), Pos.TOP_CENTER);
        StackPane.setMargin(hudView.getRoot(), new Insets(HUD_MARGIN));
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(HUD_MARGIN));
        stage.setTitle("Idle Smoothie");
        stage.setScene(new Scene(root, 540, 960));
        stage.setResizable(false);
        stage.show();

        new GameLoop(game, List.of(mineView, hudView, upgradePanel, shopView), this::save).start();
    }

    /**
     * Saves the game when the window is closed.
     */
    @Override
    public void stop() {
        save();
    }

    private void save() {
        try {
            saves.save(game.createSnapshot());
        } catch (IOException e) {
            System.err.println("Could not save the game: " + e.getMessage());
        }
    }

    /**
     * Starts a new game and deletes the save, so the old progress does not come back at the next launch.
     */
    private void resetProgress() {
        game.reset();
        try {
            saves.delete();
        } catch (IOException e) {
            System.err.println("Could not delete the save: " + e.getMessage());
        }
    }
}