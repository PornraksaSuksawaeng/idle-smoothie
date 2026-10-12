package com.idlesmoothie.model.creature;

/**
 * Last step of a trip: the Pulpie empties its bag into the crate, then becomes idle again.
 */
public class DepositingState implements PulpieState {

    @Override
    public double getShare() {
        return 0.1;
    }

    @Override
    public void finish(Pulpie pulpie) {
        pulpie.emptyBag();
    }

    @Override
    public PulpieState next() {
        return null;
    }
}