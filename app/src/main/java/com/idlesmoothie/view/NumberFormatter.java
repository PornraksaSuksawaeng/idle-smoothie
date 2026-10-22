package com.idlesmoothie.view;

import java.util.Locale;

/**
 * Writes large numbers the idle-game way, such as 1.50K, 23.40M or 7.89ab.
 * After K, M, B and T, each pair of letters is a thousand times more: aa = 10^15, ab = 10^18, ..., ba = 10^93.
 */
public final class NumberFormatter {

    private static final String[] NAMED_SUFFIXES = {"", "K", "M", "B", "T"};
    private static final int LETTERS = 26;
    private static final double THOUSAND = 1000;
    // Shown as a whole number, 999.5 would become "1000" instead of "1.00K".
    private static final double WHOLE_ROUNDS_TO_THOUSAND = 999.5;
    // Shown with 2 decimals, 999.995 would become "1000.00" instead of moving to the next suffix.
    private static final double ROUNDS_TO_THOUSAND = 999.995;

    private NumberFormatter() {
    }

    /**
     * Formats a number with 2 decimals and a suffix, or as a whole number below 1000.
     *
     * @param value the number to format, 0 or more
     * @return the short text to show
     */
    public static String format(double value) {
        if (value < WHOLE_ROUNDS_TO_THOUSAND) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        int tier = Math.max(1, (int) (Math.log10(value) / 3));
        double shown = value / Math.pow(THOUSAND, tier);
        if (shown >= ROUNDS_TO_THOUSAND) {
            shown /= THOUSAND;
            tier++;
        }
        return String.format(Locale.ROOT, "%.2f", shown) + suffix(tier);
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
