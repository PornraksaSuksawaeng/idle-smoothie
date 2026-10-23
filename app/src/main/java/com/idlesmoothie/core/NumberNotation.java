package com.idlesmoothie.core;

/**
 * How large numbers are written on screen.
 */
public enum NumberNotation {
    /** K, M, B, T then pairs of letters, such as 1.50K or 7.89ab. */
    SUFFIXES,
    /** A power of ten, such as 1.50e3 or 7.89e18. */
    SCIENTIFIC
}
