package com.idlesmoothie.model.creature;

/**
 * Second step of a trip: the Pulpie fills its bag at the extractor.
 */
public class FillingState implements PulpieState {

    @Override
    public double getShare() {
        return 0.1;
    }

    @Override
    public void finish(Pulpie pulpie) {
        pulpie.fillBag();
    }

    @Override
    public PulpieState next() {
        return new CarryingState();
    }
}