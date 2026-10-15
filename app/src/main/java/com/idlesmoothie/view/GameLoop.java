package com.idlesmoothie.view;

import com.idlesmoothie.model.station.Station;
import java.util.List;
import javafx.animation.AnimationTimer;

/**
 * Game clock: on every frame, moves the model forward by the elapsed time, then redraws the views.
 */
public class GameLoop extends AnimationTimer {

    private static final double NANOS_PER_SECOND = 1e9;
    private static final double MAX_DT = 0.1;

    private final List<Station> stations;   // TODO replace with Game once core.Game exists
    private final List<Renderable> views;
    private long lastNanos;

    /**
     * Creates the loop. Call {@link #start()} to run it.
     *
     * @param stations stations updated on every frame, in order
     * @param views    views redrawn on every frame, after the update
     */
    public GameLoop(List<Station> stations, List<Renderable> views) {
        this.stations = stations;
        this.views = views;
    }

    /**
     * Called by JavaFX on every frame. The elapsed time is capped so that a frozen window
     * does not make the model jump forward.
     *
     * @param now current time in nanoseconds
     */
    @Override
    public void handle(long now) {
        double dt = lastNanos == 0 ? 0 : Math.min((now - lastNanos) / NANOS_PER_SECOND, MAX_DT);
        lastNanos = now;
        for (Station station : stations) {
            station.update(dt);
        }
        for (Renderable view : views) {
            view.render();
        }
    }
}
