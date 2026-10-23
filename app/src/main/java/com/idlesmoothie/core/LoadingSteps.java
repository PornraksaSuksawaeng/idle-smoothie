package com.idlesmoothie.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The steps run while the loading screen is shown, one after the other, with a progress from 0 to 1.
 * <p>
 * Each step has a name, shown to the player, and a weight: a step that takes twice as long gets twice the weight,
 * so the progress moves at an even pace. The steps do not know what they load: the caller gives the action.
 */
public final class LoadingSteps {

    /**
     * What a step does. It may throw anything; the step is then reported as failed.
     */
    @FunctionalInterface
    public interface Action {

        /**
         * Runs the step.
         *
         * @throws Exception if the step fails
         */
        void run() throws Exception;
    }

    private record Step(String name, double weight, Action action) {
    }

    private final List<Step> steps = new ArrayList<>();
    private double totalWeight;
    private double doneWeight;
    private int done;

    /**
     * Adds a step of weight 1 at the end of the list.
     *
     * @param name   what the player reads while the step runs, such as {@code "Waking up the Pulpies..."}
     * @param action what the step does
     * @return this list, to add the next step
     * @throws IllegalArgumentException if {@code name} is blank or an argument is {@code null}
     */
    public LoadingSteps add(String name, Action action) {
        return add(name, 1, action);
    }

    /**
     * Adds a step at the end of the list.
     *
     * @param name   what the player reads while the step runs
     * @param weight how long the step takes compared with the others
     * @param action what the step does
     * @return this list, to add the next step
     * @throws IllegalArgumentException if {@code name} is blank, {@code weight} is not positive
     *                                  or an argument is {@code null}
     */
    public LoadingSteps add(String name, double weight, Action action) {
        if (name == null || name.isBlank() || action == null) {
            throw new IllegalArgumentException("Name and action must not be null or blank");
        }
        if (!(weight > 0) || Double.isInfinite(weight)) {
            throw new IllegalArgumentException("Weight must be a positive number: " + weight);
        }
        steps.add(new Step(name, weight, action));
        totalWeight += weight;
        return this;
    }

    /**
     * Runs the next step. If it fails, the progress does not move and the same step runs again on the next call.
     *
     * @throws LoadingException     if the step fails, with the name of the step
     * @throws IllegalStateException if every step is done
     */
    public void runNext() throws LoadingException {
        if (isDone()) {
            throw new IllegalStateException("Every step is done");
        }
        Step step = steps.get(done);
        try {
            step.action().run();
        } catch (Exception e) {
            throw new LoadingException(step.name(), e);
        }
        done++;
        doneWeight += step.weight();
    }

    /**
     * Runs every step left, in order, stopping at the first one that fails.
     *
     * @throws LoadingException if a step fails, with the name of the step
     */
    public void runAll() throws LoadingException {
        while (!isDone()) {
            runNext();
        }
    }

    /**
     * @return {@code true} once every step has run
     */
    public boolean isDone() {
        return done == steps.size();
    }

    /**
     * @return the name of the step that runs next, or {@code null} once every step is done
     */
    public String getCurrentName() {
        return isDone() ? null : steps.get(done).name();
    }

    /**
     * @return the share of the work done, from 0 to 1, weighted by step; 1 when there is no step
     */
    public double getProgress() {
        return isDone() ? 1 : doneWeight / totalWeight;
    }

    /**
     * @return the number of steps
     */
    public int size() {
        return steps.size();
    }
}
