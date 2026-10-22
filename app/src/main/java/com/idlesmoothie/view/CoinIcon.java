package com.idlesmoothie.view;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The coin icon shown after every price and after the player's coins, instead of the word "coins".
 */
public final class CoinIcon {

    /** The icon image, loaded once and shared by every view. */
    public static final Image IMAGE = new Image(CoinIcon.class.getResourceAsStream("/images/ui/coin.png"));

    private CoinIcon() {
    }

    /**
     * Puts the icon after the text of a label or a button.
     *
     * @param labeled the label or button showing an amount of coins
     * @param size    the icon size in pixels, about the height of the text
     */
    public static void addTo(Labeled labeled, double size) {
        ImageView icon = new ImageView(IMAGE);
        icon.setFitWidth(size);
        icon.setFitHeight(size);
        icon.setSmooth(true);
        labeled.setGraphic(icon);
        labeled.setContentDisplay(ContentDisplay.RIGHT);
    }
}
