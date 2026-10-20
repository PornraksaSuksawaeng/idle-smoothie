package com.idlesmoothie.model.manager;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.Storage;
import com.idlesmoothie.model.station.Elevator;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.StationStats;
import com.idlesmoothie.model.station.StationType;

class ManagerTest {

    private static final double DELTA = 1e-9;

    private final BalanceConfig config = new BalanceConfig();
    private final UUID id = UUID.randomUUID();
    private final ManagerAbility speed = new SpeedBoostAbility(1.5);

    private Manager newFloorManager() {
        return new Manager(id, Rarity.RARE, StationType.FLOOR, speed);
    }

    @Test
    void gettersReturnWhatWasGiven() {
        Manager manager = newFloorManager();

        assertEquals(id, manager.getId());
        assertEquals(Rarity.RARE, manager.getRarity());
        assertEquals(StationType.FLOOR, manager.getStationType());
        assertSame(speed, manager.getAbility());
    }

    @Test
    void newHiresGetDifferentIds() {
        Manager first = new Manager(Rarity.COMMON, StationType.FLOOR, speed);
        Manager second = new Manager(Rarity.COMMON, StationType.FLOOR, speed);

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Manager(null, Rarity.RARE, StationType.FLOOR, speed));
        assertThrows(IllegalArgumentException.class, () -> new Manager(id, null, StationType.FLOOR, speed));
        assertThrows(IllegalArgumentException.class, () -> new Manager(id, Rarity.RARE, null, speed));
        assertThrows(IllegalArgumentException.class, () -> new Manager(id, Rarity.RARE, StationType.FLOOR, null));
    }

    @Test
    void applyUsesTheAbility() {
        StationStats boosted = newFloorManager().apply(new StationStats(2.0, 10.0));

        assertEquals(3.0, boosted.getSpeed(), DELTA);
        assertEquals(10.0, boosted.getCapacity(), DELTA);
    }

    @Test
    void canWorkOnlyOnItsKindOfStation() {
        Manager manager = newFloorManager();
        Floor floor = new Floor(0, Fruit.STRAWBERRY, config);
        Elevator elevator = new Elevator(new ArrayList<>(), new Storage(Double.POSITIVE_INFINITY), config);

        assertTrue(manager.canWork(floor));
        assertFalse(manager.canWork(elevator));
    }

    @Test
    void cannotWorkOnNoStation() {
        assertFalse(newFloorManager().canWork(null));
    }
}