package com.idlesmoothie.model.resource;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageTest {

    private static final double DELTA = 1e-9;

    @Test
    void storeKeepsWholeBatchWhenThereIsRoom() {
        Storage storage = new Storage(10);
        SmoothieBatch batch = new SmoothieBatch(Fruit.STRAWBERRY, 4);

        double stored = storage.store(batch);

        assertEquals(4, stored, DELTA);
        assertEquals(4, storage.getAmount(), DELTA);
        assertTrue(batch.isEmpty());
    }

    @Test
    void storeLeavesOverflowInGivenBatch() {
        Storage storage = new Storage(10);
        SmoothieBatch batch = new SmoothieBatch(Fruit.KIWI, 13);

        double stored = storage.store(batch);

        assertEquals(10, stored, DELTA);
        assertEquals(3, batch.getQuantity(), DELTA);
        assertTrue(storage.isFull());
    }

    @Test
    void storeIntoFullStorageStoresNothing() {
        Storage storage = new Storage(5);
        storage.store(new SmoothieBatch(Fruit.MANGO, 5));
        SmoothieBatch extra = new SmoothieBatch(Fruit.MANGO, 2);

        double stored = storage.store(extra);

        assertEquals(0, stored, DELTA);
        assertEquals(2, extra.getQuantity(), DELTA);
    }

    @Test
    void storeMergesConsecutiveBatchesOfSameFruit() {
        Storage storage = new Storage(10);

        storage.store(new SmoothieBatch(Fruit.BLUEBERRY, 3));
        storage.store(new SmoothieBatch(Fruit.BLUEBERRY, 4));

        List<SmoothieBatch> batches = storage.getBatches();
        assertEquals(1, batches.size());
        assertEquals(7, batches.get(0).getQuantity(), DELTA);
    }

    @Test
    void storeKeepsDifferentFruitsSeparate() {
        Storage storage = new Storage(10);

        storage.store(new SmoothieBatch(Fruit.STRAWBERRY, 3));
        storage.store(new SmoothieBatch(Fruit.KIWI, 4));

        List<SmoothieBatch> batches = storage.getBatches();
        assertEquals(2, batches.size());
        assertEquals(Fruit.STRAWBERRY, batches.get(0).getFruit());
        assertEquals(Fruit.KIWI, batches.get(1).getFruit());
    }

    @Test
    void storeRejectsNullBatch() {
        Storage storage = new Storage(10);

        assertThrows(IllegalArgumentException.class, () -> storage.store(null));
    }

    @Test
    void takeRemovesOldestFirstAndSplitsLastBatch() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.STRAWBERRY, 7));
        storage.store(new SmoothieBatch(Fruit.KIWI, 3));

        List<SmoothieBatch> taken = storage.take(8);

        assertEquals(2, taken.size());
        assertEquals(Fruit.STRAWBERRY, taken.get(0).getFruit());
        assertEquals(7, taken.get(0).getQuantity(), DELTA);
        assertEquals(Fruit.KIWI, taken.get(1).getFruit());
        assertEquals(1, taken.get(1).getQuantity(), DELTA);
        assertEquals(2, storage.getAmount(), DELTA);
    }

    @Test
    void takeMoreThanStoredReturnsEverything() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.MANGO, 6));

        List<SmoothieBatch> taken = storage.take(100);

        assertEquals(1, taken.size());
        assertEquals(6, taken.get(0).getQuantity(), DELTA);
        assertTrue(storage.isEmpty());
    }

    @Test
    void takeFromEmptyStorageReturnsEmptyList() {
        Storage storage = new Storage(10);

        assertTrue(storage.take(5).isEmpty());
    }

    @Test
    void takeRejectsNegativeAmount() {
        Storage storage = new Storage(10);

        assertThrows(IllegalArgumentException.class, () -> storage.take(-1));
    }

    @Test
    void emptiedBatchesAreRemoved() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.KIWI, 5));

        storage.take(5);

        assertTrue(storage.getBatches().isEmpty());
    }

    @Test
    void newStorageIsEmptyAndNotFull() {
        Storage storage = new Storage(10);

        assertTrue(storage.isEmpty());
        assertFalse(storage.isFull());
        assertEquals(10, storage.getFreeSpace(), DELTA);
    }

    @Test
    void partiallyFilledStorageIsNeitherEmptyNorFull() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.STRAWBERRY, 4));

        assertFalse(storage.isEmpty());
        assertFalse(storage.isFull());
        assertEquals(6, storage.getFreeSpace(), DELTA);
    }

    @Test
    void loweringCapacityBelowContentKeepsItAndMakesStorageFull() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.DRAGON_FRUIT, 8));

        storage.setCapacity(5);

        assertEquals(8, storage.getAmount(), DELTA);
        assertEquals(0, storage.getFreeSpace(), DELTA);
        assertTrue(storage.isFull());
    }

    @Test
    void raisingCapacityAddsFreeSpace() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.MANGO, 10));

        storage.setCapacity(15);

        assertEquals(15, storage.getCapacity(), DELTA);
        assertEquals(5, storage.getFreeSpace(), DELTA);
    }

    @Test
    void constructorRejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new Storage(-1));
    }

    @Test
    void setCapacityRejectsNegativeValue() {
        Storage storage = new Storage(10);

        assertThrows(IllegalArgumentException.class, () -> storage.setCapacity(-1));
    }

    @Test
    void modifyingBatchCopiesDoesNotChangeStorage() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.KIWI, 6));

        storage.getBatches().get(0).split(6);

        assertEquals(6, storage.getAmount(), DELTA);
    }

    @Test
    void takeRemovesBatchLeftWithRoundingLeftover() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.KIWI, 0.1 + 0.2));

        storage.take(0.3);

        assertTrue(storage.getBatches().isEmpty());
    }

    @Test
    void storeEmptyBatchAddsNothing() {
        Storage storage = new Storage(10);

        double stored = storage.store(new SmoothieBatch(Fruit.MANGO, 0));

        assertEquals(0, stored, DELTA);
        assertTrue(storage.getBatches().isEmpty());
    }

    @Test
    void storeOnlyMergesWithLastBatch() {
        Storage storage = new Storage(10);

        storage.store(new SmoothieBatch(Fruit.STRAWBERRY, 2));
        storage.store(new SmoothieBatch(Fruit.KIWI, 2));
        storage.store(new SmoothieBatch(Fruit.STRAWBERRY, 2));

        assertEquals(3, storage.getBatches().size());
    }

    @Test
    void takeZeroReturnsNothingAndKeepsContent() {
        Storage storage = new Storage(10);
        storage.store(new SmoothieBatch(Fruit.BLUEBERRY, 4));

        assertTrue(storage.take(0).isEmpty());
        assertEquals(4, storage.getAmount(), DELTA);
    }
}