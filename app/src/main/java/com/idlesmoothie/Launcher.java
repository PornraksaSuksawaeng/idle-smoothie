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
        Application.launch(IdleSmoothieApp.class, args);
    }
}