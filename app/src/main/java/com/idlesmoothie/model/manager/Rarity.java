package com.idlesmoothie.model.manager;

/**
 * Rarity of a manager, rolled at purchase: the rarer it is, the stronger its bonus.
 */
public enum Rarity {
    /** Most frequent rarity: automates the station, without any bonus. */
    COMMON(600, 1.0, "#9BA8B4"),
    /** About one purchase in four: +50% on its stat. */
    RARE(280, 1.5, "#3B8FE8"),
    /** About one purchase in eleven: doubles its stat. */
    EPIC(90, 2.0, "#9B4DE0"),
    /** About one purchase in forty: almost triples its stat. */
    LEGENDARY(25, 2.75, "#FFB627"),
    /** One purchase in two hundred: quadruples its stat. */
    MYTHIC(5, 4.0, "#F0428F");

    private final double weight;
    private final double bonusMultiplier;
    private final String colorHex;

    Rarity(double weight, double bonusMultiplier, String colorHex) {
        this.weight = weight;
        this.bonusMultiplier = bonusMultiplier;
        this.colorHex = colorHex;
    }

    /**
     * Returns the relative chance of rolling this rarity; weights do not need to add up to 1000.
     *
     * @return the weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Returns how much stronger the manager's bonus is, such as {@code 1.5} for +50%.
     *
     * @return the bonus multiplier
     */
    public double getBonusMultiplier() {
        return bonusMultiplier;
    }

    /**
     * Returns the color shown by the view, as a hex string.
     *
     * @return the color
     */
    public String getColorHex() {
        return colorHex;
    }
}