package com.idlesmoothie.model.station;

/**
 * Kind of station in the production chain.
 * Used to know which station a manager can work on, and what the view should draw.
 */
public enum StationType {
    FLOOR,
    ELEVATOR,
    SELL
}