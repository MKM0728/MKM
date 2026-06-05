package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameConfig;
import com.roguelike.core.WeaponType;
import com.roguelike.entity.EnemyType;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public final class SettingsPanel {

    private static VBox panel;
    private static double brightness = 0.0;
    private static final ColorAdjust colorAdjust = new ColorAdjust();

    private SettingsPanel() {}

    public static void toggle(Runnable onReturnToMenu) {
        if (panel != null) {
            hide();
            return;
        }
        show(onReturnToMenu);
    }

    public static void show(Runnable onReturnToMenu) {
        hide();

        // Background overlay
        var bg = new Rectangle(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        bg.setFill(Color.rgb(0, 0, 0, 0.7));

        // Panel
        var panelBg = new Rectangle(400, 500);
        panelBg.setArcWidth(12);
        panelBg.setArcHeight(12);
        panelBg.setFill(Color.rgb(20, 20, 30, 0.95));
        panelBg.setStroke(Color.rgb(100, 100, 130, 0.7));
        panelBg.setStrokeWidth(2);

        var title = new Text("⚙ Settings");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 24));
        title.setFill(Color.GOLD);

        // Brightness section
        var brightnessLabel = new Text("☀ Brightness");
        brightnessLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        brightnessLabel.setFill(Color.WHITE);

        var brightnessValue = new Text(String.format("%.0f%%", (brightness + 1) * 50));
        brightnessValue.setFont(Font.font("Monospaced", 12));
        brightnessValue.setFill(Color.GRAY);

        var slider = new Slider(-0.8, 0.8, brightness);
        slider.setPrefWidth(300);
        slider.setStyle("-fx-control-inner-background: #333;");
        slider.valueProperty().addListener((obs, old, val) -> {
            brightness = val.doubleValue();
            colorAdjust.setBrightness(brightness);
            brightnessValue.setText(String.format("%.0f%%", (brightness + 1) * 50));
        });

        // Game Guide section
        var guideLabel = new Text("📖 Game Guide");
        guideLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        guideLabel.setFill(Color.WHITE);

        var tabButtons = new javafx.scene.layout.HBox(10);
        tabButtons.setAlignment(Pos.CENTER);
        var guideContent = new Text();
        guideContent.setFont(Font.font("Monospaced", 11));
        guideContent.setFill(Color.LIGHTGRAY);
        guideContent.setWrappingWidth(350);

        var btnEnemies = new Button("Enemies");
        var btnWeapons = new Button("Weapons");
        var btnHowTo = new Button("How to Play");

        for (var btn : new Button[]{btnEnemies, btnWeapons, btnHowTo}) {
            btn.setFont(Font.font("Monospaced", 12));
            btn.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
        }

        btnEnemies.setOnAction(e -> {
            btnEnemies.setStyle("-fx-background-color: #666; -fx-text-fill: #fff; -fx-cursor: hand;");
            btnWeapons.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
            btnHowTo.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
            guideContent.setText(enemyGuide());
        });

        btnWeapons.setOnAction(e -> {
            btnWeapons.setStyle("-fx-background-color: #666; -fx-text-fill: #fff; -fx-cursor: hand;");
            btnEnemies.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
            btnHowTo.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
            guideContent.setText(weaponGuide());
        });

        btnHowTo.setOnAction(e -> {
            btnHowTo.setStyle("-fx-background-color: #666; -fx-text-fill: #fff; -fx-cursor: hand;");
            btnEnemies.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
            btnWeapons.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
            guideContent.setText(howToPlayGuide());
        });

        tabButtons.getChildren().addAll(btnEnemies, btnWeapons, btnHowTo);
        guideContent.setText(enemyGuide());
        btnEnemies.setStyle("-fx-background-color: #666; -fx-text-fill: #fff; -fx-cursor: hand;");

        // Return to menu button
        var returnBtn = new Button("Return to Main Menu");
        returnBtn.setFont(Font.font("Monospaced", 14));
        returnBtn.setStyle("-fx-background-color: #833; -fx-text-fill: #faa; -fx-cursor: hand; -fx-border-color: #a55;");
        returnBtn.setOnAction(e -> {
            hide();
            onReturnToMenu.run();
        });

        // Close button
        var closeBtn = new Button("Close");
        closeBtn.setFont(Font.font("Monospaced", 14));
        closeBtn.setStyle("-fx-background-color: #444; -fx-text-fill: #eee; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> hide());

        panel = new VBox(12, title, brightnessLabel, brightnessValue, slider,
            new javafx.scene.text.Text(""), guideLabel, tabButtons, guideContent,
            new javafx.scene.text.Text(""), returnBtn, closeBtn);
        panel.setAlignment(Pos.CENTER);
        panel.setLayoutX(GameConfig.SCREEN_WIDTH / 2.0 - 200);
        panel.setLayoutY(60);
        panel.setMaxWidth(400);

        // Add to scene
        var group = new javafx.scene.Group(panelBg, panel);
        group.setTranslateX(panel.getLayoutX());
        group.setTranslateY(panel.getLayoutY());
        panel.setLayoutX(0);
        panel.setLayoutY(0);
        group.setViewOrder(-20000);

        // Store as combined group
        panel.setUserData(group);
        FXGL.getGameScene().getRoot().getChildren().add(group);
    }

    public static void hide() {
        if (panel != null) {
            var group = (javafx.scene.Group) panel.getUserData();
            if (group != null) FXGL.getGameScene().getRoot().getChildren().remove(group);
            panel = null;
        }
    }

    public static ColorAdjust getColorAdjust() { return colorAdjust; }

    public static boolean isVisible() { return panel != null; }

    private static String enemyGuide() {
        return "Enemies:\n\n" +
            "🦇 Red Bat\n" +
            "  HP: 30 | ATK: 5 | SPD: Fast\n" +
            "  Flying enemy, flaps wings.\n" +
            "  Drops: Sickle or Sword (40%)\n\n" +
            "💀 Purple Skeleton\n" +
            "  HP: 60 | ATK: 10 | DEF: 2\n" +
            "  Walks on legs, moderate speed.\n" +
            "  Drops: Sword or Axe (40%)\n\n" +
            "👻 White Ghost\n" +
            "  HP: 120 | ATK: 15\n" +
            "  One per floor. Guards the path.\n" +
            "  Locks room until defeated!\n" +
            "  Drops: Random weapon (60%)";
    }

    private static String weaponGuide() {
        return "Weapons:\n\n" +
            "👊 Fists\n" +
            "  DMG: 5 | Starting weapon\n" +
            "  Fast but weak, no special effect.\n\n" +
            "🔪 Sickle\n" +
            "  DMG: 10 | Slash attack\n" +
            "  Silver arc flash on hit.\n" +
            "  Drops from: Bat\n\n" +
            "⚔ Sword\n" +
            "  DMG: 15 | Thrust attack\n" +
            "  Gold flash, precise strike.\n" +
            "  Drops from: Bat, Skeleton\n\n" +
            "🪓 Axe\n" +
            "  DMG: 30 | Heavy slam\n" +
            "  Screen shake, orange burst.\n" +
            "  Drops from: Skeleton, Ghost";
    }

    private static String howToPlayGuide() {
        return "How to Play:\n\n" +
            "🎮 Controls:\n" +
            "  D-pad buttons: Hold to move\n" +
            "  Mouse click: Step to adjacent tile\n\n" +
            "🎯 Goal:\n" +
            "  Find the treasure chest on each\n" +
            "  floor to descend deeper.\n" +
            "  Clear all 3 floors to win!\n\n" +
            "💡 Tips:\n" +
            "  - Follow room arrows for direction\n" +
            "  - Pick up weapons from slain enemies\n" +
            "  - Ghost rooms lock you in - fight!\n" +
            "  - Use auto-save to continue later";
    }
}
