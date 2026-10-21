package com.idlesmoothie.core;

import java.util.ArrayList;
import java.util.List;

import com.idlesmoothie.model.economy.BasePriceCalculator;
import com.idlesmoothie.model.economy.BuyMode;
import com.idlesmoothie.model.economy.PriceCalculator;
import com.idlesmoothie.model.economy.UpgradeReport;
import com.idlesmoothie.model.economy.UpgradeService;
import com.idlesmoothie.model.economy.Wallet;
import com.idlesmoothie.model.manager.CapacityBoostAbility;
import com.idlesmoothie.model.manager.Manager;
import com.idlesmoothie.model.manager.ManagerAbility;
import com.idlesmoothie.model.manager.ManagerFactory;
import com.idlesmoothie.model.manager.SpeedBoostAbility;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Mine;
import com.idlesmoothie.model.station.Station;
import com.idlesmoothie.model.station.StationType;

/**
 * Single entry point of the model for the view (Facade pattern).
 * Builds the whole game, advances it each frame and turns the player's actions into model calls.
 * The view reads the state through {@link #getMine()} and {@link #getCoins()}.
 * It is also the originator of the Memento pattern: {@link #createSnapshot()} copies the state
 * and {@link #restore(GameSnapshot)} rebuilds it.
 */
public class Game {

    private final BalanceConfig config;
    private final Wallet wallet;
    private final Mine mine;
    private final UpgradeService upgradeService;
    private final ManagerFactory managerFactory;

    /**
     * Starts a new game with the default balance values.
     */
    public Game() {
        this(new BalanceConfig());
    }

    /**
     * Starts a new game with the given balance values and the default prices.
     *
     * @param config game balance values
     */
    public Game(BalanceConfig config) {
        this(config, new BasePriceCalculator(config.getSmoothieBasePrice()), new Wallet());
    }

    /**
     * Starts a new game with the given parts and real random managers.
     *
     * @param config          game balance values
     * @param priceCalculator gives the price of the smoothie sold
     * @param wallet          the player's coins
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public Game(BalanceConfig config, PriceCalculator priceCalculator, Wallet wallet) {
        this(config, priceCalculator, wallet, new ManagerFactory());
    }

    /**
     * Starts a new game with the given parts, so tests can choose the prices, the balance and the rolls.
     *
     * @param config          game balance values
     * @param priceCalculator gives the price of the smoothie sold
     * @param wallet          the player's coins
     * @param managerFactory  builds the managers the player buys
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public Game(BalanceConfig config, PriceCalculator priceCalculator, Wallet wallet,
                ManagerFactory managerFactory) {
        if (config == null || priceCalculator == null || wallet == null || managerFactory == null) {
            throw new IllegalArgumentException("Config, price calculator, wallet and manager factory must not be null");
        }
        this.config = config;
        this.wallet = wallet;
        this.mine = new Mine(priceCalculator, wallet, config);
        this.upgradeService = new UpgradeService(wallet, config.getUpgradeCostGrowth());
        this.managerFactory = managerFactory;
    }

    /**
     * Advances the game. Called by the game loop on every frame.
     *
     * @param dt elapsed time since the last frame, in seconds
     * @throws IllegalArgumentException if {@code dt} is negative
     */
    public void update(double dt) {
        mine.update(dt);
    }

    /**
     * Starts one round of the floor the player tapped.
     *
     * @param index index of the floor, 0 for the top one
     * @throws IllegalArgumentException if no unlocked floor has this index
     */
    public void tapFloor(int index) {
        if (index < 0 || index >= mine.getFloors().size()) {
            throw new IllegalArgumentException("No unlocked floor at index " + index);
        }
        mine.getFloors().get(index).start();
    }

    /**
     * Starts one trip of the elevator.
     */
    public void tapElevator() {
        mine.getElevator().start();
    }

    /**
     * Starts one sale of the sell station.
     */
    public void tapSellStation() {
        mine.getSellStation().start();
    }

    /**
     * @return {@code true} if the next floor can be unlocked and the player can pay for it
     */
    public boolean canBuyNextFloor() {
        return mine.canUnlockNextFloor() && wallet.canAfford(mine.getNextFloorCost());
    }

    /**
     * Pays for the next floor and unlocks it, or does nothing if {@link #canBuyNextFloor()} is false.
     *
     * @return the new floor, or {@code null} if it could not be bought
     */
    public Floor buyNextFloor() {
        if (!canBuyNextFloor() || !wallet.spendCoins(mine.getNextFloorCost())) {
            return null;
        }
        return mine.unlockNextFloor();
    }

    /**
     * Returns the price shown on an upgrade button, without buying anything.
     * In {@link BuyMode#MAX} with too few coins for one level, it is the price of one level.
     *
     * @param station the station to upgrade
     * @param mode    how many levels to buy: one, ten, or as many as the player can pay
     * @return the price in coins, possibly more than the player has
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public double getUpgradeCost(Station station, BuyMode mode) {
        requireUpgradable(station, mode);
        return upgradeService.previewCost(station, mode);
    }

    /**
     * @param station the station to upgrade
     * @param mode    how many levels to buy
     * @return {@code true} if the player can pay, used to grey out the button
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public boolean canUpgrade(Station station, BuyMode mode) {
        return wallet.canAfford(getUpgradeCost(station, mode));
    }

    /**
     * Pays for the levels and upgrades the station, or does nothing if the player cannot pay.
     *
     * @param station the station to upgrade
     * @param mode    how many levels to buy
     * @return the levels gained and the coins spent, both 0 if the player could not pay
     * @throws IllegalArgumentException if an argument is {@code null}
     */
    public UpgradeReport upgrade(Station station, BuyMode mode) {
        requireUpgradable(station, mode);
        return upgradeService.buy(station, mode);
    }

    /**
     * Copies the state worth saving into plain values.
     * Pulpie and elevator positions and crate contents are left out.
     *
     * @return the coins, the level of every station, and every manager with the station it works on
     */
    public GameSnapshot createSnapshot() {
        List<Integer> floorLevels = new ArrayList<>();
        List<ManagerSnapshot> managers = new ArrayList<>();
        for (Floor floor : mine.getFloors()) {
            floorLevels.add(floor.getLevel());
            addManager(managers, floor, floor.getIndex());
        }
        addManager(managers, mine.getElevator(), 0);
        addManager(managers, mine.getSellStation(), 0);
        return new GameSnapshot(wallet.getCoins(), floorLevels, mine.getElevator().getLevel(), mine.getSellStation().getLevel(), managers);
    }

    /**
     * Rebuilds a saved state on this game, which must be new: one floor, every station at level 1, no manager.
     * The whole snapshot is checked before anything changes, so a refused snapshot leaves the game untouched.
     * Each floor gets its level before the next one is unlocked, so the barriers accept it;
     * managers come back last, with their saved rarity and bonus instead of a new roll.
     *
     * @param snapshot the state to rebuild
     * @throws IllegalArgumentException if {@code snapshot} is {@code null} or holds values a game cannot have
     * @throws IllegalStateException    if this game is not new
     */
    public void restore(GameSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot must not be null");
        }
        // A new game has one floor, every station at level 1 and no manager. The coins are replaced anyway.
        GameSnapshot newGame = new GameSnapshot(wallet.getCoins(), List.of(1), 1, 1, List.of());
        if (!createSnapshot().equals(newGame)) {
            throw new IllegalStateException("A snapshot can only be restored on a new game");
        }
        checkRestorable(snapshot);
        wallet.resetCoins();
        wallet.addCoins(snapshot.coins());
        List<Integer> floorLevels = snapshot.floorLevels();
        for (int i = 0; i < floorLevels.size(); i++) {
            raiseLevel(mine.getFloors().get(i), floorLevels.get(i));
            if (i < floorLevels.size() - 1) {
                mine.unlockNextFloor();
            }
        }
        raiseLevel(mine.getElevator(), snapshot.elevatorLevel());
        raiseLevel(mine.getSellStation(), snapshot.sellLevel());
        for (ManagerSnapshot saved : snapshot.managers()) {
            Station station = stationOf(saved);
            ManagerAbility ability = saved.speedBonus()
                    ? new SpeedBoostAbility(saved.factor())
                    : new CapacityBoostAbility(saved.factor());
            station.assignManager(new Manager(saved.id(), saved.rarity(), saved.stationType(), ability));
            station.restoreManagersHired(saved.managersHired());
        }
    }

    /**
     * Returns the price of a manager for the station: a multiple of its first upgrade price,
     * growing with each manager already hired for it. Upgrading the station does not change it.
     *
     * @param station the station to automate
     * @return the price in coins
     * @throws IllegalArgumentException if the station is {@code null}
     */
    public double getManagerCost(Station station) {
        if (station == null) {
            throw new IllegalArgumentException("Station must not be null");
        }
        return station.getBaseUpgradeCost() * config.getManagerCostMultiplier()
            * Math.pow(config.getManagerCostGrowth(), station.getManagersHired());
    }

    /**
     * Tells whether the player can pay for a manager for the station, used to grey out the button.
     *
     * @param station the station to automate
     * @return {@code true} if the player has enough coins
     * @throws IllegalArgumentException if the station is {@code null}
     */
    public boolean canHireManager(Station station) {
        return wallet.canAfford(getManagerCost(station));
    }

    /**
     * Pays for a manager with a rolled rarity and bonus, and puts it on the station,
     * replacing the previous one if any.
     *
     * @param station the station to automate
     * @return the new manager, for the view to show the roll, or {@code null} if the player cannot pay
     * @throws IllegalArgumentException if the station is {@code null}
     */
    public Manager hireManager(Station station) {
        if (!wallet.spendCoins(getManagerCost(station))) {
            return null;
        }
        Manager manager = managerFactory.create(station.getType());
        station.assignManager(manager);
        return manager;
    }

    /**
     * @return the type of station that limits the income, to highlight it
     */
    public StationType getBottleneck() {
        return mine.findBottleneck();
    }

    /**
     * @return the player's coins
     */
    public double getCoins() {
        return wallet.getCoins();
    }

    /**
     * @return the mine, for the view to read the stations
     */
    public Mine getMine() {
        return mine;
    }

    private static void requireUpgradable(Station station, BuyMode mode) {
        if (station == null || mode == null) {
            throw new IllegalArgumentException("Station and mode must not be null");
        }
    }

    // A station starts at level 1, so a saved level n needs n - 1 more. upgrade(0) would throw.
    private static void raiseLevel(Station station, int level) {
        if (level > 1) {
            station.upgrade(level - 1);
        }
    }

    // Saves the station's manager, if it has one.
    private static void addManager(List<ManagerSnapshot> managers, Station station, int floorIndex) {
        Manager manager = station.getManager();
        if (manager == null) {
            return;
        }
        boolean speed = manager.getAbility() instanceof SpeedBoostAbility;
        double factor = speed
                ? ((SpeedBoostAbility) manager.getAbility()).getFactor()
                : ((CapacityBoostAbility) manager.getAbility()).getFactor();
        managers.add(new ManagerSnapshot(station.getType(), floorIndex, station.getManagersHired(),
                manager.getId(), manager.getRarity(), speed, factor));
    }

    // Finds the station a saved manager works on.
    private Station stationOf(ManagerSnapshot saved) {
        return switch (saved.stationType()) {
            case FLOOR -> mine.getFloors().get(saved.floorIndex());
            case ELEVATOR -> mine.getElevator();
            case SELL -> mine.getSellStation();
        };
    }

    // Refuses a damaged snapshot before anything changes, so it never leaves a half-loaded game.
    private void checkRestorable(GameSnapshot snapshot) {
        List<Integer> levels = snapshot.floorLevels();
        if (!(snapshot.coins() >= 0) || levels.isEmpty() || levels.size() > mine.getMaxFloors()) {
            throw new IllegalArgumentException("Invalid coins or number of floors in the save");
        }
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i) < 1) {
                throw new IllegalArgumentException("Invalid level for floor " + i);
            }
            if (i > 0 && mine.isBehindBarrier(i) && levels.get(i - 1) < config.getBarrierMinLevel()) {
                throw new IllegalArgumentException("Floor " + i + " is behind a barrier that was not crossed");
            }
        }
        if (snapshot.elevatorLevel() < 1 || snapshot.sellLevel() < 1) {
            throw new IllegalArgumentException("Invalid station level in the save");
        }
        for (ManagerSnapshot saved : snapshot.managers()) {
            if (saved.stationType() == null || saved.id() == null || saved.rarity() == null
                    || saved.factor() < 1 || saved.managersHired() < 1) {
                throw new IllegalArgumentException("Invalid manager in the save");
            }
            if (saved.stationType() == StationType.FLOOR
                    && (saved.floorIndex() < 0 || saved.floorIndex() >= levels.size())) {
                throw new IllegalArgumentException("A saved manager works on a floor that does not exist");
            }
        }
    }
}