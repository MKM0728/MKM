package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public final class GameOverScreen {

    private GameOverScreen() {}

    public static void show(int floorReached, int enemiesSlain, int turnsTaken,
                            Runnable onRestart, Runnable onMenu) {
        var title = new Text("YOU DIED");
        title.setFont(Font.font("Monospaced", 52));
        title.setFill(Color.RED);

        var stats = new Text(String.format(
            "Floor: %d  |  Enemies slain: %d  |  Turns: %d",
            floorReached, enemiesSlain, turnsTaken
        ));
        stats.setFont(Font.font("Monospaced", 14));
        stats.setFill(Color.LIGHTGRAY);

        var btnRestart = new Button("Try Again");
        var btnMenu = new Button("Main Menu");

        for (var btn : new Button[]{btnRestart, btnMenu}) {
            btn.setPrefWidth(200);
            btn.setFont(Font.font("Monospaced", 18));
            btn.setStyle("-fx-background-color: #333; -fx-text-fill: #eee; -fx-border-color: #555;");
        }

        btnRestart.setOnAction(e -> onRestart.run());
        btnMenu.setOnAction(e -> onMenu.run());

        var box = new VBox(25, title, stats, btnRestart, btnMenu);
        box.setAlignment(Pos.CENTER);

        var bg = FXGL.getGameScene().getRoot();
        bg.getChildren().clear();
        bg.getChildren().add(box);
        box.setLayoutX(FXGL.getAppWidth() / 2.0 - 150);
        box.setLayoutY(FXGL.getAppHeight() / 2.0 - 120);
    }
}
