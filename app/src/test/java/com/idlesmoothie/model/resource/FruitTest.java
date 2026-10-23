package com.idlesmoothie.model.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FruitTest {
    
    @Test
    void nextFollowsDeclarationOrder() {
        Fruit[] fruits = Fruit.values();
        for (int i = 0; i < fruits.length - 1; i++) {
            assertEquals(fruits[i + 1], fruits[i].next());
        }
    }

    @Test
    void nextReturnsNullForLastFruit() {
        Fruit[] fruits = Fruit.values();
        Fruit last = fruits[fruits.length - 1];

        assertNull(last.next());
    }

    @Test
    void isLastIsTrueOnlyForLastFruit() {
        Fruit[] fruits = Fruit.values();
        for (int i = 0; i < fruits.length; i++) {
            boolean expected = (i == fruits.length - 1);
            assertEquals(expected, fruits[i].isLast());
        }
    }

    @Test
    void everyFruitHasNameAndValidColor() {
        for (Fruit fruit : Fruit.values()) {
            assertFalse(fruit.getDisplayName().isBlank());
            assertTrue(fruit.getColorHex().matches("#[0-9A-Fa-f]{6}"));
        }
    }
}