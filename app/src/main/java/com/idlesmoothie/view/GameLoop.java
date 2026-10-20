package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import java.util.List;
import javafx.animation.AnimationTimer;

/**
 * Game clock: on every frame, moves the game forward by the elapsed time, then redraws the views.
 * It also saves the game every {@value #SAVE_INTERVAL} seconds, in case the game crashes.
 */
public class GameLoop extends AnimationTimer {

    private static final double NANOS_PER_SECOND = 1e9;
    private static final double MAX_DT = 0.1;
    private static final double SAVE_INTERVAL = 30;

    private final Game game;
    private final List<Renderable> views;
    private final Runnable save;
    private long lastNanos;
    private double sinceLastSave;

    /**
     * Creates the loop. Call {@link #start()} to run it.
     *
     * @param game  the game updated on every frame
     * @param views views redrawn on every frame, after the update
     * @param save  saves the game, called every {@value #SAVE_INTERVAL} seconds
     */
    public GameLoop(Game game, List<Renderable> views, Runnable save) {
        this.game = game;
        this.views = views;
        this.save = save;
    }

    /**
     * Called by JavaFX on every frame. The elapsed time is capped so that a frozen window
     * does not make the game jump forward.
     *
     * @param now current time in nanoseconds
     */
    @Override
    public void handle(long now) {
        double dt = lastNanos == 0 ? 0 : Math.min((now - lastNanos) / NANOS_PER_SECOND, MAX_DT);
        lastNanos = now;
        game.update(dt);
        sinceLastSave += dt;
        if (sinceLastSave >= SAVE_INTERVAL) {
            sinceLastSave = 0;
            save.run();
        }
        for (Renderable view : views) {
            view.render();
        }
    }
}
