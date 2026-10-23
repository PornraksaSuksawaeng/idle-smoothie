package com.idlesmoothie.model.creature;

/**
 * First step of a trip: the Pulpie walks from the crate to the extractor.
 */
public class GoingToExtractorState implements PulpieState {

    @Override
    public double getShare() {
        return 0.4;
    }

    @Override
    public void finish(Pulpie pulpie) {
        // Nothing happens: the Pulpie just reached the extractor.
    }

    @Override
    public PulpieState next() {
        return new FillingState();
    }
}