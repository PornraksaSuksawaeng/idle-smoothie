package com.idlesmoothie.model.manager;

import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.idlesmoothie.model.station.StationStats;
import com.idlesmoothie.model.station.StationType;

class ManagerFactoryTest {

    private static final double DELTA = 1e-9;
    private static final double SPEED_ROLL = 0.1;
    private static final double CAPACITY_ROLL = 0.9;

    /** Returns the given rolls in order: first the rarity, then the bonus. */
    private static DoubleSupplier rolls(Double... values) {
        Iterator<Double> next = List.of(values).iterator();
        return next::next;
    }

    /** Roll that lands in the middle of the given rarity's share. */
    private static double rollFor(Rarity wanted) {
        double total = 0;
        double before = 0;
        for (Rarity rarity : Rarity.values()) {
            if (rarity == wanted) {
                before = total;
            }
            total += rarity.getWeight();
        }
        return (before + wanted.getWeight() / 2) / total;
    }

    private Manager create(Rarity rarity, double abilityRoll) {
        return new ManagerFactory(rolls(rollFor(rarity), abilityRoll)).create(StationType.FLOOR);
    }

    @Test
    void everyRarityCanBeRolled() {
        for (Rarity rarity : Rarity.values()) {
            assertEquals(rarity, create(rarity, SPEED_ROLL).getRarity(), rarity + " was not rolled");
        }
    }

    @Test
    void lowestRollGivesTheMostCommonRarity() {
        Manager manager = new ManagerFactory(rolls(0.0, SPEED_ROLL)).create(StationType.FLOOR);

        assertEquals(Rarity.values()[0], manager.getRarity());
    }

    @Test
    void highestRollGivesTheRarestRarity() {
        Manager manager = new ManagerFactory(rolls(0.9999999, SPEED_ROLL)).create(StationType.FLOOR);

        assertEquals(Rarity.values()[Rarity.values().length - 1], manager.getRarity());
    }

    @Test
    void lowBonusRollGivesSpeed() {
        assertTrue(create(Rarity.EPIC, SPEED_ROLL).getAbility() instanceof SpeedBoostAbility);
    }

    @Test
    void highBonusRollGivesCapacity() {
        assertTrue(create(Rarity.EPIC, CAPACITY_ROLL).getAbility() instanceof CapacityBoostAbility);
    }

    @Test
    void bonusIsAsStrongAsTheRarity() {
        for (Rarity rarity : Rarity.values()) {
            SpeedBoostAbility speed = (SpeedBoostAbility) create(rarity, SPEED_ROLL).getAbility();
            CapacityBoostAbility capacity = (CapacityBoostAbility) create(rarity, CAPACITY_ROLL).getAbility();

            assertEquals(rarity.getBonusMultiplier(), speed.getFactor(), DELTA);
            assertEquals(rarity.getBonusMultiplier(), capacity.getFactor(), DELTA);
        }
    }

    @Test
    void managerWorksOnTheRequestedStationType() {
        for (StationType type : StationType.values()) {
            Manager manager = new ManagerFactory(rolls(0.5, SPEED_ROLL)).create(type);

            assertEquals(type, manager.getStationType());
        }
    }

    @Test
    void realRandomFactoryBuildsValidManagers() {
        ManagerFactory factory = new ManagerFactory();

        for (int i = 0; i < 1000; i++) {
            Manager manager = factory.create(StationType.ELEVATOR);
            assertEquals(StationType.ELEVATOR, manager.getStationType());
            assertTrue(manager.getAbility().apply(new StationStats(1, 1)).getSpeed() >= 1);
        }
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new ManagerFactory(null));
        assertThrows(IllegalArgumentException.class, () -> new ManagerFactory().create(null));
    }
}