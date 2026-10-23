package com.idlesmoothie.view;

import java.util.function.Consumer;
import java.util.function.Function;

import com.idlesmoothie.core.NumberNotation;
import com.idlesmoothie.core.Settings;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

/**
 * Settings popup in the middle of the screen, over a darkened background:
 * a wooden frame around a sheet of paper, with a green title ribbon and a round close button.
 * It turns the music and the glow of the managers on or off, sets the music volume, picks how numbers are written,
 * and resets the progress after asking the player to confirm since it cannot be undone.
 * The look comes from the {@code game-ui.css} stylesheet added to the scene.
 */
public class SettingsView {

    private static final double WIDTH = 400;
    private static final double SPACING = 14;
    private static final double RIBBON_RISE = 26;
    private static final double CLOSE_OFFSET = 14;
    private static final double NAIL_RADIUS = 5;
    private static final double NAIL_INSET = 6;
    private static final double VOLUME_SLIDER_WIDTH = 140;
    private static final double VOLUME_TEXT_WIDTH = 60;
    private static final double PERCENT = 100;
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final Settings settings;
    private final Runnable onChange;
    private final Runnable onSave;
    private final Runnable onReset;
    private final StackPane root;
    private final Text title = new Text();
    private final VBox paper = new VBox();
    private final VBox menu;
    private final VBox confirmation;

    /**
     * Creates the popup, hidden until {@link #show()} is called.
     *
     * @param settings the preferences changed by the popup
     * @param onChange applies a setting as soon as it changes, even while the volume slider is dragged
     * @param onSave   saves the settings, once a change is done
     * @param onReset  what to do once the player confirmed the reset
     */
    public SettingsView(Settings settings, Runnable onChange, Runnable onSave, Runnable onReset) {
        this.settings = settings;
        this.onChange = onChange;
        this.onSave = onSave;
        this.onReset = onReset;
        menu = createMenu();
        confirmation = createConfirmation();

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

        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> hide());
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(-CLOSE_OFFSET, -CLOSE_OFFSET, 0, 0));

        StackPane card = new StackPane(frame, ribbon, closeButton);
        card.setPrefWidth(WIDTH);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        root = new StackPane(card);
        root.getStyleClass().add("overlay");
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

    /**
     * The settings themselves, one row each: a label on the left and its control on the right.
     */
    private VBox createMenu() {
        HBox musicSwitch = choiceSwitch(new Boolean[] {true, false}, on -> on ? "ON" : "OFF",
                settings.isMusic(), settings::setMusic);
        HBox auraSwitch = choiceSwitch(new Boolean[] {true, false}, on -> on ? "ON" : "OFF",
                settings.isManagerAura(), settings::setManagerAura);
        HBox notationSwitch = choiceSwitch(NumberNotation.values(),
                notation -> notation == NumberNotation.SUFFIXES ? "1.5K" : "1.5e3",
                settings.getNotation(), settings::setNotation);

        Button resetButton = gameButton("RESET", "red");
        resetButton.setOnAction(e -> showConfirmation(true));
        Label hint = hint("Start over from a single floor. Your coins, levels and managers are lost.");
        Region separator = new Region();
        separator.getStyleClass().add("separator-line");

        VBox box = new VBox(SPACING, row("MUSIC", musicSwitch), row("VOLUME", createVolumeSlider()),
                row("MANAGER AURA", auraSwitch), row("NUMBERS", notationSwitch), separator, row("PROGRESS", resetButton), hint);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    private VBox createConfirmation() {
        Label question = new Label("Reset all your progress?");
        question.getStyleClass().add("row-label");
        Label warning = hint("This cannot be undone.");

        Button cancelButton = gameButton("CANCEL", "brown");
        cancelButton.setOnAction(e -> showConfirmation(false));
        Button confirmButton = gameButton("RESET", "red");
        confirmButton.setOnAction(e -> reset());
        HBox choices = new HBox(SPACING, cancelButton, confirmButton);
        choices.setAlignment(Pos.CENTER);

        VBox box = new VBox(SPACING, question, warning, choices);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    private void showConfirmation(boolean shown) {
        title.setText(shown ? "RESET?" : "SETTINGS");
        paper.getChildren().setAll(shown ? confirmation : menu);
    }

    private void reset() {
        onReset.run();
        hide();
    }

    private void hide() {
        root.setVisible(false);
    }

    private static HBox row(String text, Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("row-label");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(label, gap, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * The music volume, from 0% to 100%, with its value written next to it.
     * The volume changes while the slider is dragged, and is saved once it is released.
     */
    private HBox createVolumeSlider() {
        Slider slider = new Slider(0, 1, settings.getMusicVolume());
        slider.setPrefWidth(VOLUME_SLIDER_WIDTH);
        Label value = new Label(percent(slider.getValue()));
        value.getStyleClass().add("row-label");
        // Wide enough for "100%", so the slider does not move when the number gets longer.
        value.setMinWidth(VOLUME_TEXT_WIDTH);
        value.setMaxWidth(VOLUME_TEXT_WIDTH);
        value.setAlignment(Pos.CENTER_RIGHT);

        slider.valueProperty().addListener((property, before, volume) -> {
            settings.setMusicVolume(volume.doubleValue());
            value.setText(percent(volume.doubleValue()));
            onChange.run();
            if (!slider.isValueChanging()) {
                onSave.run();
            }
        });
        slider.valueChangingProperty().addListener((property, wasChanging, changing) -> {
            if (!changing) {
                onSave.run();
            }
        });

        HBox box = new HBox(SPACING / 2, slider, value);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private static String percent(double volume) {
        return Math.round(volume * PERCENT) + "%";
    }

    /**
     * A pill of choices, the current one in green. A click selects a choice, stores it, then applies and saves it.
     *
     * @param choices the values to pick from, in order
     * @param text    the text of each choice
     * @param current the choice selected at first
     * @param apply   stores the picked choice in the settings
     */
    private <T> HBox choiceSwitch(T[] choices, Function<T, String> text, T current,
            Consumer<T> apply) {
        HBox pill = new HBox();
        for (T choice : choices) {
            Button segment = new Button(text.apply(choice));
            segment.getStyleClass().add("mode-segment");
            segment.pseudoClassStateChanged(SELECTED, choice.equals(current));
            segment.setOnAction(e -> {
                pill.getChildren().forEach(other -> other.pseudoClassStateChanged(SELECTED, other == segment));
                apply.accept(choice);
                onChange.run();
                onSave.run();
            });
            pill.getChildren().add(segment);
        }
        pill.getStyleClass().add("mode-switch");
        pill.setMaxWidth(Region.USE_PREF_SIZE);
        return pill;
    }

    private static Button gameButton(String text, String color) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", color);
        return button;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
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