package com.idlesmoothie.view;

import com.idlesmoothie.core.Game;
import com.idlesmoothie.model.economy.BuyMode;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Station;

/**
 * Turns the player's clicks into calls to the {@link Game} facade.
 * The views never act on the model themselves: they all go through this controller.
 * It also remembers the buy mode chosen in the upgrade popup.
 */
public class UpgradeController {

    private final Game game;
    private BuyMode mode = BuyMode.X1;

    /**
     * Creates the controller with the {@link BuyMode#X1} mode selected.
     *
     * @param game the game the player's actions are sent to
     */
    public UpgradeController(Game game) {
        this.game = game;
    }

    /**
     * Starts one round of work of the tapped station.
     *
     * @param station the station the player clicked
     */
    public void onTap(Station station) {
        switch (station.getType()) {
            case FLOOR -> game.tapFloor(((Floor) station).getIndex());
            case ELEVATOR -> game.tapElevator();
            case SELL -> game.tapSellStation();
        }
    }

    /**
     * Buys levels for the station with the current buy mode. Does nothing if the player cannot pay.
     *
     * @param station the station to upgrade
     */
    public void onUpgrade(Station station) {
        game.upgrade(station, mode);
    }

    /**
     * Hires a manager for the station, replacing the current one if any.
     *
     * @param station the station to automate
     * @return the new manager, or {@code null} if the player cannot pay
     */
    public Manager onHireManager(Station station) {
        return game.hireManager(station);
    }

    /**
     * Buys the next floor. Does nothing if the player cannot pay or a barrier blocks it.
     */
    public void onUnlockFloor() {
        game.buyNextFloor();
    }

    /**
     * @param mode how many levels the next upgrades buy
     */
    public void setMode(BuyMode mode) {
        this.mode = mode;
    }

    /**
     * @return how many levels the next upgrades buy
     */
    public BuyMode getMode() {
        return mode;
    }
}
