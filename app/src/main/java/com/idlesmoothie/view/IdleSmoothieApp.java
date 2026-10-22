package com.idlesmoothie.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.core.SaveManager;

import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX application: builds the main window, loads the save at launch and saves on exit.
 */
public class IdleSmoothieApp extends Application {

    private static final double HUD_MARGIN = 12;
    private static final double SETTINGS_ICON_SIZE = 30;
    // Turns the gear white, like the texts of the HUD: no color, much brighter.
    private static final double GEAR_SATURATION = -1;
    private static final double GEAR_BRIGHTNESS = 0.75;
    private static final double HOVER_SCALE = 1.1;
    private static final Duration HOVER_DURATION = Duration.millis(150);

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
        Button settingsButton = createSettingsButton(settingsView);

        StackPane root = new StackPane(mineView.getCanvas(), hudView.getRoot(), settingsButton,
                upgradePanel.getRoot(), shopView.getRoot(), settingsView.getRoot());
        StackPane.setAlignment(hudView.getRoot(), Pos.TOP_CENTER);
        StackPane.setMargin(hudView.getRoot(), new Insets(HUD_MARGIN));
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(HUD_MARGIN));
        stage.setTitle("Idle Smoothie");
        Scene scene = new Scene(root, 540, 960);
        scene.getStylesheets().add(getClass().getResource("/styles/game-ui.css").toExternalForm());
        stage.setScene(scene);
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

    /**
     * Creates the gear button that opens the settings, which smoothly grows a little under the mouse.
     */
    private Button createSettingsButton(SettingsView settingsView) {
        Button button = new Button();
        button.setGraphic(settingsIcon());
        button.getStyleClass().add("hud-round-button");
        ScaleTransition zoom = new ScaleTransition(HOVER_DURATION, button);
        button.setOnMouseEntered(e -> zoomTo(zoom, HOVER_SCALE));
        button.setOnMouseExited(e -> zoomTo(zoom, 1));
        button.setOnAction(e -> settingsView.show());
        return button;
    }

    /**
     * Animates the node from its current size to the given scale, even if the previous animation was not finished.
     */
    private static void zoomTo(ScaleTransition zoom, double scale) {
        zoom.stop();
        zoom.setToX(scale);
        zoom.setToY(scale);
        zoom.play();
    }
    /**
     * @return the gear icon, or a text if the image is missing
     */
    private Node settingsIcon() {
        InputStream stream = getClass().getResourceAsStream("/images/ui/settings.png");
        if (stream == null) {
            return new Label("Settings");
        }
        ImageView icon = new ImageView(new Image(stream));
        icon.setFitWidth(SETTINGS_ICON_SIZE);
        icon.setFitHeight(SETTINGS_ICON_SIZE);
        icon.setSmooth(true);
        icon.setEffect(new ColorAdjust(0, GEAR_SATURATION, GEAR_BRIGHTNESS, 0));
        return icon;
    }
}