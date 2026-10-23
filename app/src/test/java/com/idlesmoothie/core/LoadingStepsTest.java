package com.idlesmoothie.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class LoadingStepsTest {

    private static final double DELTA = 1e-9;

    private final List<String> ran = new ArrayList<>();

    private LoadingSteps.Action record(String name) {
        return () -> ran.add(name);
    }

    @Test
    void startsAtZeroWithTheFirstStepName() {
        LoadingSteps steps = new LoadingSteps().add("First", record("a")).add("Second", record("b"));

        assertEquals(0, steps.getProgress(), DELTA);
        assertEquals("First", steps.getCurrentName());
        assertFalse(steps.isDone());
        assertEquals(2, steps.size());
    }

    @Test
    void runsTheStepsInOrder() throws LoadingException {
        LoadingSteps steps = new LoadingSteps().add("First", record("a")).add("Second", record("b"));

        steps.runNext();

        assertEquals(List.of("a"), ran);
        assertEquals("Second", steps.getCurrentName());

        steps.runNext();

        assertEquals(List.of("a", "b"), ran);
        assertTrue(steps.isDone());
        assertNull(steps.getCurrentName());
    }

    @Test
    void progressFollowsTheWeights() throws LoadingException {
        LoadingSteps steps = new LoadingSteps()
                .add("Light", 1, record("a"))
                .add("Heavy", 3, record("b"));

        steps.runNext();

        assertEquals(0.25, steps.getProgress(), DELTA);

        steps.runNext();

        assertEquals(1, steps.getProgress(), DELTA);
    }

    @Test
    void runAllRunsEveryStep() throws LoadingException {
        LoadingSteps steps = new LoadingSteps().add("A", record("a")).add("B", record("b")).add("C", record("c"));

        steps.runAll();

        assertEquals(List.of("a", "b", "c"), ran);
        assertTrue(steps.isDone());
    }

    @Test
    void noStepIsAlreadyDone() {
        LoadingSteps steps = new LoadingSteps();

        assertTrue(steps.isDone());
        assertEquals(1, steps.getProgress(), DELTA);
    }

    @Test
    void failingStepIsNamedAndDoesNotMoveTheProgress() {
        IOException cause = new IOException("disk full");
        LoadingSteps steps = new LoadingSteps()
                .add("Fine", record("a"))
                .add("Opening your save...", () -> {
                    throw cause;
                })
                .add("Never reached", record("c"));

        LoadingException error = assertThrows(LoadingException.class, steps::runAll);

        assertEquals("Opening your save...", error.getStepName());
        assertSame(cause, error.getCause());
        assertEquals("Opening your save... failed: disk full", error.getMessage());
        assertEquals(List.of("a"), ran);
        assertEquals(1.0 / 3, steps.getProgress(), DELTA);
        assertEquals("Opening your save...", steps.getCurrentName());
    }

    @Test
    void runNextAfterTheLastStepFails() throws LoadingException {
        LoadingSteps steps = new LoadingSteps().add("Only", record("a"));
        steps.runNext();

        assertThrows(IllegalStateException.class, steps::runNext);
    }

    @Test
    void addRejectsBadSteps() {
        LoadingSteps steps = new LoadingSteps();

        assertThrows(IllegalArgumentException.class, () -> steps.add(null, record("a")));
        assertThrows(IllegalArgumentException.class, () -> steps.add(" ", record("a")));
        assertThrows(IllegalArgumentException.class, () -> steps.add("Name", null));
        assertThrows(IllegalArgumentException.class, () -> steps.add("Name", 0, record("a")));
        assertThrows(IllegalArgumentException.class, () -> steps.add("Name", -1, record("a")));
        assertThrows(IllegalArgumentException.class, () -> steps.add("Name", Double.NaN, record("a")));
        assertThrows(IllegalArgumentException.class, () -> steps.add("Name", Double.POSITIVE_INFINITY, record("a")));
        assertEquals(0, steps.size());
    }
}
