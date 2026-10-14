package com.idlesmoothie.model.station;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.Storage;

class ElevatorTest {

    private static final double DELTA = 1e-9;
    private static final double STEP = 0.01;

    private final BalanceConfig config = new BalanceConfig();
    private final List<Floor> floors = new ArrayList<>();
    private final Storage surface = new Storage(Double.POSITIVE_INFINITY);

    /** Elevator that behaves as if a manager was assigned. */
    private static class AutomatedElevator extends Elevator {

        AutomatedElevator(List<Floor> floors, Storage surface, BalanceConfig config) {
            super(floors, surface, config);
        }

        @Override
        public boolean isAutomated() {
            return true;
        }
    }

    private static void run(Station station, double seconds) {
        for (double t = 0; t < seconds; t += STEP) {
            station.update(STEP);
        }
    }

    /** Time to travel the given number of floors, plus a margin for frame rounding. */
    private double travel(int floorCount) {
        return floorCount / config.getElevatorMoveSpeed() + 0.1;
    }

    /** Adds a floor whose crate holds the given quantity. */
    private Floor addFloor(double stored) {
        Floor floor = new Floor(floors.size(), Fruit.STRAWBERRY, config);
        floors.add(floor);
        while (floor.getStoredAmount() < stored) {
            floor.start();
            run(floor, 1 / config.getFloorTripSpeed() + 0.1);
        }
        floor.collect(floor.getStoredAmount() - stored);
        return floor;
    }

    @Test
    void newElevatorWaitsAtTheSurface() {
        Elevator elevator = new Elevator(floors, surface, config);

        assertEquals(StationType.ELEVATOR, elevator.getType());
        assertEquals(0, elevator.getPosition(), DELTA);
        assertTrue(elevator.isGoingDown());
        assertEquals(0, elevator.getCabinAmount(), DELTA);
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Elevator(null, surface, config));
        assertThrows(IllegalArgumentException.class, () -> new Elevator(floors, null, config));
        assertThrows(IllegalArgumentException.class, () -> new Elevator(floors, surface, null));
    }

    @Test
    void statsComeFromConfigAndLevel() {
        Elevator elevator = new Elevator(floors, surface, config);

        elevator.upgrade(2);

        assertEquals(config.getElevatorMoveSpeed(), elevator.getStats().getSpeed(), DELTA);
        assertEquals(3 * config.getElevatorBaseCapacity(), elevator.getStats().getCapacity(), DELTA);
    }

    @Test
    void upgradeCostGrowsWithLevel() {
        Elevator elevator = new Elevator(floors, surface, config);
        assertEquals(config.getElevatorBaseCost(), elevator.getUpgradeCost(), DELTA);

        elevator.upgrade(1);

        assertEquals(config.getElevatorBaseCost() * config.getUpgradeCostGrowth(), elevator.getUpgradeCost(), DELTA);
    }

    @Test
    void elevatorDoesNotMoveWithoutTap() {
        addFloor(10);
        Elevator elevator = new Elevator(floors, surface, config);

        run(elevator, travel(5));

        assertEquals(0, elevator.getPosition(), DELTA);
        assertEquals(0, surface.getAmount(), DELTA);
    }

    @Test
    void elevatorGoesDownFloorByFloor() {
        addFloor(0);
        addFloor(0);
        addFloor(0);
        Elevator elevator = new Elevator(floors, surface, config);

        elevator.start();
        run(elevator, 1.5 / config.getElevatorMoveSpeed());

        assertTrue(elevator.isGoingDown());
        assertTrue(elevator.getPosition() > 1 && elevator.getPosition() < 2);
    }

    @Test
    void tapBringsCratesToTheSurface() {
        addFloor(10);
        addFloor(20);
        Elevator elevator = new Elevator(floors, surface, config);

        elevator.start();
        run(elevator, travel(4));

        assertEquals(30, surface.getAmount(), DELTA);
        assertEquals(0, floors.get(0).getStoredAmount(), DELTA);
        assertEquals(0, floors.get(1).getStoredAmount(), DELTA);
        assertFalse(elevator.isRunning());
        assertEquals(0, elevator.getPosition(), DELTA);
    }

    @Test
    void fullCabinTurnsBackBeforeTheBottom() {
        double capacity = config.getElevatorBaseCapacity();
        addFloor(capacity);
        addFloor(10);
        Elevator elevator = new Elevator(floors, surface, config);

        elevator.start();
        run(elevator, travel(2));

        assertEquals(capacity, surface.getAmount(), DELTA);
        assertEquals(10, floors.get(1).getStoredAmount(), DELTA);
    }

    @Test
    void cabinTakesOnlyWhatFits() {
        double capacity = config.getElevatorBaseCapacity();
        addFloor(capacity + 15);
        Elevator elevator = new Elevator(floors, surface, config);

        elevator.start();
        run(elevator, travel(2));

        assertEquals(capacity, surface.getAmount(), DELTA);
        assertEquals(15, floors.get(0).getStoredAmount(), DELTA);
    }

    @Test
    void cabinIsFullAtTheBottomBeforeGoingUp() {
        addFloor(10);
        Elevator elevator = new Elevator(floors, surface, config);

        elevator.start();
        run(elevator, travel(1));

        assertFalse(elevator.isGoingDown());
        assertEquals(10, elevator.getCabinAmount(), DELTA);
        assertEquals(0, surface.getAmount(), DELTA);
    }

    @Test
    void elevatorWaitsForNextTapAfterRoundTrip() {
        Floor floor = addFloor(10);
        Elevator elevator = new Elevator(floors, surface, config);
        elevator.start();
        run(elevator, travel(2));
        floor.start();
        run(floor, 1 / config.getFloorTripSpeed() + 0.1);

        run(elevator, travel(2));

        assertEquals(10, surface.getAmount(), DELTA);
    }

    @Test
    void automatedElevatorKeepsTravelling() {
        Floor floor = addFloor(10);
        Elevator elevator = new AutomatedElevator(floors, surface, config);
        run(elevator, travel(2));
        floor.start();
        run(floor, 1 / config.getFloorTripSpeed() + 0.1);

        run(elevator, travel(2));

        assertEquals(10 + floor.getStats().getCapacity(), surface.getAmount(), DELTA);
    }

    @Test
    void newFloorsAreServedWithoutTellingTheElevator() {
        Elevator elevator = new Elevator(floors, surface, config);
        addFloor(10);

        elevator.start();
        run(elevator, travel(2));

        assertEquals(10, surface.getAmount(), DELTA);
    }

    @Test
    void tapWithNoFloorEndsTheRound() {
        Elevator elevator = new Elevator(floors, surface, config);

        elevator.start();
        elevator.update(STEP);

        assertFalse(elevator.isRunning());
        assertEquals(0, elevator.getPosition(), DELTA);
    }

    @Test
    void deeperMineLowersThroughput() {
        addFloor(0);
        Elevator elevator = new Elevator(floors, surface, config);
        double shallow = elevator.getThroughput();

        addFloor(0);

        assertEquals(shallow / 2, elevator.getThroughput(), DELTA);
    }
}