package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public final class HudOverlay {

    private final Text hpText;
    private final Text weaponText;
    private final Text floorText;
    private final Text turnText;

    public HudOverlay() {
        hpText = new Text();
        weaponText = new Text();
        floorText = new Text();
        turnText = new Text();

        for (var t : new Text[]{hpText, weaponText, floorText, turnText}) {
            t.setFont(Font.font("Monospaced", 15));
            t.setFill(Color.WHITE);
        }

        hpText.setTranslateX(10);
        hpText.setTranslateY(20);
        hpText.setFill(Color.LIMEGREEN);

        weaponText.setTranslateX(10);
        weaponText.setTranslateY(40);
        weaponText.setFill(Color.GOLD);

        floorText.setTranslateX(10);
        floorText.setTranslateY(60);

        turnText.setTranslateX(FXGL.getAppWidth() - 160);
        turnText.setTranslateY(25);

        var scene = FXGL.getGameScene();
        scene.addUINode(hpText);
        scene.addUINode(weaponText);
        scene.addUINode(floorText);
        scene.addUINode(turnText);
    }

    public void update(int hp, int maxHp, int floor, int turns, String weaponLabel) {
        hpText.setText(String.format("HP: %d/%d", hp, maxHp));
        hpText.setFill(hp < maxHp * 0.3 ? Color.RED : Color.LIMEGREEN);

        weaponText.setText(String.format("Weapon: %s", weaponLabel));
        floorText.setText(String.format("Floor: %d", floor));
        turnText.setText(String.format("Turns: %d", turns));
    }

    public void remove() {
        var scene = FXGL.getGameScene();
        scene.removeUINode(hpText);
        scene.removeUINode(weaponText);
        scene.removeUINode(floorText);
        scene.removeUINode(turnText);
    }
}
