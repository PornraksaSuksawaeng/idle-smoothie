package com.idlesmoothie.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberFormatterTest {

    @Test
    void smallNumbersAreWholeNumbers() {
        assertEquals("0", NumberFormatter.format(0));
        assertEquals("12", NumberFormatter.format(12.3));
        assertEquals("999", NumberFormatter.format(999));
    }

    @Test
    void namedSuffixes() {
        assertEquals("1.50K", NumberFormatter.format(1_500));
        assertEquals("23.40M", NumberFormatter.format(23_400_000));
        assertEquals("1.00B", NumberFormatter.format(1e9));
        assertEquals("1.00T", NumberFormatter.format(1e12));
    }

    @Test
    void letterSuffixesAfterTrillion() {
        assertEquals("1.00aa", NumberFormatter.format(1e15));
        assertEquals("7.89ab", NumberFormatter.format(7.89e18));
        assertEquals("1.00az", NumberFormatter.format(1e90));
        assertEquals("1.00ba", NumberFormatter.format(1e93));
    }

    @Test
    void roundingMovesToTheNextSuffix() {
        assertEquals("1.00K", NumberFormatter.format(999.7));
        assertEquals("1.00M", NumberFormatter.format(999_999));
    }

    @Test
    void decimalsKeepTwoDigitsWithoutTrailingZeros() {
        assertEquals("2.16", NumberFormatter.formatDecimal(2.16));
        assertEquals("1.5", NumberFormatter.formatDecimal(1.5));
        assertEquals("3", NumberFormatter.formatDecimal(3));
        assertEquals("0.25", NumberFormatter.formatDecimal(0.25));
        assertEquals("1.50K", NumberFormatter.formatDecimal(1_500));
    }
}
