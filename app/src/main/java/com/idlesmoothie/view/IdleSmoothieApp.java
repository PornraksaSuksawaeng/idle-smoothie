package com.idlesmoothie.view;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.core.GameSnapshot;
import com.idlesmoothie.core.LoadingException;
import com.idlesmoothie.core.LoadingSteps;
import com.idlesmoothie.core.SaveManager;
import com.idlesmoothie.core.Settings;
import com.idlesmoothie.core.SettingsManager;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX application: shows the loading screen while the game loads, then the main window.
 * Loads the save at launch and saves on exit.
 */
public class IdleSmoothieApp extends Application {

    private static final double WIDTH = 540;
    private static final double HEIGHT = 960;
    private static final double HUD_MARGIN = 12;
    private static final double SETTINGS_ICON_SIZE = 30;
    private static final double HOVER_SCALE = 1.1;
    private static final Duration HOVER_DURATION = Duration.millis(150);
    private static final Duration FADE_DURATION = Duration.millis(500);
    private static final String FONT = "/fonts/LilitaOne-Regular.ttf";
    private static final double SIGN_FONT_SIZE = 16;

    private SaveManager saves;
    private SettingsManager settingsFile;
    private Settings settings;
    private MusicPlayer music;
    private Optional<GameSnapshot> snapshot = Optional.empty();
    private Game game;

    /**
     * Shows the loading screen, loads the game off the JavaFX thread, then fades into the game.
     *
     * @param stage the primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        LoadingScreen loading = new LoadingScreen();
        StackPane root = new StackPane(loading.getCanvas());
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/styles/game-ui.css").toExternalForm());
        stage.setTitle("Idle Smoothie");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        loading.start();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws LoadingException {
                LoadingSteps steps = createSteps();
                while (!steps.isDone()) {
                    updateMessage(steps.getCurrentName());
                    steps.runNext();
                    updateProgress(steps.getProgress(), 1);
                }
                return null;
            }
        };
        task.messageProperty().addListener((property, old, message) -> loading.setMessage(message));
        task.progressProperty().addListener((property, old, progress) -> loading.setProgress(progress.doubleValue()));
        task.setOnSucceeded(event -> showGame(root, loading));
        task.setOnFailed(event -> showError(loading, task.getException()));
        Thread thread = new Thread(task, "loading");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * The loading steps, in order. They run off the JavaFX thread: they only read files and build images,
     * never touch the scene.
     */
    private LoadingSteps createSteps() {
        return new LoadingSteps()
                .add("Reading your settings...", 0.5, this::loadSettings)
                .add("Tuning the music...", this::loadMusic)
                .add("Painting the signs...", 0.5, () -> Assets.font(FONT, SIGN_FONT_SIZE))
                .add("Waking up the Pulpies...", 6, MineView::preloadSprites)
                .add("Hiring the managers...", ShopView::preloadSprites)
                .add("Digging the mine...", 4, () -> Assets.preloadFolder("/images/mine"))
                .add("Polishing the coins...", 0.5, () -> Assets.preloadFolder("/images/ui"))
                .add("Opening your save...", 0.5, this::loadSave)
                .add("Mixing the first smoothie...", 0.5, this::createGame);
    }

    /**
     * Reads the settings, or keeps the default ones if the file is damaged.
     *
     * @throws IOException if the folder of the save and the settings cannot be created
     */
    private void loadSettings() throws IOException {
        Path folder = Path.of(System.getProperty("user.home"), ".idlesmoothie");
        Files.createDirectories(folder);
        saves = new SaveManager(folder.resolve("save.json"));
        settingsFile = new SettingsManager(folder.resolve("settings.json"));
        try {
            settings = settingsFile.load();
        } catch (IOException e) {
            System.err.println("Could not load the settings, using the default ones: " + e.getMessage());
            settings = new Settings();
        }
        NumberFormatter.setNotation(settings.getNotation());
    }

    /**
     * Loads the music and starts it right away, so it already plays on the loading screen.
     */
    private void loadMusic() {
        MusicPlayer player = new MusicPlayer();
        double volume = settings.getMusicVolume();
        boolean playing = settings.isMusic();
        Platform.runLater(() -> {
            player.setVolume(volume);
            player.setPlaying(playing);
        });
        music = player;
    }

    /**
     * Reads the save, or starts a new game if it is damaged.
     */
    private void loadSave() {
        try {
            snapshot = saves.load();
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not load the save, starting a new game: " + e.getMessage());
        }
    }

    /**
     * Builds the game from the save, or a new one if the save cannot be restored.
     */
    private void createGame() {
        Game loaded = new Game();
        if (snapshot.isPresent()) {
            try {
                loaded.restore(snapshot.get());
            } catch (IllegalArgumentException e) {
                System.err.println("Could not restore the save, starting a new game: " + e.getMessage());
                loaded = new Game();
            }
        }
        game = loaded;
    }

    /**
     * Builds the game screen under the loading screen, on the JavaFX thread, then fades into it once the bar is
     * full.
     */
    private void showGame(StackPane root, LoadingScreen loading) {
        GameLoop loop;
        try {
            UpgradeController controller = new UpgradeController(game);
            UpgradePanel upgradePanel = new UpgradePanel(game, controller);
            ShopView shopView = new ShopView(game, controller);
            MessageView messageView = new MessageView();
            MineView mineView = new MineView(WIDTH, HEIGHT, game, controller, upgradePanel, shopView, messageView,
                    settings);
            HudView hudView = new HudView(game);
            SettingsView settingsView = new SettingsView(settings, this::applySettings, this::saveSettings,
                    this::resetProgress);
            Button settingsButton = createSettingsButton(settingsView);
            ClickPulse clickPulse = new ClickPulse();
            // A filter, so the pulse plays even when a button or a popup handles the click.
            root.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED,
                    e -> clickPulse.play(e.getSceneX(), e.getSceneY()));
            StackPane.setAlignment(hudView.getRoot(), Pos.TOP_CENTER);
            StackPane.setMargin(hudView.getRoot(), new Insets(HUD_MARGIN));
            StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
            StackPane.setMargin(settingsButton, new Insets(HUD_MARGIN));
            root.getChildren().addAll(0, List.of(mineView.getCanvas(), hudView.getRoot(), settingsButton,
                    upgradePanel.getRoot(), shopView.getRoot(), settingsView.getRoot(), messageView.getRoot(),
                    clickPulse.getRoot()));
            loop = new GameLoop(game, List.of(mineView, hudView, upgradePanel, shopView), this::save);
        } catch (RuntimeException e) {
            showError(loading, e);
            return;
        }
        loading.finish(() -> {
            loop.start();
            FadeTransition fade = new FadeTransition(FADE_DURATION, loading.getCanvas());
            fade.setToValue(0);
            fade.setOnFinished(event -> {
                loading.stop();
                root.getChildren().remove(loading.getCanvas());
            });
            fade.play();
        });
    }

    /**
     * Shows what failed on the loading screen instead of leaving a blank window.
     */
    private static void showError(LoadingScreen loading, Throwable error) {
        error.printStackTrace();
        if (error instanceof LoadingException failed) {
            String step = failed.getStepName().replace("...", "");
            Throwable cause = failed.getCause();
            loading.showError("Oops! Stuck while " + Character.toLowerCase(step.charAt(0)) + step.substring(1) + ".",
                    describe(cause));
        } else {
            loading.showError("Oops! The game could not start.", describe(error));
        }
    }

    /**
     * @return the kind of error and its message, with a file path shortened to the file name
     */
    private static String describe(Throwable error) {
        String message = error.getMessage();
        if (error instanceof FileSystemException fileError && fileError.getFile() != null) {
            message = Path.of(fileError.getFile()).getFileName() + (fileError.getReason() == null
                    ? "" : ": " + fileError.getReason());
        }
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    /**
     * Saves the game when the window is closed, if it was loaded.
     */
    @Override
    public void stop() {
        save();
    }

    private void save() {
        if (game == null || saves == null) {
            return;
        }
        try {
            saves.save(game.createSnapshot());
        } catch (IOException e) {
            System.err.println("Could not save the game: " + e.getMessage());
        }
    }

    /**
     * Applies the settings the player just changed.
     */
    private void applySettings() {
        NumberFormatter.setNotation(settings.getNotation());
        music.setVolume(settings.getMusicVolume());
        music.setPlaying(settings.isMusic());
    }

    /**
     * Writes the settings to their file, so they come back at the next launch.
     */
    private void saveSettings() {
        try {
            settingsFile.save(settings);
        } catch (IOException e) {
            System.err.println("Could not save the settings: " + e.getMessage());
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
        Image image = Assets.image("/images/ui/settings.png");
        if (image == null) {
            return new Label("Settings");
        }
        ImageView icon = new ImageView(image);
        icon.setFitWidth(SETTINGS_ICON_SIZE);
        icon.setFitHeight(SETTINGS_ICON_SIZE);
        icon.setSmooth(true);
        return icon;
    }
}
