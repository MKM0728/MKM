package com.roguelike.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.input.UserAction;
import com.roguelike.ai.BehaviorTree;
import com.roguelike.ai.FovSystem;
import com.roguelike.ai.PathFinder;
import com.roguelike.combat.AttackSystem;
import com.roguelike.combat.DamageType;
import com.roguelike.entity.*;
import com.roguelike.map.DungeonGenerator;
import com.roguelike.map.Tile;
import com.roguelike.save.SaveManager;
import com.roguelike.ui.GameOverScreen;
import com.roguelike.ui.HudOverlay;
import com.roguelike.ui.InventoryPanel;
import com.roguelike.ui.MenuScreen;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

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
    private boolean playerActed;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle(GameConfig.GAME_TITLE);
        settings.setWidth(GameConfig.SCREEN_WIDTH);
        settings.setHeight(GameConfig.SCREEN_HEIGHT);
        settings.setVersion("0.1.0");
    }

    @Override
    protected void initInput() {
        FXGL.onKeyDown(KeyCode.W, "move-up", () -> tryMove(0, -1));
        FXGL.onKeyDown(KeyCode.S, "move-down", () -> tryMove(0, 1));
        FXGL.onKeyDown(KeyCode.A, "move-left", () -> tryMove(-1, 0));
        FXGL.onKeyDown(KeyCode.D, "move-right", () -> tryMove(1, 0));
        FXGL.onKeyDown(KeyCode.I, "inventory", this::toggleInventory);
        FXGL.onKeyDown(KeyCode.ESCAPE, "pause", this::togglePause);
    }

    @Override
    protected void initGame() {
        FXGL.getGameScene().setBackgroundColor(Color.BLACK);
        systems = new SystemManager();
        fov = new FovSystem(GameConfig.FOV_RADIUS);
        inventory = new InventoryPanel();
        inventory.setOnUse(this::useItem);
        inventory.setOnDrop(this::dropItem);
        hud = new HudOverlay();
        showMenu();
    }

    @Override
    protected void onUpdate(double tpf) {
        if (state != GameState.PLAYING) return;
        if (playerActed) {
            enemyTurns();
            playerActed = false;
            updateHud();
            checkState();
        }
    }

    // --- Game flow ---

    private void showMenu() {
        state = GameState.MENU;
        MenuScreen.show(this::newGame, () -> loadGame("auto"), () -> FXGL.getGameController().exit());
    }

    private void newGame() {
        seed = System.currentTimeMillis();
        floor = 1;
        turns = 0;
        enemiesSlain = 0;
        startFloor();
    }

    private void startFloor() {
        enemies.clear();
        items.clear();
        all.clear();

        generator = new DungeonGenerator(GameConfig.MAP_WIDTH, GameConfig.MAP_HEIGHT, seed + floor);
        dungeon = generator.generate(GameConfig.MAX_BSP_DEPTH);
        pathFinder = new PathFinder(dungeon);

        player = PlayerFactory.create(generator.stairsUpX(), generator.stairsUpY());
        all.add(player);

        spawnEnemies();
        spawnItems();

        visible = fov.compute(dungeon, playerX(), playerY());
        state = GameState.PLAYING;
        playerActed = false;
        updateHud();
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
                var evt = AttackSystem.meleeAttack(player, enemy);
                if (!enemy.get(HealthComponent.class).isAlive()) {
                    enemiesSlain++;
                }
                playerActed = true;
                turns++;
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
        playerActed = true;
        turns++;
    }

    private void pickupItem(Entity item) {
        items.remove(item);
        all.remove(item);
        // Add to inventory - store in a simple list on the player
        var inv = new ArrayList<>(inventoryItems());
        inv.add(item);
        // Update inventory data
        updateInventoryStorage(inv);
    }

    private List<Entity> inventoryItems() {
        // In a full implementation this would be stored on the player entity
        return new ArrayList<>();
    }

    private void updateInventoryStorage(List<Entity> inv) {
        // Stub - would use an InventoryComponent in full implementation
    }

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
        if (inventory.isVisible()) {
            inventory.hide();
        } else {
            inventory.show(inventoryItems());
        }
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

            var state = bt.update(seesPlayer, hp.hpRatio(), ep.x(), ep.y(), playerX(), playerY());

            switch (state) {
                case ATTACK -> {
                    AttackSystem.meleeAttack(enemy, player);
                    if (!player.get(HealthComponent.class).isAlive()) {
                        gameOver();
                        return;
                    }
                }
                case CHASE -> {
                    var path = pathFinder.findPath(ep.x(), ep.y(), playerX(), playerY());
                    if (!path.isEmpty()) {
                        var step = path.get(0);
                        if (isTileFree(step.x(), step.y())) {
                            ep.set(step.x(), step.y());
                        }
                    }
                }
                case FLEE -> {
                    var flee = bt.getFleeTarget(ep.x(), ep.y(), playerX(), playerY());
                    var path = pathFinder.findPath(ep.x(), ep.y(),
                        clamp(flee[0], 1, GameConfig.MAP_WIDTH - 2),
                        clamp(flee[1], 1, GameConfig.MAP_HEIGHT - 2));
                    if (!path.isEmpty()) {
                        var step = path.get(0);
                        if (isTileFree(step.x(), step.y())) {
                            ep.set(step.x(), step.y());
                        }
                    }
                }
                case WANDER -> {
                    var wander = bt.getWanderTarget(ep.x(), ep.y(), 2, 2,
                        GameConfig.MAP_WIDTH - 3, GameConfig.MAP_HEIGHT - 3);
                    var path = pathFinder.findPath(ep.x(), ep.y(), wander[0], wander[1]);
                    if (!path.isEmpty()) {
                        var step = path.get(0);
                        if (isTileFree(step.x(), step.y())) {
                            ep.set(step.x(), step.y());
                        }
                    }
                }
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
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    private void loadGame(String slot) {
        try {
            var data = SaveManager.load(slot);
            if (data == null) {
                newGame();
                return;
            }
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
        GameOverScreen.show(floor, enemiesSlain, turns, this::newGame, this::showMenu);
    }

    private void checkState() {
        if (enemies.isEmpty()) {
            // floor cleared - player can proceed to stairs
        }
        if (!player.get(HealthComponent.class).isAlive()) {
            gameOver();
        }
    }

    private void updateHud() {
        var hp = player.get(HealthComponent.class);
        hud.update(hp.hp(), hp.maxHp(), floor, turns);
    }

    // --- Helpers ---

    private int playerX() { return player.get(PositionComponent.class).x(); }
    private int playerY() { return player.get(PositionComponent.class).y(); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public static void main(String[] args) {
        launch(args);
    }
}
