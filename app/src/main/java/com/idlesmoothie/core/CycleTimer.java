package com.idlesmoothie.core;

/**
 * Tracks the progress of a repeating cycle, such as a Pulpie trip or an elevator round trip.
 * Leftover progress is kept between calls, so no cycle is lost whatever the frame rate.
 */
public final class CycleTimer {

    /** Tolerance for floating-point rounding when counting finished cycles. */
    private static final double EPSILON = 1e-9;

    private double progress;

    /**
     * Advances the timer and counts the cycles finished meanwhile.
     *
     * @param dt    elapsed time since the last call, in seconds
     * @param speed number of cycles per second
     * @return the number of cycles finished during {@code dt}, usually 0 or 1
     * @throws IllegalArgumentException if {@code dt} or {@code speed} is negative
     */
    public int advance(double dt, double speed) {
        if (dt < 0 || speed < 0) {
            throw new IllegalArgumentException("dt and speed must be non-negative");
        }
        progress += dt * speed;
        int done = (int) (progress + EPSILON);
        progress = Math.max(0, progress - done);
        return done;
    }

    /**
     * Drops the current progress and starts again from zero.
     */
    public void reset() {
        progress = 0;
    }

    /**
     * @return the progress of the current cycle, between 0 and 1
     */
    public double getProgress() {
        return progress;
    }
}