package com.idlesmoothie.model.creature;

/**
 * Third step of a trip: the Pulpie walks back from the extractor to the crate with a full bag.
 */
public class CarryingState implements PulpieState {

    @Override
    public double getShare() {
        return 0.4;
    }

    @Override
    public void finish(Pulpie pulpie) {
        // Nothing happens: the Pulpie just reached the crate.
    }

    @Override
    public PulpieState next() {
        return new DepositingState();
    }
}