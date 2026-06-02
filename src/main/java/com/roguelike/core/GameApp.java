package com.roguelike.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;

import com.roguelike.ai.BehaviorTree;
import com.roguelike.ai.FovSystem;
import com.roguelike.ai.PathFinder;
import com.roguelike.combat.AttackSystem;
import com.roguelike.entity.*;
import com.roguelike.map.DungeonGenerator;
import com.roguelike.map.Tile;
import com.roguelike.save.SaveManager;
import com.roguelike.save.SaveData;
import com.roguelike.save.SaveManager;
import com.roguelike.ui.GameOverScreen;
import com.roguelike.ui.HudOverlay;
import com.roguelike.ui.MenuScreen;
import com.roguelike.ui.SettingsPanel;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

import com.roguelike.map.Room;

public class GameApp extends GameApplication {

    private GameState state = GameState.MENU;
    private Entity player;
    private final List<Entity> enemies = new ArrayList<>();
    private Tile[][] dungeon;
    private DungeonGenerator generator;
    private PathFinder pathFinder;
    private FovSystem fov;
    private HudOverlay hud;
    private int floor = 1;
    private int turns, enemiesSlain;
    private long seed;
    private boolean[][] visible, explored;
    private int playerMoves;
    private boolean animating;
    private int[] holdDir;
    private long lastMoveTime;

    private Group worldGroup;
    private Group playerGroup;
    private Canvas[] playerFrames;
    private int playerFrameIdx;
    private Rectangle playerHpBar;
    private Rectangle[][] tileRects;
    private final List<Group> enemyGroups = new ArrayList<>();
    private Group chestGroup;
    private int chestX, chestY;
    private WeaponType equippedWeapon = WeaponType.FISTS;
    private final List<WeaponDrop> groundWeapons = new ArrayList<>();
    private final List<Group> weaponDropGroups = new ArrayList<>();
    private HBox dpad;

    // Ghost room lock
    private Room ghostRoom;
    private boolean ghostRoomLocked;
    private record LockedDoor(int x, int y, Tile original) {}
    private final List<LockedDoor> lockedDoors = new ArrayList<>();

    // Direction hints
    private Room currentRoom;
    private Group arrowGroup;
    private long autoSaveTimer;

    // Settings button
    private Button settingsBtn;

    private record WeaponDrop(int x, int y, WeaponType type) {}

    private static final int TILESIZE = 32;
    private static final int COLS = GameConfig.SCREEN_WIDTH / TILESIZE;
    private static final int ROWS = GameConfig.SCREEN_HEIGHT / TILESIZE;
    private static final int PLAYER_SPEED = 2;
    private static final int BASE_ANIM_MS = 150;

    @Override
    protected void initSettings(GameSettings s) {
        s.setTitle(GameConfig.GAME_TITLE);
        s.setWidth(GameConfig.SCREEN_WIDTH);
        s.setHeight(GameConfig.SCREEN_HEIGHT);
        s.setVersion("0.1.0");
    }

    @Override
    protected void initInput() {
        // Empty - use game loop for hold detection + mouse for click
    }

    @Override
    protected void initGame() {
        FXGL.getGameScene().setBackgroundColor(Color.rgb(15, 15, 20));
        fov = new FovSystem(GameConfig.FOV_RADIUS);
        hud = new HudOverlay();

        // World group
        worldGroup = new Group();
        tileRects = new Rectangle[ROWS][COLS];
        for (int y = 0; y < ROWS; y++)
            for (int x = 0; x < COLS; x++) {
                var r = new Rectangle(TILESIZE - 1, TILESIZE - 1);
                r.setX(x * TILESIZE); r.setY(y * TILESIZE); r.setVisible(false);
                tileRects[y][x] = r; worldGroup.getChildren().add(r);
            }

        // Pixel player with HP bar
        playerGroup = new Group();
        int s = TILESIZE;
        playerFrames = new Canvas[]{
            makePlayerFrame(false, false, false, WeaponType.FISTS),
            makePlayerFrame(true, false, false, WeaponType.FISTS),
            makePlayerFrame(false, true, false, WeaponType.FISTS),
            makePlayerFrame(false, false, true, WeaponType.FISTS)
        };
        playerFrameIdx = 0;
        playerGroup.getChildren().add(playerFrames[0]);
        playerHpBar = new Rectangle(s - 3, 4, Color.LIMEGREEN);
        playerHpBar.setY(-8);
        playerGroup.getChildren().add(playerHpBar);
        playerGroup.setVisible(false);
        setPlayerScreenPos(COLS / 2, ROWS / 2);

        worldGroup.getChildren().add(playerGroup);
        worldGroup.setVisible(false);
        FXGL.getGameScene().getRoot().getChildren().add(worldGroup);

        // Mouse click
        worldGroup.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (state != GameState.PLAYING) return;
            int tx = (int)(e.getX() / TILESIZE), ty = (int)(e.getY() / TILESIZE);
            int dx = tx - COLS / 2, dy = ty - ROWS / 2;
            if (Math.abs(dx) + Math.abs(dy) == 1) tryMove(dx, dy);
        });

        // Hold-to-move D-pad
        dpad = new HBox(8);
        dpad.setAlignment(javafx.geometry.Pos.CENTER);
        dpad.setTranslateX(80);
        dpad.setTranslateY(GameConfig.SCREEN_HEIGHT - 55);
        for (var kv : new String[][]{{"▲", "U"}, {"▼", "D"}, {"◀", "L"}, {"▶", "R"}}) {
            var btn = new Button(kv[0]);
            btn.setFont(Font.font(22));
            btn.setStyle("-fx-background-color: rgba(15,15,15,0.9); -fx-text-fill: #0f0; -fx-border-color: #0f0; -fx-border-width: 2; -fx-min-width: 65; -fx-min-height: 50; -fx-cursor: hand;");
            int[] d = switch (kv[1]) { case "U" -> new int[]{0, -1}; case "D" -> new int[]{0, 1}; case "L" -> new int[]{-1, 0}; default -> new int[]{1, 0}; };
            btn.setOnMousePressed(ev -> hold(d[0], d[1]));
            btn.setOnMouseReleased(ev -> release());
            dpad.getChildren().add(btn);
        }
        dpad.setViewOrder(-9999);
        FXGL.getGameScene().getRoot().getChildren().add(dpad);

        // Settings button (top-right)
        settingsBtn = new Button("⚙");
        settingsBtn.setFont(Font.font(20));
        settingsBtn.setStyle("-fx-background-color: rgba(20,20,30,0.85); -fx-text-fill: #888; -fx-border-color: #555; -fx-border-width: 1; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 40;");
        settingsBtn.setTranslateX(GameConfig.SCREEN_WIDTH - 48);
        settingsBtn.setTranslateY(6);
        settingsBtn.setViewOrder(-12000);
        settingsBtn.setOnAction(e -> {
            if (SettingsPanel.isVisible()) {
                SettingsPanel.hide();
                if (state == GameState.PAUSED) { state = GameState.PLAYING; }
            } else {
                SettingsPanel.show(() -> { SettingsPanel.hide(); showMenu(); });
                if (state == GameState.PLAYING) state = GameState.PAUSED;
            }
        });
        FXGL.getGameScene().getRoot().getChildren().add(settingsBtn);

        // Apply brightness effect
        FXGL.getGameScene().getRoot().setEffect(SettingsPanel.getColorAdjust());

        showMenu();
    }

    @Override
    protected void onUpdate(double tpf) {
        if (dpad != null) dpad.toFront();
        if (hud != null) hud.toFront();
        if (settingsBtn != null) settingsBtn.toFront();

        if (state == GameState.PAUSED) return;

        if (state != GameState.PLAYING || animating) return;

        if (holdDir != null && playerMoves > 0) {
            long now = java.lang.System.currentTimeMillis();
            if (now - lastMoveTime > 120) { tryMove(holdDir[0], holdDir[1]); lastMoveTime = now; }
        }

        if (playerMoves <= 0) {
            enemyTurns(); playerMoves = PLAYER_SPEED;
            renderAll(); updateHud(); checkState();
        }

        // Auto-save every 10 seconds
        autoSaveTimer++;
        if (autoSaveTimer > 600) { autoSaveTimer = 0; autoSave(); }
    }

    // --- Pixel art ---
    private Canvas makePlayerFrame(boolean wl, boolean wr, boolean atk, WeaponType weapon) {
        Canvas c = new Canvas(TILESIZE, TILESIZE);
        GraphicsContext g = c.getGraphicsContext2D();
        int p = 3;
        g.setFill(Color.rgb(30, 35, 30)); g.fillRect(0, 0, TILESIZE, TILESIZE);
        // Hair
        g.setFill(Color.rgb(60, 50, 30)); g.fillRect(p * 4, p * 0, p * 3, p * 1); g.fillRect(p * 3, p * 1, p * 5, p * 1);
        // Face
        g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 3, p * 2, p * 5, p * 3);
        g.setFill(Color.BLACK); g.fillRect(p * 4, p * 3, p * 1, p * 1); g.fillRect(p * 6, p * 3, p * 1, p * 1);
        // Body
        g.setFill(Color.rgb(30, 140, 30)); g.fillRect(p * 3, p * 5, p * 5, p * 4);
        g.setFill(Color.rgb(80, 50, 20)); g.fillRect(p * 3, p * 5, p * 5, p * 1);
        // Left arm
        g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 1, p * 5, p * 2, p * 3);
        // Right arm + weapon
        if (atk) {
            switch (weapon) {
                case FISTS -> {
                    g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 8, p * 4, p * 2, p * 3);
                    g.setFill(Color.rgb(255, 200, 160)); g.fillRect(p * 10, p * 5, p * 1, p * 1);
                }
                case SICKLE -> {
                    g.setFill(Color.rgb(120, 70, 30)); g.fillRect(p * 8, p * 3, p * 1, p * 4);
                    g.setFill(Color.rgb(180, 180, 200)); g.fillRect(p * 5, p * 2, p * 3, p * 1);
                    g.setFill(Color.rgb(160, 160, 180)); g.fillRect(p * 5, p * 3, p * 1, p * 1);
                    g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 8, p * 6, p * 1, p * 2);
                }
                case SWORD -> {
                    g.setFill(Color.rgb(210, 210, 230)); g.fillRect(p * 9, p * 1, p * 1, p * 5);
                    g.setFill(Color.rgb(200, 170, 50)); g.fillRect(p * 8, p * 5, p * 3, p * 1);
                    g.setFill(Color.rgb(100, 60, 30)); g.fillRect(p * 9, p * 6, p * 1, p * 3);
                    g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 9, p * 6, p * 1, p * 1);
                }
                case AXE -> {
                    g.setFill(Color.rgb(120, 70, 30)); g.fillRect(p * 8, p * 2, p * 1, p * 6);
                    g.setFill(Color.rgb(180, 180, 200)); g.fillRect(p * 6, p * 1, p * 4, p * 2);
                    g.setFill(Color.rgb(160, 160, 180)); g.fillRect(p * 7, p * 3, p * 1, p * 1);
                    g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 8, p * 6, p * 1, p * 2);
                }
            }
        } else {
            g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 8, p * 5, p * 2, p * 3);
        }
        // Legs
        g.setFill(Color.rgb(60, 40, 20));
        if (wl) { g.fillRect(p * 3, p * 9, p * 2, p * 2); g.fillRect(p * 6, p * 9, p * 2, p * 1); }
        else if (wr) { g.fillRect(p * 3, p * 9, p * 2, p * 1); g.fillRect(p * 6, p * 9, p * 2, p * 2); }
        else { g.fillRect(p * 3, p * 9, p * 2, p * 2); g.fillRect(p * 6, p * 9, p * 2, p * 2); }
        // Shoes
        g.setFill(Color.rgb(50, 30, 10)); g.fillRect(p * 2, p * 10, p * 3, p * 1); g.fillRect(p * 5, p * 10, p * 3, p * 1);
        return c;
    }

    private Canvas makeEnemyFrame(String kind, boolean alt) {
        Canvas c = new Canvas(TILESIZE, TILESIZE);
        GraphicsContext g = c.getGraphicsContext2D();
        int p = 3;
        g.setFill(Color.rgb(30, 35, 30)); g.fillRect(0, 0, TILESIZE, TILESIZE);
        switch (kind) {
            case "bat" -> {
                g.setFill(Color.rgb(200, 30, 30)); g.fillRect(p * 3, p * 2, p * 5, p * 4);
                g.fillRect(p * 4, p * 1, p * 3, p * 2);
                g.setFill(Color.BLACK); g.fillRect(p * 5, p * 2, p * 2, p * 1);
                if (alt) {
                    g.setFill(Color.rgb(240, 50, 50)); g.fillRect(p * 1, p * 3, p * 2, p * 2);
                } else {
                    g.setFill(Color.rgb(240, 50, 50)); g.fillRect(p * 1, p * 5, p * 2, p * 3);
                }
                g.fillRect(p * 8, alt ? p * 3 : p * 5, p * 2, alt ? p * 2 : p * 3);
            }
            case "skeleton" -> {
                g.setFill(Color.rgb(170, 80, 170));
                g.fillRect(p * 4, p * 0, p * 3, p * 2);
                g.setFill(Color.BLACK); g.fillRect(p * 5, p * 1, p * 1, p * 1); g.fillRect(p * 6, p * 1, p * 1, p * 1);
                g.fillRect(p * 3, p * 2, p * 5, p * 5); g.fillRect(p * 2, p * 3, p * 1, p * 3); g.fillRect(p * 8, p * 3, p * 1, p * 3);
                if (alt) { g.fillRect(p * 3, p * 7, p * 2, p * 2); g.fillRect(p * 6, p * 7, p * 2, p * 1); }
                else { g.fillRect(p * 3, p * 7, p * 2, p * 2); g.fillRect(p * 6, p * 7, p * 2, p * 2); }
            }
            case "ghost" -> {
                // Translucent white ghost
                g.setFill(Color.rgb(220, 230, 255, 0.7));
                // Head
                g.fillRect(p * 4, p * 1, p * 3, p * 2);
                g.fillRect(p * 3, p * 2, p * 5, p * 1);
                // Body
                g.fillRect(p * 3, p * 3, p * 5, p * 4);
                // Arms
                g.fillRect(p * 1, p * 3, p * 2, p * 3);
                g.fillRect(p * 8, p * 3, p * 2, p * 3);
                // Eyes (dark hollow)
                g.setFill(Color.rgb(60, 40, 80));
                g.fillRect(p * 4, p * 2, p * 1, p * 1);
                g.fillRect(p * 6, p * 2, p * 1, p * 1);
                // Wavy bottom
                g.setFill(Color.rgb(220, 230, 255, alt ? 0.5 : 0.7));
                if (alt) {
                    g.fillRect(p * 2, p * 7, p * 7, p * 2);
                    g.fillRect(p * 3, p * 9, p * 5, p * 2);
                    g.fillRect(p * 1, p * 7, p * 1, p * 1);
                    g.fillRect(p * 9, p * 7, p * 1, p * 1);
                } else {
                    g.fillRect(p * 3, p * 7, p * 5, p * 3);
                    g.fillRect(p * 4, p * 9, p * 3, p * 2);
                }
                // Glow
                g.setFill(Color.rgb(200, 210, 255, 0.3));
                g.fillRect(p * 2, p * 1, p * 7, p * 10);
            }
            default -> {
                g.setFill(Color.rgb(80, 200, 80));
                if (alt) g.fillRect(p * 2, p * 3, p * 7, p * 6);
                else g.fillRect(p * 3, p * 2, p * 5, p * 7);
                g.setFill(Color.BLACK); g.fillRect(p * 4, p * 4, p * 2, p * 2);
            }
        }
        return c;
    }

    private Canvas makeTreasureChest() {
        Canvas c = new Canvas(TILESIZE, TILESIZE);
        GraphicsContext g = c.getGraphicsContext2D();
        int p = 3;
        g.setFill(Color.rgb(30, 35, 30)); g.fillRect(0, 0, TILESIZE, TILESIZE);
        // Chest body
        g.setFill(Color.rgb(139, 90, 30)); g.fillRect(p * 2, p * 3, p * 7, p * 6);
        g.setFill(Color.rgb(110, 70, 20)); g.fillRect(p * 2, p * 3, p * 1, p * 6);
        // Gold lid
        g.setFill(Color.rgb(200, 170, 50)); g.fillRect(p * 3, p * 2, p * 5, p * 2);
        g.setFill(Color.rgb(180, 150, 40)); g.fillRect(p * 3, p * 2, p * 1, p * 2);
        // Lock
        g.setFill(Color.rgb(255, 215, 0)); g.fillRect(p * 5, p * 5, p * 2, p * 2);
        g.setFill(Color.rgb(200, 170, 0)); g.fillRect(p * 5, p * 5, p * 1, p * 1);
        // Sparkle
        g.setFill(Color.rgb(255, 255, 200)); g.fillRect(p * 6, p * 6, p * 1, p * 1);
        return c;
    }

    private void refreshAttackFrame() {
        Canvas old = playerFrames[3];
        playerFrames[3] = makePlayerFrame(false, false, true, equippedWeapon);
        if (playerFrameIdx == 3) {
            playerGroup.getChildren().remove(old);
            playerGroup.getChildren().add(0, playerFrames[3]);
        }
    }

    private Canvas makeWeaponSprite(WeaponType type) {
        Canvas c = new Canvas(TILESIZE, TILESIZE);
        GraphicsContext g = c.getGraphicsContext2D();
        int p = 3, s = TILESIZE;
        g.setFill(Color.rgb(30, 35, 30)); g.fillRect(0, 0, s, s);
        // Glow under weapon
        g.setFill(Color.rgb(60, 55, 40)); g.fillRect(p * 3, p * 6, p * 5, p * 3);
        switch (type) {
            case SICKLE -> {
                g.setFill(Color.rgb(120, 70, 30)); g.fillRect(p * 6, p * 3, p * 1, p * 5);
                g.setFill(Color.rgb(180, 180, 200)); g.fillRect(p * 3, p * 2, p * 3, p * 1);
                g.setFill(Color.rgb(160, 160, 180)); g.fillRect(p * 3, p * 3, p * 1, p * 1);
            }
            case SWORD -> {
                g.setFill(Color.rgb(210, 210, 230)); g.fillRect(p * 5, p * 1, p * 1, p * 6);
                g.setFill(Color.rgb(200, 170, 50)); g.fillRect(p * 4, p * 6, p * 3, p * 1);
                g.setFill(Color.rgb(100, 60, 30)); g.fillRect(p * 5, p * 7, p * 1, p * 2);
            }
            case AXE -> {
                g.setFill(Color.rgb(120, 70, 30)); g.fillRect(p * 5, p * 2, p * 1, p * 7);
                g.setFill(Color.rgb(180, 180, 200)); g.fillRect(p * 3, p * 1, p * 5, p * 2);
            }
        }
        return c;
    }

    private void setPlayerFrame(int i) {
        if (playerFrameIdx == i) return;
        playerGroup.getChildren().remove(playerFrames[playerFrameIdx]);
        playerFrames[i].setTranslateX(0); playerFrames[i].setTranslateY(0);
        playerGroup.getChildren().add(0, playerFrames[i]);
        playerFrameIdx = i;
    }

    private void setPlayerScreenPos(int sx, int sy) {
        playerGroup.setTranslateX(sx * TILESIZE + 1);
        playerGroup.setTranslateY(sy * TILESIZE + 1);
    }

    // --- Input ---
    private void hold(int dx, int dy) { holdDir = new int[]{dx, dy}; lastMoveTime = 0; setPlayerFrame(1); tryMove(dx, dy); }
    private void release() { holdDir = null; if (!animating) setPlayerFrame(0); }

    // --- Game flow ---
    private void showMenu() {
        state = GameState.MENU; GameOverScreen.hide(); hud.hide();
        if (settingsBtn != null) settingsBtn.setVisible(false);
        SettingsPanel.hide();
        worldGroup.setVisible(false);
        boolean hasSave = SaveManager.slotExists("auto");
        MenuScreen.show(this::newGame, () -> { loadGame("auto"); if (settingsBtn != null) settingsBtn.setVisible(true); }, () -> FXGL.getGameController().exit(), hasSave);
    }

    private void newGame() {
        MenuScreen.hide(); GameOverScreen.hide();
        seed = java.lang.System.currentTimeMillis(); floor = 1; turns = 0; enemiesSlain = 0;
        equippedWeapon = WeaponType.FISTS; refreshAttackFrame();
        try { SaveManager.deleteSlot("auto"); } catch (Exception ignored) {}
        if (settingsBtn != null) settingsBtn.setVisible(true);
        startFloor();
    }

    private void startFloor() {
        holdDir = null; animating = false;
        enemies.clear(); enemyGroups.forEach(g -> worldGroup.getChildren().remove(g)); enemyGroups.clear();
        if (chestGroup != null) { worldGroup.getChildren().remove(chestGroup); chestGroup = null; }
        weaponDropGroups.forEach(g -> worldGroup.getChildren().remove(g)); weaponDropGroups.clear(); groundWeapons.clear();
        ghostRoom = null; ghostRoomLocked = false; lockedDoors.clear();
        if (arrowGroup != null) { worldGroup.getChildren().remove(arrowGroup); arrowGroup = null; }
        currentRoom = null;

        generator = new DungeonGenerator(GameConfig.mapWidth(floor), GameConfig.mapHeight(floor), seed + floor);
        dungeon = generator.generate(GameConfig.MAX_BSP_DEPTH);
        pathFinder = new PathFinder(dungeon);
        explored = new boolean[GameConfig.mapHeight(floor)][GameConfig.mapWidth(floor)];

        player = PlayerFactory.create(generator.stairsUpX(), generator.stairsUpY());
        spawnEnemies();

        for (var e : enemies) {
            var ec = e.get(EnemyComponent.class);
            String kind = ec != null ? ec.type().kind() : "slime";
            var c0 = makeEnemyFrame(kind, false);
            var g = new Group(c0); g.setVisible(false);
            enemyGroups.add(g); worldGroup.getChildren().add(g);
        }

        // Spawn chest far from player
        int px = playerX(), py = playerY(), mw = GameConfig.mapWidth(floor), mh = GameConfig.mapHeight(floor);
        do { chestX = 2 + (int)(Math.random() * (mw - 4)); chestY = 2 + (int)(Math.random() * (mh - 4));
        } while (dungeon[chestY][chestX] != Tile.FLOOR
            || (Math.abs(chestX - px) + Math.abs(chestY - py) < Math.min(mw, mh) / 2)
            || enemyAt(chestX, chestY));
        chestGroup = new Group(makeTreasureChest());
        chestGroup.setVisible(false);
        worldGroup.getChildren().add(chestGroup);

        visible = fov.compute(dungeon, playerX(), playerY()); markExplored();
        worldGroup.setVisible(true); playerGroup.toFront();
        state = GameState.PLAYING; playerMoves = PLAYER_SPEED;
        renderAll(); updateHud();
    }

    private void spawnEnemies() {
        int count = GameConfig.enemyCount(floor);
        var regTypes = new EnemyType[]{EnemyType.BAT, EnemyType.SKELETON};
        for (int i = 0; i < count; i++) {
            int x, y;
            do { x = 2 + (int)(Math.random() * (GameConfig.mapWidth(floor) - 4)); y = 2 + (int)(Math.random() * (GameConfig.mapHeight(floor) - 4));
            } while (dungeon[y][x] != Tile.FLOOR || (x == playerX() && y == playerY()));
            enemies.add(EnemyFactory.create(regTypes[(int)(Math.random() * regTypes.length)], x, y));
        }

        // Spawn ghost in a mandatory room on the path to the chest
        ghostRoom = findGhostRoom();
        ghostRoomLocked = false;
        lockedDoors.clear();
        if (ghostRoom != null) {
            int gx = ghostRoom.centerX(), gy = ghostRoom.centerY();
            enemies.add(EnemyFactory.create(EnemyType.GHOST, gx, gy));
        }
    }

    private Room findGhostRoom() {
        // Find a room that's on the path from player start to chest
        var rooms = generator.allRooms();
        if (rooms.size() < 2) return null;
        Room start = generator.startRoom();
        int chestX = this.chestX, chestY = this.chestY;
        // Pick a room midway between start and chest, not the start room
        Room best = null;
        double bestDist = Double.MAX_VALUE;
        double midX = (start.centerX() + chestX) / 2.0;
        double midY = (start.centerY() + chestY) / 2.0;
        for (var r : rooms) {
            if (r.equals(start)) continue;
            double d = Math.abs(r.centerX() - midX) + Math.abs(r.centerY() - midY);
            if (d < bestDist) { bestDist = d; best = r; }
        }
        return best;
    }

    // --- Player actions ---
    private void tryMove(int dx, int dy) {
        if (state != GameState.PLAYING || playerMoves <= 0 || animating) return;
        int nx = playerX() + dx, ny = playerY() + dy;
        if (ny < 0 || ny >= dungeon.length || nx < 0 || nx >= dungeon[0].length) return;
        if (dungeon[ny][nx] == Tile.STAIRS_DOWN) { floor++; startFloor(); return; }
        if (!dungeon[ny][nx].isWalkable()) return;

        // Ghost room lock: prevent leaving the room while locked
        if (ghostRoomLocked && !inGhostRoom(nx, ny) && inGhostRoom(playerX(), playerY())) return;

        // Check treasure chest
        if (chestGroup != null && chestGroup.isVisible() && chestX == nx && chestY == ny) {
            collectChest(); return;
        }

        // Pick up weapon
        for (int i = 0; i < groundWeapons.size(); i++) {
            var wd = groundWeapons.get(i);
            if (wd.x() == nx && wd.y() == ny) {
                equippedWeapon = wd.type();
                refreshAttackFrame();
                worldGroup.getChildren().remove(weaponDropGroups.get(i));
                groundWeapons.remove(i); weaponDropGroups.remove(i);
                showFloatingText(nx, ny, wd.type().label() + "!");
                break;
            }
        }

        for (var enemy : enemies) {
            var ep = enemy.get(PositionComponent.class);
            if (ep != null && ep.x() == nx && ep.y() == ny) {
                attackEnemy(enemy, nx, ny); return;
            }
        }

        movePlayer(nx, ny);
    }

    private void attackEnemy(Entity enemy, int nx, int ny) {
        var hp = enemy.get(HealthComponent.class);
        if (hp == null || !hp.isAlive()) return;
        setPlayerFrame(3);
        int dmg = equippedWeapon.damage();
        String fxColor, fxText;
        switch (equippedWeapon) {
            case FISTS -> {
                fxColor = "yellow"; fxText = "-fx-effect: dropshadow(gaussian, yellow, 8, 0.7, 0, 0);";
            }
            case SICKLE -> {
                fxColor = "silver"; fxText = "-fx-effect: dropshadow(gaussian, silver, 14, 0.9, 0, 0);";
            }
            case SWORD -> {
                fxColor = "gold"; fxText = "-fx-effect: dropshadow(gaussian, gold, 16, 1.0, 0, 0);";
            }
            case AXE -> {
                fxColor = "orangered"; fxText = "-fx-effect: dropshadow(gaussian, orangered, 22, 1.0, 0, 0);";
                // Screen shake for axe
                var root = FXGL.getGameScene().getRoot();
                root.setTranslateX(4); root.setTranslateY(2);
                new Thread(() -> {
                    try { Thread.sleep(60); } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> { root.setTranslateX(0); root.setTranslateY(0); });
                }).start();
            }
            default -> {
                fxColor = "white"; fxText = "-fx-effect: dropshadow(gaussian, white, 10, 0.8, 0, 0);";
            }
        }
        playerGroup.setStyle(fxText);
        hp.takeDamage(dmg);
        playerMoves--; turns++;
        showFloatingText(nx, ny, "-" + dmg, fxColor);
        if (hp != null && !hp.isAlive()) {
            enemiesSlain++;
            var ec = enemy.get(EnemyComponent.class);
            // Ghost check: unlock room on death
            if (ec != null && ec.type() == EnemyType.GHOST && ghostRoomLocked) {
                unlockGhostRoom();
            }
            tryDropWeapon(nx, ny, enemy);
        }
        new Thread(() -> {
            try { Thread.sleep(400); } catch (Exception ignored) {}
            javafx.application.Platform.runLater(() -> { playerGroup.setStyle(null); if (holdDir == null && !animating) setPlayerFrame(0); });
        }).start();
    }

    private void movePlayer(int nx, int ny) {
        animating = true;
        int fx = playerX(), fy = playerY();
        setPlayerFrame(playerFrameIdx == 1 ? 2 : 1);
        long dur = (long)(BASE_ANIM_MS / PLAYER_SPEED);
        double startX = playerGroup.getTranslateX();
        double startY = playerGroup.getTranslateY();
        var anim = new TranslateTransition(Duration.millis(dur), playerGroup);
        anim.setFromX(startX); anim.setFromY(startY);
        anim.setToX(startX + (nx - fx) * TILESIZE);
        anim.setToY(startY + (ny - fy) * TILESIZE);
        anim.setOnFinished(e -> {
            player.get(PositionComponent.class).set(nx, ny);
            visible = fov.compute(dungeon, nx, ny); markExplored();
            playerMoves--; turns++;
            checkRoomChange(nx, ny);
            renderAll();
            animating = false;
            if (holdDir == null) setPlayerFrame(0);
        });
        anim.play();
    }

    private void checkRoomChange(int px, int py) {
        Room newRoom = generator.roomAt(px, py);
        if (newRoom != null && !newRoom.equals(currentRoom)) {
            currentRoom = newRoom;
            showDirectionHint();
            // Ghost room entry check
            if (ghostRoom != null && ghostRoom.equals(newRoom) && !ghostRoomLocked) {
                lockGhostRoom();
            }
        }
    }

    private void showDirectionHint() {
        if (chestGroup == null) return;
        int dir = chestDirection(playerX(), playerY());
        String[] dirNames = {"Up", "Down", "Left", "Right"};
        showFloatingText(playerX(), playerY(), "→ " + dirNames[dir >= 0 ? dir : 0], "gold");
    }

    private void lockGhostRoom() {
        if (ghostRoom == null) return;
        ghostRoomLocked = true;
        // Lock room exits by finding corridor tiles adjacent to the room
        int rx = ghostRoom.x(), ry = ghostRoom.y(), rw = ghostRoom.width(), rh = ghostRoom.height();
        for (int y = ry; y < ry + rh; y++) {
            for (int x = rx; x < rx + rw; x++) {
                // Check 4-neighbor tiles outside the room
                for (int[] d : new int[][]{{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (ny < 0 || ny >= dungeon.length || nx < 0 || nx >= dungeon[0].length) continue;
                    // If outside room and walkable (corridor), block it
                    if ((nx < rx || nx >= rx + rw || ny < ry || ny >= ry + rh)
                        && (dungeon[ny][nx] == Tile.CORRIDOR || dungeon[ny][nx] == Tile.DOOR)) {
                        lockedDoors.add(new LockedDoor(nx, ny, dungeon[ny][nx]));
                        dungeon[ny][nx] = Tile.WALL;
                    }
                }
            }
        }
        showFloatingText(playerX(), playerY(), "LOCKED!", "white");
    }

    private void unlockGhostRoom() {
        ghostRoomLocked = false;
        for (var ld : lockedDoors) {
            if (ld.y >= 0 && ld.y < dungeon.length && ld.x >= 0 && ld.x < dungeon[0].length) {
                dungeon[ld.y][ld.x] = ld.original;
            }
        }
        lockedDoors.clear();
        showFloatingText(playerX(), playerY(), "UNLOCKED!", "lime");
    }

    private boolean inGhostRoom(int x, int y) {
        if (ghostRoom == null) return false;
        return x >= ghostRoom.x() && x < ghostRoom.x() + ghostRoom.width()
            && y >= ghostRoom.y() && y < ghostRoom.y() + ghostRoom.height();
    }

    private void showFloatingText(int mx, int my, String text) {
        showFloatingText(mx, my, text, "yellow");
    }

    private void showFloatingText(int mx, int my, String text, String colorName) {
        int sx = mx - (playerX() - COLS / 2), sy = my - (playerY() - ROWS / 2);
        javafx.application.Platform.runLater(() -> {
            var t = new javafx.scene.text.Text(text);
            t.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
            t.setFill(Color.valueOf(colorName));
            t.setX(sx * TILESIZE + 4); t.setY(sy * TILESIZE - 6);
            worldGroup.getChildren().add(t);
            new Thread(() -> {
                try { Thread.sleep(500); } catch (Exception ignored) {}
                javafx.application.Platform.runLater(() -> worldGroup.getChildren().remove(t));
            }).start();
        });
    }

    // --- Rendering ---
    private void renderAll() {
        int ox = playerX() - COLS / 2, oy = playerY() - ROWS / 2;
        for (int sy = 0; sy < ROWS; sy++)
            for (int sx = 0; sx < COLS; sx++) {
                int mx = ox + sx, my = oy + sy;
                var r = tileRects[sy][sx];
                if (mx < 0 || mx >= GameConfig.mapWidth(floor) || my < 0 || my >= GameConfig.mapHeight(floor)) { r.setVisible(false); continue; }
                if (my >= visible.length || mx >= visible[my].length || my >= dungeon.length || mx >= dungeon[my].length) { r.setVisible(false); continue; }
                if (visible[my][mx]) {
                    r.setVisible(true);
                    r.setFill(switch (dungeon[my][mx]) {
                        case WALL -> Color.rgb(60, 60, 65); case FLOOR -> Color.rgb(50, 45, 38);
                        case CORRIDOR -> Color.rgb(42, 37, 32); case DOOR -> Color.rgb(100, 70, 30);
                        case STAIRS_DOWN, STAIRS_UP -> Color.rgb(180, 160, 30);
                        case WATER -> Color.rgb(20, 60, 100); default -> Color.BLACK;
                    });
                } else if (explored[my][mx]) { r.setVisible(true); r.setFill(Color.rgb(30, 27, 25)); }
                else { r.setVisible(false); }
            }

        playerGroup.setVisible(true);
        var pHp = player.get(HealthComponent.class);
        playerHpBar.setWidth((TILESIZE - 3) * Math.max(0.1, (double)pHp.hp() / pHp.maxHp()));
        playerHpBar.setFill(pHp.hpRatio() < 0.3 ? Color.RED : Color.LIMEGREEN);
        setPlayerScreenPos(COLS / 2, ROWS / 2);

        for (int i = 0; i < enemyGroups.size(); i++) {
            if (i >= enemies.size()) { enemyGroups.get(i).setVisible(false); continue; }
            var e = enemies.get(i); var ep = e.get(PositionComponent.class);
            if (ep == null || !visible[ep.y()][ep.x()]) { enemyGroups.get(i).setVisible(false); continue; }
            int sx = ep.x() - ox, sy = ep.y() - oy;
            if (sx < 0 || sx >= COLS || sy < 0 || sy >= ROWS) { enemyGroups.get(i).setVisible(false); continue; }
            enemyGroups.get(i).setTranslateX(sx * TILESIZE + 1); enemyGroups.get(i).setTranslateY(sy * TILESIZE + 1);
            enemyGroups.get(i).setVisible(true);
        }

        // Treasure chest visibility
        if (chestGroup != null) {
            int csx = chestX - ox, csy = chestY - oy;
            if (csx >= 0 && csx < COLS && csy >= 0 && csy < ROWS && (visible[chestY][chestX] || explored[chestY][chestX])) {
                chestGroup.setTranslateX(csx * TILESIZE + 1);
                chestGroup.setTranslateY(csy * TILESIZE + 1);
                chestGroup.setVisible(true);
                if (visible[chestY][chestX]) chestGroup.toFront();
            } else {
                chestGroup.setVisible(false);
            }
        }

        // Weapon drops
        for (int i = 0; i < weaponDropGroups.size(); i++) {
            if (i >= groundWeapons.size()) { weaponDropGroups.get(i).setVisible(false); continue; }
            var wd = groundWeapons.get(i);
            int wsx = wd.x() - ox, wsy = wd.y() - oy;
            if (wsx >= 0 && wsx < COLS && wsy >= 0 && wsy < ROWS && visible[wd.y()][wd.x()]) {
                weaponDropGroups.get(i).setTranslateX(wsx * TILESIZE + 1);
                weaponDropGroups.get(i).setTranslateY(wsy * TILESIZE + 1);
                weaponDropGroups.get(i).setVisible(true);
            } else {
                weaponDropGroups.get(i).setVisible(false);
            }
        }

        // Direction arrow hint
        updateArrowHint(ox, oy);
    }

    private void updateArrowHint(int ox, int oy) {
        if (arrowGroup != null) { worldGroup.getChildren().remove(arrowGroup); arrowGroup = null; }
        if (chestGroup == null || !chestGroup.isVisible()) return;
        int dir = chestDirection(playerX(), playerY());
        if (dir < 0) return;
        // Show arrow near player
        char[] arrows = {'▲', '▼', '◀', '▶'};
        int[][] offsets = {{0, -2}, {0, 2}, {-2, 0}, {2, 0}};
        var t = new javafx.scene.text.Text(String.valueOf(arrows[dir]));
        t.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        t.setFill(Color.rgb(255, 215, 0, 0.8));
        int sx = COLS / 2 + offsets[dir][0], sy = ROWS / 2 + offsets[dir][1];
        t.setX(sx * TILESIZE + 10); t.setY(sy * TILESIZE + 24);
        arrowGroup = new Group(t);
        worldGroup.getChildren().add(arrowGroup);
    }

    private int chestDirection(int fx, int fy) {
        if (chestGroup == null) return -1;
        int dx = chestX - fx, dy = chestY - fy;
        if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? 3 : 2;
        else return dy > 0 ? 1 : 0;
    }

    private void markExplored() {
        for (int y = 0; y < GameConfig.mapHeight(floor); y++)
            for (int x = 0; x < GameConfig.mapWidth(floor); x++)
                if (visible[y][x]) explored[y][x] = true;
    }

    // --- Enemy AI ---
    private void enemyTurns() {
        var bt = new BehaviorTree();
        var deadIndices = new ArrayList<Integer>();
        for (int i = 0; i < enemies.size(); i++) {
            var e = enemies.get(i);
            if (!e.get(HealthComponent.class).isAlive()) { deadIndices.add(i); continue; }
            var ep = e.get(PositionComponent.class); var ec = e.get(EnemyComponent.class);
            var hp = e.get(HealthComponent.class);
            if (ep == null || ec == null || hp == null) continue;

            double speed = ec.type().moveSpeed();
            int moves = (int) speed + (Math.random() < (speed % 1) ? 1 : 0);
            for (int m = 0; m < moves; m++) {
                if (!e.get(HealthComponent.class).isAlive()) break;
                ep = e.get(PositionComponent.class); hp = e.get(HealthComponent.class);
                if (ep == null || hp == null) break;

                boolean sees = fov.isVisible(playerX(), playerY()) && Math.abs(ep.x() - playerX()) <= 8 && Math.abs(ep.y() - playerY()) <= 8;
                sees = sees && lineOfSight(ep.x(), ep.y(), playerX(), playerY());

                var ai = bt.update(sees, hp.hpRatio(), ep.x(), ep.y(), playerX(), playerY());
                switch (ai) {
                    case ATTACK -> {
                        AttackSystem.meleeAttack(e, player);
                        if (!player.get(HealthComponent.class).isAlive()) { gameOver(); return; }
                    }
                    case CHASE -> moveEnemy(e, ep, playerX(), playerY());
                    case FLEE -> {
                        var f = bt.getFleeTarget(ep.x(), ep.y(), playerX(), playerY());
                        moveEnemy(e, ep, clamp(f[0], 1, GameConfig.mapWidth(floor) - 2), clamp(f[1], 1, GameConfig.mapHeight(floor) - 2));
                    }
                    case WANDER -> {
                        var w = bt.getWanderTarget(ep.x(), ep.y(), 2, 2, GameConfig.mapWidth(floor) - 3, GameConfig.mapHeight(floor) - 3);
                        moveEnemy(e, ep, w[0], w[1]);
                    }
                }
            }
        }
        for (int idx = deadIndices.size() - 1; idx >= 0; idx--) {
            int di = deadIndices.get(idx);
            worldGroup.getChildren().remove(enemyGroups.remove(di));
            enemies.remove(di);
        }
    }

    private void moveEnemy(Entity e, PositionComponent ep, int tx, int ty) {
        var p = pathFinder.findPath(ep.x(), ep.y(), tx, ty);
        if (!p.isEmpty()) {
            var s = p.get(0);
            if (isTileFree(s.x(), s.y())) {
                int idx = enemies.indexOf(e);
                if (idx >= 0 && idx < enemyGroups.size()) {
                    var g = enemyGroups.get(idx);
                    if (g.getChildren().size() > 1) { var c = g.getChildren().remove(0); g.getChildren().add(c); }
                }
                ep.set(s.x(), s.y());
            }
        }
    }

    private boolean lineOfSight(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1, err = dx - dy;
        while (x1 != x2 || y1 != y2) {
            if (dungeon[y1][x1].isBlockingVision()) return false;
            int e2 = 2 * err; if (e2 > -dy) { err -= dy; x1 += sx; } if (e2 < dx) { err += dx; y1 += sy; }
        }
        return true;
    }

    private boolean enemyAt(int x, int y) {
        for (var e : enemies) { var ep = e.get(PositionComponent.class); if (ep != null && ep.x() == x && ep.y() == y) return true; }
        return false;
    }

    private void tryDropWeapon(int x, int y, Entity enemy) {
        var ec = enemy.get(EnemyComponent.class);
        if (ec == null) return;
        double dropRate = ec.type() == EnemyType.GHOST ? 0.6 : 0.4;
        if (Math.random() > dropRate) return;
        WeaponType drop;
        if (ec.type() == EnemyType.BAT) {
            drop = Math.random() < 0.5 ? WeaponType.SICKLE : WeaponType.SWORD;
        } else if (ec.type() == EnemyType.SKELETON) {
            drop = Math.random() < 0.5 ? WeaponType.SWORD : WeaponType.AXE;
        } else {
            // Ghost drops any weapon randomly
            var weapons = new WeaponType[]{WeaponType.SICKLE, WeaponType.SWORD, WeaponType.AXE};
            drop = weapons[(int)(Math.random() * weapons.length)];
        }
        groundWeapons.add(new WeaponDrop(x, y, drop));
        var g = new Group(makeWeaponSprite(drop));
        g.setVisible(false);
        weaponDropGroups.add(g);
        worldGroup.getChildren().add(g);
    }

    private boolean isTileFree(int x, int y) {
        if (y < 0 || y >= dungeon.length || x < 0 || x >= dungeon[0].length) return false;
        if (x == playerX() && y == playerY()) return false;
        for (var e : enemies) { var ep = e.get(PositionComponent.class); if (ep != null && ep.x() == x && ep.y() == y) return false; }
        return dungeon[y][x].isWalkable();
    }

    // --- Save/Load ---
    private void loadGame(String s) {
        try {
            var d = SaveManager.load(s);
            if (d == null) { newGame(); return; }
            floor = d.floor; seed = d.seed;
            turns = d.turns; enemiesSlain = d.enemiesSlain;
            if (d.equippedWeapon != null) {
                try { equippedWeapon = WeaponType.valueOf(d.equippedWeapon); } catch (Exception ex) { equippedWeapon = WeaponType.FISTS; }
                refreshAttackFrame();
            }
            startFloor();
            player.get(PositionComponent.class).set(d.playerX, d.playerY);
            var php = player.get(HealthComponent.class);
            php.takeDamage(php.hp() - d.playerHp);
            // Restore enemies
            if (d.enemies != null) {
                for (int i = 0; i < d.enemies.size() && i < enemies.size(); i++) {
                    var ee = d.enemies.get(i);
                    if (!ee.alive && i < enemies.size()) {
                        enemies.get(i).get(HealthComponent.class).takeDamage(999);
                    }
                }
            }
            // Restore ground weapons
            if (d.groundWeapons != null) {
                for (var we : d.groundWeapons) {
                    try {
                        var wt = WeaponType.valueOf(we.type);
                        groundWeapons.add(new WeaponDrop(we.x, we.y, wt));
                        var g = new Group(makeWeaponSprite(wt));
                        g.setVisible(false);
                        weaponDropGroups.add(g);
                        worldGroup.getChildren().add(g);
                    } catch (Exception ex) {}
                }
            }
            // Restore ghost state
            if (d.ghostRoomX > 0 && d.ghostRoomY > 0) {
                ghostRoom = new Room(d.ghostRoomX, d.ghostRoomY, d.ghostRoomW, d.ghostRoomH);
                if (d.ghostRoomLocked) {
                    // Re-lock the room
                    ghostRoomLocked = false;
                    lockGhostRoom();
                }
            }
            updateHud();
            autoSave();
        } catch (Exception ex) { newGame(); }
    }

    private void autoSave() {
        if (state != GameState.PLAYING || player == null) return;
        try {
            var enemyEntries = new ArrayList<SaveData.EnemyEntry>();
            for (var e : enemies) {
                var ee = new SaveData.EnemyEntry();
                var ec = e.get(EnemyComponent.class);
                var ep = e.get(PositionComponent.class);
                var hp = e.get(HealthComponent.class);
                ee.type = ec != null ? ec.type().name() : "UNKNOWN";
                ee.x = ep != null ? ep.x() : 0;
                ee.y = ep != null ? ep.y() : 0;
                ee.hp = hp != null ? hp.hp() : 0;
                ee.alive = hp != null && hp.isAlive();
                enemyEntries.add(ee);
            }
            var weaponEntries = new ArrayList<SaveData.WeaponEntry>();
            for (var wd : groundWeapons) {
                var we = new SaveData.WeaponEntry();
                we.type = wd.type().name();
                we.x = wd.x(); we.y = wd.y();
                weaponEntries.add(we);
            }
            SaveManager.save(player, floor, seed, turns, enemiesSlain,
                equippedWeapon.name(), enemyEntries, weaponEntries,
                !ghostRoomLocked || enemies.stream().anyMatch(e -> {
                    var ec = e.get(EnemyComponent.class);
                    return ec != null && ec.type() == EnemyType.GHOST && e.get(HealthComponent.class).isAlive();
                }),
                ghostRoom != null ? ghostRoom.x() : 0,
                ghostRoom != null ? ghostRoom.y() : 0,
                ghostRoom != null ? ghostRoom.width() : 0,
                ghostRoom != null ? ghostRoom.height() : 0,
                ghostRoomLocked, "auto");
        } catch (Exception ignored) {}
    }

    private void collectChest() {
        worldGroup.getChildren().remove(chestGroup);
        chestGroup = null;
        holdDir = null;
        if (floor >= GameConfig.MAX_FLOORS) {
            state = GameState.GAME_OVER;
            hud.hide(); worldGroup.setVisible(false);
            if (settingsBtn != null) settingsBtn.setVisible(false);
            try { SaveManager.deleteSlot("auto"); } catch (Exception ignored) {}
            showVictory();
        } else {
            state = GameState.MENU;
            floor++;
            autoSave();
            worldGroup.setVisible(false);
            showFloorTransition();
        }
    }

    private void showFloorTransition() {
        var t = new javafx.scene.text.Text("Floor " + floor + " - Go deeper!");
        t.setFont(Font.font("Monospaced", FontWeight.BOLD, 32));
        t.setFill(Color.GOLD);
        t.setX(GameConfig.SCREEN_WIDTH / 2.0 - 180);
        t.setY(GameConfig.SCREEN_HEIGHT / 2.0);
        FXGL.getGameScene().getRoot().getChildren().add(t);
        var pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            FXGL.getGameScene().getRoot().getChildren().remove(t);
            startFloor();
        });
        pause.play();
    }

    private void showVictory() {
        var box = new javafx.scene.layout.VBox(20);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setLayoutX(GameConfig.SCREEN_WIDTH / 2.0 - 150);
        box.setLayoutY(GameConfig.SCREEN_HEIGHT / 2.0 - 80);
        var title = new javafx.scene.text.Text("YOU WIN!");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 48));
        title.setFill(Color.GOLD);
        var stats = new javafx.scene.text.Text("Treasure found! Floors: " + floor + "  Enemies slain: " + enemiesSlain);
        stats.setFont(Font.font("Monospaced", 16)); stats.setFill(Color.WHITE);
        var btn = new Button("Play Again");
        btn.setFont(Font.font(16));
        btn.setStyle("-fx-background-color: #333; -fx-text-fill: #eee; -fx-font-size: 16;");
        btn.setOnAction(e -> { FXGL.getGameScene().getRoot().getChildren().remove(box); newGame(); });
        box.getChildren().addAll(title, stats, btn);
        if (settingsBtn != null) settingsBtn.setVisible(false);
        FXGL.getGameScene().getRoot().getChildren().add(box);
    }

    private void gameOver() { state = GameState.GAME_OVER; hud.hide(); worldGroup.setVisible(false); if (settingsBtn != null) settingsBtn.setVisible(false); try { SaveManager.deleteSlot("auto"); } catch (Exception ignored) {} GameOverScreen.show(floor, enemiesSlain, turns, this::newGame, this::showMenu); }
    private void checkState() { if (!player.get(HealthComponent.class).isAlive()) gameOver(); }
    private void updateHud() { var hp = player.get(HealthComponent.class); hud.update(hp.hp(), hp.maxHp(), floor, turns, equippedWeapon.label()); }
    private int playerX() { return player.get(PositionComponent.class).x(); }
    private int playerY() { return player.get(PositionComponent.class).y(); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public static void main(String[] args) { launch(args); }
}
