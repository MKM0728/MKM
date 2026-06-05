package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public final class HudOverlay {

    private final Group group;
    private final Rectangle bg;
    private final Text hpText;
    private final Text weaponText;
    private final Text floorText;
    private final Text turnText;

    public HudOverlay() {
        group = new Group();

        // Background panel
        bg = new Rectangle(185, 72);
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        bg.setFill(Color.rgb(10, 10, 20, 0.85));
        bg.setStroke(Color.rgb(80, 80, 100, 0.7));
        bg.setStrokeWidth(1.5);

        hpText = new Text();
        weaponText = new Text();
        floorText = new Text();
        turnText = new Text();

        Font f = Font.font("Monospaced", FontWeight.BOLD, 13);

        hpText.setFont(f);
        hpText.setFill(Color.LIMEGREEN);
        hpText.setX(8);
        hpText.setY(16);

        weaponText.setFont(f);
        weaponText.setFill(Color.GOLD);
        weaponText.setX(8);
        weaponText.setY(34);

        floorText.setFont(f);
        floorText.setFill(Color.WHITE);
        floorText.setX(8);
        floorText.setY(52);

        turnText.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        turnText.setFill(Color.rgb(180, 180, 200));
        turnText.setX(FXGL.getAppWidth() - 120);
        turnText.setY(20);

        group.getChildren().addAll(bg, hpText, weaponText, floorText);
        group.setTranslateX(6);
        group.setTranslateY(6);
        group.setViewOrder(-10000);

        FXGL.getGameScene().getRoot().getChildren().add(group);
        FXGL.getGameScene().getRoot().getChildren().add(turnText);
    }

    public void update(int hp, int maxHp, int floor, int turns, String weaponLabel) {
        hpText.setText(String.format("♥ HP: %d/%d", hp, maxHp));
        hpText.setFill(hp < maxHp * 0.3 ? Color.RED : Color.LIMEGREEN);

        weaponText.setText(String.format("⚔ %s", weaponLabel));
        floorText.setText(String.format("◆ Floor %d", floor));
        turnText.setText(String.format("Turns: %d", turns));

        group.setVisible(true);
        turnText.setVisible(true);
        group.toFront();
        turnText.toFront();
    }

    public void toFront() {
        group.toFront();
        turnText.toFront();
    }

    public void hide() {
        group.setVisible(false);
        turnText.setVisible(false);
    }

    public void remove() {
        FXGL.getGameScene().getRoot().getChildren().remove(group);
        FXGL.getGameScene().getRoot().getChildren().remove(turnText);
    }
}
