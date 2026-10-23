package com.idlesmoothie.core;

/**
 * Thrown when a step of {@link LoadingSteps} fails. It names the step, so the loading screen can say what broke.
 */
public final class LoadingException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String stepName;

    /**
     * @param stepName the name of the step that failed
     * @param cause    what went wrong
     */
    public LoadingException(String stepName, Throwable cause) {
        super(stepName + " failed: " + cause.getMessage(), cause);
        this.stepName = stepName;
    }

    /**
     * @return the name of the step that failed
     */
    public String getStepName() {
        return stepName;
    }
}
