package com.idlesmoothie.view;

import java.util.Locale;

import com.idlesmoothie.core.NumberNotation;

/**
 * Writes large numbers the idle-game way, such as 1.50K, 23.40M or 7.89ab.
 * After K, M, B and T, each pair of letters is a thousand times more: aa = 10^15, ab = 10^18, ..., ba = 10^93.
 * In the scientific notation chosen in the settings, they are written as a power of ten instead, such as 7.89e18.
 */
public final class NumberFormatter {

    private static final String[] NAMED_SUFFIXES = {"", "K", "M", "B", "T"};
    private static final int LETTERS = 26;
    private static final double THOUSAND = 1000;
    // Shown as a whole number, 999.5 would become "1000" instead of "1.00K".
    private static final double WHOLE_ROUNDS_TO_THOUSAND = 999.5;
    // Shown with 2 decimals, 999.995 would become "1000.00" instead of moving to the next suffix.
    private static final double ROUNDS_TO_THOUSAND = 999.995;
    // Below this, speeds and multipliers keep their decimals.
    private static final double DECIMALS_LIMIT = 100;

    private static NumberNotation notation = NumberNotation.SUFFIXES;

    private NumberFormatter() {
    }

    /**
     * Picks how every number is written from now on.
     *
     * @param notation suffixes such as 1.50K, or scientific such as 1.50e3
     * @throws IllegalArgumentException if {@code notation} is {@code null}
     */
    public static void setNotation(NumberNotation notation) {
        if (notation == null) {
            throw new IllegalArgumentException("Notation must not be null");
        }
        NumberFormatter.notation = notation;
    }

    /**
     * Formats a number with 2 decimals and a suffix or a power of ten, or as a whole number below 1000.
     *
     * @param value the number to format, 0 or more
     * @return the short text to show
     */
    public static String format(double value) {
        if (value < WHOLE_ROUNDS_TO_THOUSAND) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (notation == NumberNotation.SCIENTIFIC) {
            // Java writes 1.50e+03: the sign and the leading zeros of the exponent are dropped.
            return String.format(Locale.ROOT, "%.2e", value).replaceFirst("e\\+0*", "e");
        }
        int tier = Math.max(1, (int) (Math.log10(value) / 3));
        double shown = value / Math.pow(THOUSAND, tier);
        if (shown >= ROUNDS_TO_THOUSAND) {
            shown /= THOUSAND;
            tier++;
        }
        return String.format(Locale.ROOT, "%.2f", shown) + suffix(tier);
    }

    /**
     * Formats a small number with up to 2 decimals and no trailing zeros, such as 2.16, 1.5 or 3,
     * for speeds and multipliers. From 100 on, it is the same as {@link #format(double)}.
     *
     * @param value the number to format, 0 or more
     * @return the short text to show
     */
    public static String formatDecimal(double value) {
        if (value >= DECIMALS_LIMIT) {
            return format(value);
        }
        String text = String.format(Locale.ROOT, "%.2f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // 1 = K, 2 = M, 3 = B, 4 = T, then 5 = aa, 6 = ab, ..., 30 = az, 31 = ba.
    private static String suffix(int tier) {
        if (tier < NAMED_SUFFIXES.length) {
            return NAMED_SUFFIXES[tier];
        }
        int index = tier - NAMED_SUFFIXES.length;
        char first = (char) ('a' + index / LETTERS);
        char second = (char) ('a' + index % LETTERS);
        return "" + first + second;
    }
}
