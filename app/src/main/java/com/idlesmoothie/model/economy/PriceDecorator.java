package com.idlesmoothie.model.economy;

import java.util.Objects;

public abstract class PriceDecorator implements PriceCalculator {
    /**
     * The calculator this decorator wraps.
     */
    protected final PriceCalculator wrapped;

    /**
     * Wraps a calculator.
     *
     * @param wrapped the calculator to decorate
     * @throws NullPointerException if wrapped is null
     */
    protected PriceDecorator(PriceCalculator wrapped) {
        Objects.requireNonNull(wrapped, "wrapped");
        this.wrapped = wrapped;
    }
}
