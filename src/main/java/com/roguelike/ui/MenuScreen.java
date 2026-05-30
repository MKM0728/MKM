package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getUIFactoryService;

public final class MenuScreen {

    private MenuScreen() {}

    public static void show(Runnable onNewGame, Runnable onContinue, Runnable onQuit) {
        var ui = getUIFactoryService();

        var title = new Text("Roguelike Dungeon");
        title.setFont(Font.font("Monospaced", 48));
        title.setFill(Color.ORANGE);

        var subtitle = new Text("Descend into the darkness");
        subtitle.setFont(Font.font("Monospaced", 16));
        subtitle.setFill(Color.GRAY);

        var btnNew = new Button("New Game");
        var btnContinue = new Button("Continue");
        var btnQuit = new Button("Quit");

        for (var btn : new Button[]{btnNew, btnContinue, btnQuit}) {
            btn.setPrefWidth(200);
            btn.setFont(Font.font("Monospaced", 18));
            btn.setStyle("-fx-background-color: #333; -fx-text-fill: #eee; -fx-border-color: #555;");
        }
        btnContinue.setDisable(true);

        btnNew.setOnAction(e -> onNewGame.run());
        btnContinue.setOnAction(e -> onContinue.run());
        btnQuit.setOnAction(e -> onQuit.run());

        var box = new VBox(20, title, subtitle, btnNew, btnContinue, btnQuit);
        box.setAlignment(Pos.CENTER);

        var bg = FXGL.getGameScene().getRoot();
        bg.getChildren().clear();
        bg.getChildren().add(box);
        box.setLayoutX(FXGL.getAppWidth() / 2.0 - 150);
        box.setLayoutY(FXGL.getAppHeight() / 2.0 - 150);
    }
}
