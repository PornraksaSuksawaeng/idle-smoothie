package com.idlesmoothie.model.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.SmoothieBatch;
import com.idlesmoothie.model.resource.Storage;

class PulpieTest {

    private static final double DELTA = 1e-9;
    private static final double SPEED = 1;
    private static final double CARRY = 10;

    private final Storage crate = new Storage(Double.POSITIVE_INFINITY);

    /** Runs exactly one step of the trip with speed 1, using the share of the current state. */
    private void finishStep(Pulpie pulpie) {
        pulpie.update(pulpie.getState().getShare(), SPEED, CARRY);
    }

    @Test
    void newPulpieIsIdleWithEmptyBag() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);

        assertTrue(pulpie.isIdle());
        assertNull(pulpie.getState());
        assertNull(pulpie.getBag());
    }

    @Test
    void constructorRejectsNullFruit() {
        assertThrows(IllegalArgumentException.class, () -> new Pulpie(null, crate));
    }

    @Test
    void constructorRejectsNullCrate() {
        assertThrows(IllegalArgumentException.class, () -> new Pulpie(Fruit.STRAWBERRY, null));
    }

    @Test
    void idlePulpieDoesNotMove() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);

        pulpie.update(10, SPEED, CARRY);

        assertTrue(pulpie.isIdle());
        assertTrue(crate.isEmpty());
    }

    @Test
    void dispatchStartsTripTowardExtractor() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);

        pulpie.dispatch();

        assertFalse(pulpie.isIdle());
        assertInstanceOf(GoingToExtractorState.class, pulpie.getState());
        assertEquals(0, pulpie.getProgress(), DELTA);
    }

    @Test
    void dispatchDuringTripDoesNotRestartIt() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);
        pulpie.dispatch();
        finishStep(pulpie);

        pulpie.dispatch();

        assertInstanceOf(FillingState.class, pulpie.getState());
    }

    @Test
    void partialUpdateStaysInCurrentState() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);
        pulpie.dispatch();

        pulpie.update(0.2, SPEED, CARRY);

        assertInstanceOf(GoingToExtractorState.class, pulpie.getState());
        assertEquals(0.5, pulpie.getProgress(), DELTA);
    }

    @Test
    void tripGoesThroughEveryStateInOrder() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);
        pulpie.dispatch();

        finishStep(pulpie);
        assertInstanceOf(FillingState.class, pulpie.getState());
        finishStep(pulpie);
        assertInstanceOf(CarryingState.class, pulpie.getState());
        finishStep(pulpie);
        assertInstanceOf(DepositingState.class, pulpie.getState());
        finishStep(pulpie);
        assertTrue(pulpie.isIdle());
    }

    @Test
    void bagIsFilledAtExtractorWithFloorFruit() {
        Pulpie pulpie = new Pulpie(Fruit.KIWI, crate);
        pulpie.dispatch();

        finishStep(pulpie);
        assertNull(pulpie.getBag());
        finishStep(pulpie);

        SmoothieBatch bag = pulpie.getBag();
        assertNotNull(bag);
        assertEquals(Fruit.KIWI, bag.getFruit());
        assertEquals(CARRY, bag.getQuantity(), DELTA);
    }

    @Test
    void crateOnlyReceivesSmoothieWhenPulpieDeposits() {
        Pulpie pulpie = new Pulpie(Fruit.MANGO, crate);
        pulpie.dispatch();

        finishStep(pulpie);
        finishStep(pulpie);
        finishStep(pulpie);
        assertTrue(crate.isEmpty());

        finishStep(pulpie);
        assertEquals(CARRY, crate.getAmount(), DELTA);
        assertNull(pulpie.getBag());
    }

    @Test
    void fullTripTakesOneSecondAtSpeedOne() {
        Pulpie pulpie = new Pulpie(Fruit.BLUEBERRY, crate);
        pulpie.dispatch();

        for (int i = 0; i < 100; i++) {
            pulpie.update(0.01, SPEED, CARRY);
        }

        assertTrue(pulpie.isIdle());
        assertEquals(CARRY, crate.getAmount(), DELTA);
    }

    @Test
    void fasterPulpieFinishesTripSooner() {
        Pulpie pulpie = new Pulpie(Fruit.BLUEBERRY, crate);
        pulpie.dispatch();

        for (int i = 0; i < 50; i++) {
            pulpie.update(0.01, 2, CARRY);
        }

        assertTrue(pulpie.isIdle());
        assertEquals(CARRY, crate.getAmount(), DELTA);
    }

    @Test
    void bagUsesCarryAtTheMomentItIsFilled() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);
        pulpie.dispatch();
        pulpie.update(0.4, SPEED, 5);

        pulpie.update(0.1, SPEED, 25);

        assertEquals(25, pulpie.getBag().getQuantity(), DELTA);
    }

    @Test
    void severalTripsAddUpInCrate() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);

        for (int trip = 0; trip < 3; trip++) {
            pulpie.dispatch();
            for (int step = 0; step < 4; step++) {
                finishStep(pulpie);
            }
        }

        assertEquals(3 * CARRY, crate.getAmount(), DELTA);
    }

    @Test
    void modifyingBagCopyDoesNotChangeCarriedSmoothie() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);
        pulpie.dispatch();
        finishStep(pulpie);
        finishStep(pulpie);

        pulpie.getBag().split(CARRY);

        assertEquals(CARRY, pulpie.getBag().getQuantity(), DELTA);
    }

    @Test
    void updateRejectsNegativeTime() {
        Pulpie pulpie = new Pulpie(Fruit.STRAWBERRY, crate);
        pulpie.dispatch();

        assertThrows(IllegalArgumentException.class, () -> pulpie.update(-1, SPEED, CARRY));
    }
}