package com.idlesmoothie.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CycleTimerTest {

    private static final double DELTA = 1e-9;

    @Test
    void newTimerStartsAtZero() {
        CycleTimer timer = new CycleTimer();

        assertEquals(0, timer.getProgress(), DELTA);
    }

    @Test
    void partialAdvanceFinishesNoCycle() {
        CycleTimer timer = new CycleTimer();

        int done = timer.advance(0.5, 1);

        assertEquals(0, done);
        assertEquals(0.5, timer.getProgress(), DELTA);
    }

    @Test
    void reachingOneFinishesOneCycle() {
        CycleTimer timer = new CycleTimer();

        int done = timer.advance(1, 1);

        assertEquals(1, done);
        assertEquals(0, timer.getProgress(), DELTA);
    }

    @Test
    void speedScalesProgress() {
        CycleTimer timer = new CycleTimer();

        timer.advance(0.25, 2);

        assertEquals(0.5, timer.getProgress(), DELTA);
    }

    @Test
    void leftoverIsKeptForNextCycle() {
        CycleTimer timer = new CycleTimer();

        int done = timer.advance(1.4, 1);

        assertEquals(1, done);
        assertEquals(0.4, timer.getProgress(), DELTA);
    }

    @Test
    void longAdvanceFinishesSeveralCycles() {
        CycleTimer timer = new CycleTimer();

        int done = timer.advance(2.5, 1);

        assertEquals(2, done);
        assertEquals(0.5, timer.getProgress(), DELTA);
    }

    @Test
    void smallAdvancesAddUp() {
        CycleTimer timer = new CycleTimer();

        int first = timer.advance(0.4, 1);
        int second = timer.advance(0.4, 1);
        int third = timer.advance(0.4, 1);

        assertEquals(0, first);
        assertEquals(0, second);
        assertEquals(1, third);
        assertEquals(0.2, timer.getProgress(), DELTA);
    }

    @Test
    void roundingErrorsDoNotMissACycle() {
        CycleTimer timer = new CycleTimer();
        int done = 0;

        for (int i = 0; i < 10; i++) {
            done += timer.advance(0.1, 1);
        }

        assertEquals(1, done);
        assertEquals(0, timer.getProgress(), DELTA);
    }

    @Test
    void progressIsNeverNegativeAfterRounding() {
        CycleTimer timer = new CycleTimer();

        for (int i = 0; i < 10; i++) {
            timer.advance(0.1, 1);
        }

        assertTrue(timer.getProgress() >= 0);
    }

    @Test
    void zeroSpeedNeverFinishesACycle() {
        CycleTimer timer = new CycleTimer();

        int done = timer.advance(100, 0);

        assertEquals(0, done);
        assertEquals(0, timer.getProgress(), DELTA);
    }

    @Test
    void zeroElapsedTimeChangesNothing() {
        CycleTimer timer = new CycleTimer();
        timer.advance(0.3, 1);

        int done = timer.advance(0, 1);

        assertEquals(0, done);
        assertEquals(0.3, timer.getProgress(), DELTA);
    }

    @Test
    void resetDropsProgress() {
        CycleTimer timer = new CycleTimer();
        timer.advance(0.7, 1);

        timer.reset();

        assertEquals(0, timer.getProgress(), DELTA);
    }

    @Test
    void advanceRejectsNegativeTime() {
        CycleTimer timer = new CycleTimer();

        assertThrows(IllegalArgumentException.class, () -> timer.advance(-1, 1));
    }

    @Test
    void advanceRejectsNegativeSpeed() {
        CycleTimer timer = new CycleTimer();

        assertThrows(IllegalArgumentException.class, () -> timer.advance(1, -1));
    }
}