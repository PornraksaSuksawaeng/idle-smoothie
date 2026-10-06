package com.idlesmoothie.model.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SmoothieBatchTest {

    private static final double DELTA = 1e-9;

    @Test
    void splitMovesAmountToNewBatch() {
        SmoothieBatch batch = new SmoothieBatch(Fruit.MANGO, 10);

        SmoothieBatch part = batch.split(4);

        assertEquals(6, batch.getQuantity(), DELTA);
        assertEquals(4, part.getQuantity(), DELTA);
        assertEquals(Fruit.MANGO, part.getFruit());
    }

    @Test
    void splitMoreThanAvailableTakesEverything() {
        SmoothieBatch batch = new SmoothieBatch(Fruit.KIWI, 5);

        SmoothieBatch part = batch.split(10);

        assertEquals(0, batch.getQuantity(), DELTA);
        assertEquals(5, part.getQuantity(), DELTA);
    }

    @Test
    void splitRejectsNegativeAmount() {
        SmoothieBatch batch = new SmoothieBatch(Fruit.STRAWBERRY, 10);

        assertThrows(IllegalArgumentException.class, () -> batch.split(-1));
    }

    @Test
    void repeatedSplitsLeaveBatchEmpty() {
        SmoothieBatch batch = new SmoothieBatch(Fruit.STRAWBERRY, 0.3);

        batch.split(0.1);
        batch.split(0.1);
        batch.split(0.1);

        assertTrue(batch.isEmpty());
    }

    @Test
    void mergeAddsQuantityAndEmptiesOther() {
        SmoothieBatch batch1 = new SmoothieBatch(Fruit.DRAGON_FRUIT, 2);
        SmoothieBatch batch2 = new SmoothieBatch(Fruit.DRAGON_FRUIT, 3);

        batch1.merge(batch2);

        assertEquals(5, batch1.getQuantity(), DELTA);
        assertTrue(batch2.isEmpty());
        assertFalse(batch1.isEmpty());
    }

    @Test
    void mergeRejectsDifferentFruit() {
        SmoothieBatch batch1 = new SmoothieBatch(Fruit.STRAWBERRY, 2);
        SmoothieBatch batch2 = new SmoothieBatch(Fruit.KIWI, 3);

        assertThrows(IllegalArgumentException.class, () -> batch1.merge(batch2));
    }

    @Test
    void mergeRejectsItself() {
        SmoothieBatch batch = new SmoothieBatch(Fruit.STRAWBERRY, 2);

        assertThrows(IllegalArgumentException.class, () -> batch.merge(batch));
    }

    @Test
    void constructorRejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new SmoothieBatch(Fruit.MANGO, -1));
    }

    @Test
    void constructorRejectsNullFruit() {
        assertThrows(IllegalArgumentException.class, () -> new SmoothieBatch(null, 1));
    }
}