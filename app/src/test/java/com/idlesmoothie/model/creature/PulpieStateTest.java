package com.idlesmoothie.model.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class PulpieStateTest {

    private static final double DELTA = 1e-9;

    @Test
    void sharesOfAFullTripAddUpToOne() {
        double total = 0;
        PulpieState state = new GoingToExtractorState();

        while (state != null) {
            total += state.getShare();
            state = state.next();
        }

        assertEquals(1, total, DELTA);
    }

    @Test
    void walkingStatesTakeMostOfTheTrip() {
        assertEquals(0.4, new GoingToExtractorState().getShare(), DELTA);
        assertEquals(0.1, new FillingState().getShare(), DELTA);
        assertEquals(0.4, new CarryingState().getShare(), DELTA);
        assertEquals(0.1, new DepositingState().getShare(), DELTA);
    }

    @Test
    void statesFollowEachOtherInTripOrder() {
        assertInstanceOf(FillingState.class, new GoingToExtractorState().next());
        assertInstanceOf(CarryingState.class, new FillingState().next());
        assertInstanceOf(DepositingState.class, new CarryingState().next());
        assertNull(new DepositingState().next());
    }
}