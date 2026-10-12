package com.idlesmoothie.model.creature;

/**
 * One step of a Pulpie trip, such as walking to the extractor or depositing smoothie.
 * Implements the State pattern: the Pulpie delegates what happens at each step to its current state.
 */
public interface PulpieState {

    /**
     * @return the share of the whole trip this state takes, such as {@code 0.4}
     */
    public double getShare();

    /**
     * Applies what happens when the state is over, such as filling the bag.
     *
     * @param pulpie the Pulpie leaving this state
     */
    public void finish(Pulpie pulpie);

    /**
     * @return the state that comes after this one, or {@code null} when the trip is over
     */
    public PulpieState next();
}