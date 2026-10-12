package com.idlesmoothie.model.creature;

import com.idlesmoothie.core.CycleTimer;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import com.idlesmoothie.model.resource.Storage;

/**
 * Small fruit creature working on a floor: it walks to the extractor, fills its bag,
 * carries it back and empties it into the floor crate.
 * Its behavior at each step is delegated to its current {@link PulpieState}; it is idle between trips.
 */
public class Pulpie {

    private final Fruit fruit;
    private final Storage crate;
    private final CycleTimer timer;
    private PulpieState state;
    private SmoothieBatch bag;
    private double carry;

    /**
     * Creates an idle Pulpie with an empty bag.
     *
     * @param fruit fruit of the floor it works on
     * @param crate floor crate where it deposits its smoothie
     * @throws IllegalArgumentException if the fruit or the crate is {@code null}
     */
    public Pulpie(Fruit fruit, Storage crate) {
        if (fruit == null || crate == null) {
            throw new IllegalArgumentException("Fruit and crate must not be null");
        }
        this.fruit = fruit;
        this.crate = crate;
        this.timer = new CycleTimer();
    }

    /**
     * Sends the Pulpie on a trip. Does nothing if it is already on its way.
     */
    public void dispatch() {
        if (isIdle()) {
            state = new GoingToExtractorState();
            timer.reset();
        }
    }

    /**
     * Moves the Pulpie forward in its trip and switches to the next step when the current one is over.
     *
     * @param dt    elapsed time since the last update, in seconds
     * @param speed number of full trips per second
     * @param carry quantity of smoothie the bag can hold
     * @throws IllegalArgumentException if {@code dt} or {@code speed} is negative
     */
    public void update(double dt, double speed, double carry) {
        if (isIdle()) {
            return;
        }
        this.carry = carry;
        int done = timer.advance(dt, speed / state.getShare());
        if (done > 0) {
            state.finish(this);
            state = state.next();
            timer.reset();
        }
    }

    /**
     * Fills the bag with smoothie of the floor fruit. Called by {@link FillingState}.
     */
    void fillBag() {
        bag = new SmoothieBatch(fruit, carry);
    }

    /**
     * Empties the bag into the floor crate. Called by {@link DepositingState}.
     */
    void emptyBag() {
        crate.store(bag);
        bag = null;
    }

    /**
     * @return {@code true} if the Pulpie waits for its next trip
     */
    public boolean isIdle() {
        return state == null;
    }

    /**
     * @return the current step of the trip, or {@code null} if the Pulpie is idle
     */
    public PulpieState getState() {
        return state;
    }

    /**
     * Returns a copy of the carried smoothie, for display only.
     *
     * @return a copy of the bag content, or {@code null} if the bag is empty
     */
    public SmoothieBatch getBag() {
        if (bag == null) {
            return null;
        }
        return new SmoothieBatch(bag.getFruit(), bag.getQuantity());
    }

    /**
     * @return the progress of the current step, between 0 and 1
     */
    public double getProgress() {
        return timer.getProgress();
    }
}