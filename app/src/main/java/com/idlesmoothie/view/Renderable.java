package com.idlesmoothie.view;

/**
 * A view that redraws itself from the current state of the model, once per frame.
 */
public interface Renderable {

    /**
     * Redraws the view. Called by the {@link GameLoop} after the model was updated.
     */
    void render();
}
