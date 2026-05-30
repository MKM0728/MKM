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
import com.roguelike.ui.GameOverScreen;
import com.roguelike.ui.HudOverlay;
import com.roguelike.ui.InventoryPanel;
import com.roguelike.ui.MenuScreen;

import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public class GameApp extends GameApplication {

    private GameState state = GameState.MENU;
    private Entity player;
    private final List<Entity> enemies = new ArrayList<>();
    private final List<Entity> items = new ArrayList<>();
    private final List<Entity> all = new ArrayList<>();
    private Tile[][] dungeon;
    private DungeonGenerator generator;
    private PathFinder pathFinder;
    private FovSystem fov;
    private HudOverlay hud;
    private InventoryPanel inventory;
    private SystemManager systems;
    private int floor = 1;
    private int turns;
    private int enemiesSlain;
    private long seed;
    private boolean[][] visible;
    private boolean[][] explored;
    private boolean playerActed;

    private Group worldGroup;
    private Rectangle[][] tileRects;
    private Rectangle playerRect;
    private final List<Rectangle> enemyRects = new ArrayList<>();
    private static final int TILESIZE = 32;
    private static final int COLS = GameConfig.SCREEN_WIDTH / TILESIZE;
    private static final int ROWS = GameConfig.SCREEN_HEIGHT / TILESIZE;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle(GameConfig.GAME_TITLE);
        settings.setWidth(GameConfig.SCREEN_WIDTH);
        settings.setHeight(GameConfig.SCREEN_HEIGHT);
        settings.setVersion("0.1.0");
    }

    @Override
    protected void initInput() {
        // AWT-level keyboard hook - bypasses JavaFX IME entirely
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(e -> {
                if (e.getID() != java.awt.event.KeyEvent.KEY_PRESSED) return false;
                if (state != GameState.PLAYING) return false;
                switch (e.getKeyCode()) {
                    case java.awt.event.KeyEvent.VK_W, java.awt.event.KeyEvent.VK_UP:
                        tryMove(0, -1); return true;
                    case java.awt.event.KeyEvent.VK_S, java.awt.event.KeyEvent.VK_DOWN:
                        tryMove(0, 1); return true;
                    case java.awt.event.KeyEvent.VK_A, java.awt.event.KeyEvent.VK_LEFT:
                        tryMove(-1, 0); return true;
                    case java.awt.event.KeyEvent.VK_D, java.awt.event.KeyEvent.VK_RIGHT:
                        tryMove(1, 0); return true;
                    case java.awt.event.KeyEvent.VK_I:
                        toggleInventory(); return true;
                    case java.awt.event.KeyEvent.VK_ESCAPE:
                        togglePause(); return true;
                }
                return false;
            });
    }

    @Override
    protected void initGame() {
        FXGL.getGameScene().setBackgroundColor(Color.rgb(20, 20, 30));
        systems = new SystemManager();
        fov = new FovSystem(GameConfig.FOV_RADIUS);
        inventory = new InventoryPanel();
        inventory.setOnUse(this::useItem);
        inventory.setOnDrop(this::dropItem);
        hud = new HudOverlay();

        // Create world rendering group
        worldGroup = new Group();
        tileRects = new Rectangle[ROWS][COLS];
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                var r = new Rectangle(TILESIZE - 1, TILESIZE - 1);
                r.setX(x * TILESIZE);
                r.setY(y * TILESIZE);
                r.setVisible(false);
                tileRects[y][x] = r;
                worldGroup.getChildren().add(r);
            }
        }
        playerRect = new Rectangle(TILESIZE - 3, TILESIZE - 3, Color.LIMEGREEN);
        playerRect.setX((COLS / 2) * TILESIZE + 1);
        playerRect.setY((ROWS / 2) * TILESIZE + 1);
        playerRect.setVisible(false);

        worldGroup.getChildren().add(playerRect);
        worldGroup.setVisible(false);
        FXGL.getGameScene().getRoot().getChildren().add(worldGroup);
        playerRect.toFront();

        // Mouse click to move (click adjacent tile)
        worldGroup.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (state != GameState.PLAYING) return;
            int tx = (int)(e.getX() / TILESIZE), ty = (int)(e.getY() / TILESIZE);
            int dx = tx - COLS/2, dy = ty - ROWS/2;
            if (Math.abs(dx) + Math.abs(dy) == 1) tryMove(dx, dy);
        });

        // On-screen D-pad (bypasses keyboard/IME entirely)
        var dpad = new HBox(8);
        dpad.setAlignment(javafx.geometry.Pos.CENTER);
        dpad.setTranslateX(80);
        dpad.setTranslateY(GameConfig.SCREEN_HEIGHT - 50);
        for (var kv : new String[][]{{"▲","U"},{"▼","D"},{"◀","L"},{"▶","R"}}) {
            var btn = new Button(kv[0]);
            btn.setFont(Font.font(16));
            btn.setStyle("-fx-background-color: #333; -fx-text-fill: #aaa; -fx-border-color: #555; -fx-min-width: 55; -fx-min-height: 40;");
            String dir = kv[1];
            btn.setOnAction(e -> {
                switch (dir) {
                    case "U" -> tryMove(0, -1);
                    case "D" -> tryMove(0, 1);
                    case "L" -> tryMove(-1, 0);
                    case "R" -> tryMove(1, 0);
                }
            });
            dpad.getChildren().add(btn);
        }
        FXGL.getGameScene().addUINode(dpad);

        // Setup keyboard via scene AFTER scene is ready
        javafx.application.Platform.runLater(() -> {
            var scene = FXGL.getGameScene().getRoot().getScene();
            if (scene != null) {
                scene.setOnKeyPressed(e -> {
                    if (state != GameState.PLAYING) return;
                    switch (e.getCode()) {
                        case W, UP:    tryMove(0, -1); e.consume(); break;
                        case S, DOWN:  tryMove(0, 1);  e.consume(); break;
                        case A, LEFT:  tryMove(-1, 0); e.consume(); break;
                        case D, RIGHT: tryMove(1, 0);  e.consume(); break;
                        case I:        toggleInventory(); e.consume(); break;
                        case ESCAPE:   togglePause(); e.consume(); break;
                    }
                });
            }
        });

        showMenu();
    }

    @Override
    protected void onUpdate(double tpf) {
        if (state != GameState.PLAYING) return;
        if (playerActed) {
            enemyTurns();
            playerActed = false;
            renderAll();
            updateHud();
            checkState();
        }
    }

    private void handleKey(KeyCode code) {
        if (state != GameState.PLAYING) return;
        switch (code) {
            case W, UP -> tryMove(0, -1);
            case S, DOWN -> tryMove(0, 1);
            case A, LEFT -> tryMove(-1, 0);
            case D, RIGHT -> tryMove(1, 0);
            case I -> toggleInventory();
            case ESCAPE -> togglePause();
        }
    }


    // --- Game flow ---

    private void showMenu() {
        state = GameState.MENU;
        GameOverScreen.hide();
        hud.remove();
        worldGroup.setVisible(false);
        MenuScreen.show(this::newGame, () -> loadGame("auto"), () -> FXGL.getGameController().exit());
    }

    private void newGame() {
        MenuScreen.hide();
        GameOverScreen.hide();
        seed = java.lang.System.currentTimeMillis();
        floor = 1;
        turns = 0;
        enemiesSlain = 0;
        startFloor();
    }

    private void startFloor() {
        enemies.clear();
        items.clear();
        all.clear();

        // Remove old enemy rects
        for (var r : enemyRects) worldGroup.getChildren().remove(r);
        enemyRects.clear();

        generator = new DungeonGenerator(GameConfig.MAP_WIDTH, GameConfig.MAP_HEIGHT, seed + floor);
        dungeon = generator.generate(GameConfig.MAX_BSP_DEPTH);
        pathFinder = new PathFinder(dungeon);

        explored = new boolean[GameConfig.MAP_HEIGHT][GameConfig.MAP_WIDTH];

        player = PlayerFactory.create(generator.stairsUpX(), generator.stairsUpY());
        all.add(player);

        spawnEnemies();
        spawnItems();

        // Create enemy rects
        for (int i = 0; i < enemies.size(); i++) {
            var r = new Rectangle(TILESIZE - 5, TILESIZE - 5, Color.RED);
            r.setVisible(false);
            enemyRects.add(r);
            worldGroup.getChildren().add(r);
        }

        visible = fov.compute(dungeon, playerX(), playerY());
        markExplored();
        worldGroup.setVisible(true);
        playerRect.toFront();

        state = GameState.PLAYING;
        playerActed = false;
        renderAll();
        updateHud();
    }

    private void renderAll() {
        int offsetX = playerX() - COLS / 2;
        int offsetY = playerY() - ROWS / 2;

        for (int sy = 0; sy < ROWS; sy++) {
            for (int sx = 0; sx < COLS; sx++) {
                int mx = offsetX + sx;
                int my = offsetY + sy;
                var r = tileRects[sy][sx];

                if (mx < 0 || mx >= GameConfig.MAP_WIDTH || my < 0 || my >= GameConfig.MAP_HEIGHT) {
                    r.setVisible(false);
                    continue;
                }

                if (visible[my][mx]) {
                    r.setVisible(true);
                    r.setFill(switch (dungeon[my][mx]) {
                        case WALL -> Color.rgb(60, 60, 65);
                        case FLOOR -> Color.rgb(50, 45, 38);
                        case CORRIDOR -> Color.rgb(42, 37, 32);
                        case DOOR -> Color.rgb(100, 70, 30);
                        case STAIRS_DOWN, STAIRS_UP -> Color.rgb(180, 160, 30);
                        case WATER -> Color.rgb(20, 60, 100);
                        default -> Color.BLACK;
                    });
                } else if (explored[my][mx]) {
                    r.setVisible(true);
                    r.setFill(Color.rgb(30, 27, 25));
                } else {
                    r.setVisible(false);
                }
            }
        }

        // Player always at center
        playerRect.setVisible(true);

        // Enemies
        for (int i = 0; i < enemyRects.size(); i++) {
            if (i >= enemies.size()) {
                enemyRects.get(i).setVisible(false);
                continue;
            }
            var e = enemies.get(i);
            var ep = e.get(PositionComponent.class);
            if (ep == null || !visible[ep.y()][ep.x()]) {
                enemyRects.get(i).setVisible(false);
                continue;
            }
            int sx = ep.x() - offsetX;
            int sy = ep.y() - offsetY;
            if (sx < 0 || sx >= COLS || sy < 0 || sy >= ROWS) {
                enemyRects.get(i).setVisible(false);
                continue;
            }
            enemyRects.get(i).setX(sx * TILESIZE + 1);
            enemyRects.get(i).setY(sy * TILESIZE + 1);
            enemyRects.get(i).setVisible(true);
        }
    }

    private void markExplored() {
        for (int y = 0; y < GameConfig.MAP_HEIGHT; y++) {
            for (int x = 0; x < GameConfig.MAP_WIDTH; x++) {
                if (visible[y][x]) explored[y][x] = true;
            }
        }
    }

    private void spawnEnemies() {
        int count = 3 + floor * 2;
        var types = EnemyType.values();
        for (int i = 0; i < count; i++) {
            int x, y;
            do {
                x = 2 + (int) (Math.random() * (GameConfig.MAP_WIDTH - 4));
                y = 2 + (int) (Math.random() * (GameConfig.MAP_HEIGHT - 4));
            } while (dungeon[y][x] != Tile.FLOOR || (x == playerX() && y == playerY()));

            var enemy = EnemyFactory.create(types[(int) (Math.random() * types.length)], x, y);
            enemies.add(enemy);
            all.add(enemy);
        }
    }

    private void spawnItems() {
        int count = 2 + (int) (Math.random() * 3);
        var rng = new java.util.Random();
        for (int i = 0; i < count; i++) {
            int x, y;
            do {
                x = 2 + (int) (Math.random() * (GameConfig.MAP_WIDTH - 4));
                y = 2 + (int) (Math.random() * (GameConfig.MAP_HEIGHT - 4));
            } while (dungeon[y][x] != Tile.FLOOR);

            var item = switch (rng.nextInt(3)) {
                case 0 -> ItemFactory.createWeapon(x, y, rng);
                case 1 -> ItemFactory.createPotion(x, y, rng);
                default -> ItemFactory.createScroll(x, y, rng);
            };
            items.add(item);
            all.add(item);
        }
    }

    // --- Player actions ---

    private void tryMove(int dx, int dy) {
        if (state != GameState.PLAYING || playerActed) return;
        int nx = playerX() + dx, ny = playerY() + dy;
        if (ny < 0 || ny >= dungeon.length || nx < 0 || nx >= dungeon[0].length) return;

        var tile = dungeon[ny][nx];

        if (tile == Tile.STAIRS_DOWN) {
            floor++;
            startFloor();
            return;
        }

        if (!tile.isWalkable()) return;

        for (var enemy : enemies) {
            var ep = enemy.get(PositionComponent.class);
            if (ep != null && ep.x() == nx && ep.y() == ny) {
                AttackSystem.meleeAttack(player, enemy);
                if (!enemy.get(HealthComponent.class).isAlive()) {
                    enemiesSlain++;
                }
                playerActed = true;
                turns++;
                visible = fov.compute(dungeon, playerX(), playerY());
                markExplored();
                return;
            }
        }

        for (var item : items) {
            var ip = item.get(PositionComponent.class);
            if (ip != null && ip.x() == nx && ip.y() == ny) {
                pickupItem(item);
            }
        }

        player.get(PositionComponent.class).set(nx, ny);
        visible = fov.compute(dungeon, nx, ny);
        markExplored();
        playerActed = true;
        turns++;
    }

    private void pickupItem(Entity item) {
        items.remove(item);
        all.remove(item);
    }

    private List<Entity> inventoryItems() {
        return new ArrayList<>();
    }

    private void updateInventoryStorage(List<Entity> inv) {}

    private void useItem(Entity item) {
        var ic = item.get(ItemComponent.class);
        if (ic == null) return;
        var hp = player.get(HealthComponent.class);
        if (hp != null && ic.itemType() == ItemType.POTION) {
            hp.heal(ic.value());
        }
        playerActed = true;
        turns++;
    }

    private void dropItem(Entity item) {
        var pp = player.get(PositionComponent.class);
        item.get(PositionComponent.class).set(pp.x(), pp.y());
        items.add(item);
        all.add(item);
        playerActed = true;
        turns++;
    }

    private void toggleInventory() {
        if (inventory.isVisible()) inventory.hide();
        else inventory.show(inventoryItems());
    }

    private void togglePause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED) state = GameState.PLAYING;
    }

    // --- Enemy AI ---

    private void enemyTurns() {
        var bt = new BehaviorTree();
        for (var enemy : new ArrayList<>(enemies)) {
            if (!enemy.get(HealthComponent.class).isAlive()) {
                enemies.remove(enemy);
                all.remove(enemy);
                continue;
            }
            var ep = enemy.get(PositionComponent.class);
            var ec = enemy.get(EnemyComponent.class);
            var hp = enemy.get(HealthComponent.class);
            if (ep == null || ec == null || hp == null) continue;

            boolean seesPlayer = fov.isVisible(playerX(), playerY()) &&
                Math.abs(ep.x() - playerX()) <= 8 && Math.abs(ep.y() - playerY()) <= 8;
            seesPlayer = seesPlayer && lineOfSight(ep.x(), ep.y(), playerX(), playerY());

            var aiState = bt.update(seesPlayer, hp.hpRatio(), ep.x(), ep.y(), playerX(), playerY());

            switch (aiState) {
                case ATTACK -> {
                    AttackSystem.meleeAttack(enemy, player);
                    if (!player.get(HealthComponent.class).isAlive()) {
                        gameOver();
                        return;
                    }
                }
                case CHASE -> moveEnemy(enemy, ep, playerX(), playerY());
                case FLEE -> {
                    var flee = bt.getFleeTarget(ep.x(), ep.y(), playerX(), playerY());
                    moveEnemy(enemy, ep,
                        clamp(flee[0], 1, GameConfig.MAP_WIDTH - 2),
                        clamp(flee[1], 1, GameConfig.MAP_HEIGHT - 2));
                }
                case WANDER -> {
                    var wander = bt.getWanderTarget(ep.x(), ep.y(), 2, 2,
                        GameConfig.MAP_WIDTH - 3, GameConfig.MAP_HEIGHT - 3);
                    moveEnemy(enemy, ep, wander[0], wander[1]);
                }
            }
        }
    }

    private void moveEnemy(Entity enemy, PositionComponent ep, int tx, int ty) {
        var path = pathFinder.findPath(ep.x(), ep.y(), tx, ty);
        if (!path.isEmpty()) {
            var step = path.get(0);
            if (isTileFree(step.x(), step.y())) {
                ep.set(step.x(), step.y());
            }
        }
    }

    private boolean lineOfSight(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (x1 != x2 || y1 != y2) {
            if (dungeon[y1][x1].isBlockingVision()) return false;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
        return true;
    }

    private boolean isTileFree(int x, int y) {
        if (y < 0 || y >= dungeon.length || x < 0 || x >= dungeon[0].length) return false;
        if (x == playerX() && y == playerY()) return false;
        for (var e : enemies) {
            var ep = e.get(PositionComponent.class);
            if (ep != null && ep.x() == x && ep.y() == y) return false;
        }
        return dungeon[y][x].isWalkable();
    }

    // --- Save/Load ---

    private void saveGame(String slot) {
        try {
            SaveManager.save(player, floor, seed, slot);
        } catch (Exception e) {
            java.lang.System.err.println("Save failed: " + e.getMessage());
        }
    }

    private void loadGame(String slot) {
        try {
            var data = SaveManager.load(slot);
            if (data == null) { newGame(); return; }
            floor = data.floor;
            seed = data.seed;
            startFloor();
            player.get(PositionComponent.class).set(data.playerX, data.playerY);
            player.get(HealthComponent.class).takeDamage(
                player.get(HealthComponent.class).hp() - data.playerHp);
        } catch (Exception e) {
            newGame();
        }
    }

    private void gameOver() {
        state = GameState.GAME_OVER;
        hud.remove();
        worldGroup.setVisible(false);
        GameOverScreen.show(floor, enemiesSlain, turns, this::newGame, this::showMenu);
    }

    private void checkState() {
        if (!player.get(HealthComponent.class).isAlive()) {
            gameOver();
        }
    }

    private void updateHud() {
        var hp = player.get(HealthComponent.class);
        hud.update(hp.hp(), hp.maxHp(), floor, turns);
    }

    private int playerX() { return player.get(PositionComponent.class).x(); }
    private int playerY() { return player.get(PositionComponent.class).y(); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public static void main(String[] args) {
        java.lang.System.setProperty("com.sun.javafx.ime", "disabled");
        java.lang.System.setProperty("javafx.ime", "disabled");
        launch(args);
    }
}
