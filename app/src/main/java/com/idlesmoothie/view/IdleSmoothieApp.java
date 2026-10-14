package com.idlesmoothie.view;

import com.idlesmoothie.core.BalanceConfig;
import com.idlesmoothie.model.resource.Fruit;
import com.idlesmoothie.model.resource.Storage;
import com.idlesmoothie.model.station.Elevator;
import com.idlesmoothie.model.station.Floor;
import com.idlesmoothie.model.station.Station;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application: builds the main window.
 */
public class IdleSmoothieApp extends Application {

    /**
     * Builds the model, shows the main window and starts the game loop.
     *
     * @param stage the primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        // TODO temporary: build the model through Mine / Game once they exist
        BalanceConfig config = new BalanceConfig();
        List<Floor> floors = new ArrayList<>(List.of(new Floor(0, Fruit.STRAWBERRY, config)));
        Storage surface = new Storage(Double.POSITIVE_INFINITY);
        Elevator elevator = new Elevator(floors, surface, config);
        List<Station> stations = new ArrayList<>(floors);
        stations.add(elevator);

        MineView mineView = new MineView(540, 960, 15, floors, elevator, surface);
        StackPane root = new StackPane(mineView.getCanvas());
        stage.setTitle("Idle Smoothie");
        stage.setScene(new Scene(root, 540, 960));
        stage.setResizable(false);
        stage.show();

        new GameLoop(stations, List.of(mineView)).start();
    }
}
