package com.idlesmoothie.model.resource;

/**
 * Fruit extracted in the mine.
 * Fruits are declared in the order the player unlocks them.
 */
public enum Fruit {
    STRAWBERRY("Strawberry", "#E8323C"),
    BLUEBERRY("Blueberry", "#4A5BD9"),
    KIWI("Kiwi", "#6CC644"),
    MANGO("Mango", "#FF8A1F"),
    DRAGON_FRUIT("Dragon Fruit", "#C94BE0");

    private final String displayName;
    private final String colorHex;

    Fruit(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    /**
     * @return the fruit unlocked after this one, or {@code null} if this is the last one
     */
    public Fruit next() {
        if (isLast()) {
            return null;
        }
        return values()[ordinal() + 1];
    }

    /**
     * @return {@code true} if no fruit comes after this one
     */
    public boolean isLast() {
        return ordinal() == values().length - 1;
    }

    /**
     * @return the name shown to the player
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the theme color, as a hex string
     */
    public String getColorHex() {
        return colorHex;
    }
}