package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public final class HudOverlay {

    private final Text hpText;
    private final Text floorText;
    private final Text turnText;

    public HudOverlay() {
        hpText = new Text();
        floorText = new Text();
        turnText = new Text();

        for (var t : new Text[]{hpText, floorText, turnText}) {
            t.setFont(Font.font("Monospaced", 16));
            t.setFill(Color.WHITE);
        }

        hpText.setTranslateX(10);
        hpText.setTranslateY(FXGL.getAppHeight() - 30);

        floorText.setTranslateX(10);
        floorText.setTranslateY(25);

        turnText.setTranslateX(FXGL.getAppWidth() - 160);
        turnText.setTranslateY(25);

        var scene = FXGL.getGameScene();
        scene.addUINode(hpText);
        scene.addUINode(floorText);
        scene.addUINode(turnText);
    }

    public void update(int hp, int maxHp, int floor, int turns) {
        String bar = hpBar(hp, maxHp);
        hpText.setText(String.format("HP: %s %d/%d", bar, hp, maxHp));
        hpText.setFill(hp < maxHp * 0.3 ? Color.RED : Color.WHITE);

        floorText.setText(String.format("Floor: %d", floor));
        turnText.setText(String.format("Turns: %d", turns));
    }

    private String hpBar(int hp, int maxHp) {
        int barLen = 10;
        int filled = (int) ((float) hp / maxHp * barLen);
        return "[" + "█".repeat(filled) + "░".repeat(barLen - filled) + "]";
    }

    public void remove() {
        var scene = FXGL.getGameScene();
        scene.removeUINode(hpText);
        scene.removeUINode(floorText);
        scene.removeUINode(turnText);
    }
}
