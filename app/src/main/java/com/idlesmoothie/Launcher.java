package com.idlesmoothie;

import com.idlesmoothie.view.IdleSmoothieApp;
import javafx.application.Application;

/**
 * Entry point of the game.
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts the JavaFX application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Vsync is ignored on some Linux setups and leaves the game uncapped, saturating the GPU behind the popups,
        // so it is turned off and the pulse is capped at 120 FPS instead.
        System.setProperty("javafx.animation.pulse", "120");
        System.setProperty("prism.vsync", "false");
        Application.launch(IdleSmoothieApp.class, args);
    }
}