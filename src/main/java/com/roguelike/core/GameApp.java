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

import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

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
    private int playerMoves;
    private boolean animating;
    private int[] holdDir;
    private static final int PLAYER_SPEED = 2;
    private static final int BASE_ANIM_MS = 180;

    private Group worldGroup;
    private Rectangle[][] tileRects;
    private Rectangle playerHpBar;
    private Group playerGroup;
    private javafx.scene.layout.HBox dpad;
    private Canvas[] playerFrames;
    private int playerFrameIdx;
    private final List<Group> enemyRects = new ArrayList<>();
    private static final int TILESIZE = 32;
    private int COLS = GameConfig.SCREEN_WIDTH / TILESIZE;
    private int ROWS = GameConfig.SCREEN_HEIGHT / TILESIZE;
    private Canvas treasureChest;

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
        playerGroup = new Group();

        // Create animated pixel character (3 frames: stand, walk1, walk2)
        int s = TILESIZE;
        playerFrames = new Canvas[]{new Canvas(s, s), new Canvas(s, s), new Canvas(s, s), new Canvas(s, s)};
        drawPixels(playerFrames[0], false, false, false); // stand
        drawPixels(playerFrames[1], true, false, false);  // walk L
        drawPixels(playerFrames[2], false, true, false);  // walk R
        drawPixels(playerFrames[3], false, false, true);  // attack
        playerFrameIdx = 0;

        playerGroup.getChildren().add(playerFrames[0]);
        playerHpBar = new Rectangle(s - 3, 4, Color.LIMEGREEN);
        playerHpBar.setY(-8);
        playerGroup.getChildren().add(playerHpBar);
        playerGroup.setVisible(false);
        setPlayerGroupPos(COLS / 2, ROWS / 2);

        worldGroup.getChildren().add(playerGroup);
        worldGroup.setVisible(false);
        FXGL.getGameScene().getRoot().getChildren().add(worldGroup);

        // Mouse click to move (click adjacent tile)
        worldGroup.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (state != GameState.PLAYING) return;
            int tx = (int)(e.getX() / TILESIZE), ty = (int)(e.getY() / TILESIZE);
            int dx = tx - COLS/2, dy = ty - ROWS/2;
            if (Math.abs(dx) + Math.abs(dy) == 1) tryMove(dx, dy);
        });

        // Hold-to-move D-pad
        dpad = new HBox(8);
        dpad.setAlignment(javafx.geometry.Pos.CENTER);
        dpad.setTranslateX(80);
        dpad.setTranslateY(GameConfig.SCREEN_HEIGHT - 50);
        for (var kv : new String[][]{{"▲","U"},{"▼","D"},{"◀","L"},{"▶","R"}}) {
            var btn = new Button(kv[0]);
            btn.setFont(Font.font(24));
            btn.setStyle("-fx-background-color: rgba(20,20,20,0.9); -fx-text-fill: #0f0; -fx-border-color: #0f0; -fx-border-width: 2; -fx-min-width: 70; -fx-min-height: 55; -fx-background-radius: 10; -fx-border-radius: 10; -fx-cursor: hand;");
            String dir = kv[1];
            int dx = 0, dy = 0;
            switch (dir) { case "U"->{ dy=-1; } case "D"->{ dy=1; } case "L"->{ dx=-1; } case "R"->{ dx=1; } }
            int fx = dx, fy = dy;
            btn.setOnMousePressed(e -> startHolding(fx, fy));
            btn.setOnMouseReleased(e -> stopHolding());
            btn.setOnMouseExited(e -> stopHolding());
            dpad.getChildren().add(btn);
        }
        dpad.setViewOrder(-9999);
        dpad.setPickOnBounds(true);
        dpad.setMouseTransparent(false);
        FXGL.getGameScene().getRoot().getChildren().add(dpad);
        dpad.toFront();

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

    private long lastMoveTime;

    @Override
    protected void onUpdate(double tpf) {
        if (dpad != null) dpad.toFront();
        if (state != GameState.PLAYING || animating) return;

        // Hold-to-move: keep moving while button is pressed
        if (holdDir != null && playerMoves > 0) {
            long now = java.lang.System.currentTimeMillis();
            if (now - lastMoveTime > 120) {
                tryMove(holdDir[0], holdDir[1]);
                lastMoveTime = now;
            }
        }

        if (playerMoves <= 0) {
            enemyTurns();
            playerMoves = PLAYER_SPEED;
            renderAll(); updateHud(); checkState();
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
        enemies.clear(); items.clear(); all.clear();
        for (var r : enemyRects) worldGroup.getChildren().remove(r);
        enemyRects.clear();
        if (treasureChest != null) worldGroup.getChildren().remove(treasureChest);

        int mw = GameConfig.mapWidth(floor), mh = GameConfig.mapHeight(floor);
        generator = new DungeonGenerator(mw, mh, seed + floor);
        dungeon = generator.generate(GameConfig.MAX_BSP_DEPTH + floor / 2);
        pathFinder = new PathFinder(dungeon);
        explored = new boolean[mh][mw];

        player = PlayerFactory.create(generator.stairsUpX(), generator.stairsUpY());
        all.add(player);

        spawnEnemies();
        spawnItems();
        spawnTreasure();

        for (var e : enemies) {
            var ec = e.get(EnemyComponent.class);
            String kind = ec != null && ec.type() != null ? ec.type().kind() : "slime";
            var c0 = new Canvas(TILESIZE, TILESIZE); var c1 = new Canvas(TILESIZE, TILESIZE);
            drawEnemy(c0, kind, false); drawEnemy(c1, kind, true);
            var g = new Group(c0); g.setVisible(false);
            enemyRects.add(g); worldGroup.getChildren().add(g);
        }

        COLS = GameConfig.SCREEN_WIDTH / TILESIZE;
        ROWS = GameConfig.SCREEN_HEIGHT / TILESIZE;
        rebuildTiles(mw, mh);

        visible = fov.compute(dungeon, playerX(), playerY()); markExplored();
        worldGroup.setVisible(true); playerGroup.toFront();
        state = GameState.PLAYING; playerMoves = PLAYER_SPEED;
        renderAll(); updateHud();
    }

    private void rebuildTiles(int mw, int mh) {
        // Remove only tile rectangles
        if (tileRects != null) {
            for (var row : tileRects) for (var r : row) worldGroup.getChildren().remove(r);
        }
        tileRects = new Rectangle[ROWS][ROWS > 0 ? COLS : 0];
        for (int y = 0; y < ROWS; y++)
            for (int x = 0; x < COLS; x++) {
                var r = new Rectangle(TILESIZE - 1, TILESIZE - 1);
                r.setX(x * TILESIZE); r.setY(y * TILESIZE); r.setVisible(false);
                tileRects[y][x] = r;
                worldGroup.getChildren().add(r);
                r.toBack();
            }
        if (playerGroup != null) playerGroup.toFront();
        if (dpad != null) dpad.toFront();
    }

    private void spawnTreasure() {
        int mw = GameConfig.mapWidth(floor), mh = GameConfig.mapHeight(floor);
        int tx, ty;
        do { tx = 2 + (int)(Math.random() * (mw - 4)); ty = 2 + (int)(Math.random() * (mh - 4));
        } while (dungeon[ty][tx] != Tile.FLOOR || (tx == playerX() && ty == playerY()));

        treasureChest = new Canvas(TILESIZE, TILESIZE);
        GraphicsContext g = treasureChest.getGraphicsContext2D();
        int p = 3;
        g.setFill(Color.rgb(30,35,30)); g.fillRect(0,0,TILESIZE,TILESIZE);
        g.setFill(Color.rgb(139, 100, 30)); g.fillRect(p*2,p*3,p*7,p*6); // chest body
        g.setFill(Color.rgb(200, 170, 50)); g.fillRect(p*3,p*2,p*5,p*2); // lid
        g.setFill(Color.rgb(255, 215, 0)); g.fillRect(p*4,p*4,p*1,p*1); g.fillRect(p*6,p*4,p*1,p*1); // gold glint
        treasureChest.setTranslateX(tx * TILESIZE);
        treasureChest.setTranslateY(ty * TILESIZE);
        worldGroup.getChildren().add(treasureChest);
        treasureChest.toFront();
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
        playerGroup.setVisible(true);
        var pHp = player.get(HealthComponent.class);
        playerHpBar.setWidth((TILESIZE - 3) * Math.max(0.1, (double)pHp.hp() / pHp.maxHp()));
        playerHpBar.setFill(pHp.hpRatio() < 0.3 ? Color.RED : Color.LIMEGREEN);
        setPlayerGroupPos(COLS / 2, ROWS / 2);

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
            enemyRects.get(i).setTranslateX(sx * TILESIZE + 1);
            enemyRects.get(i).setTranslateY(sy * TILESIZE + 1);
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
        int mw = GameConfig.mapWidth(floor), mh = GameConfig.mapHeight(floor);
        int batCount = GameConfig.batCount(floor), skelCount = GameConfig.skeletonCount(floor);
        for (int i = 0; i < batCount; i++) {
            int x, y;
            do { x = 2 + (int)(Math.random() * (mw - 4)); y = 2 + (int)(Math.random() * (mh - 4));
            } while (dungeon[y][x] != Tile.FLOOR || (x == playerX() && y == playerY()));
            enemies.add(EnemyFactory.create(EnemyType.BAT, x, y));
        }
        for (int i = 0; i < skelCount; i++) {
            int x, y;
            do { x = 2 + (int)(Math.random() * (mw - 4)); y = 2 + (int)(Math.random() * (mh - 4));
            } while (dungeon[y][x] != Tile.FLOOR || (x == playerX() && y == playerY()));
            enemies.add(EnemyFactory.create(EnemyType.SKELETON, x, y));
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
        if (state != GameState.PLAYING || playerMoves <= 0 || animating) return;
        int nx = playerX() + dx, ny = playerY() + dy;
        if (ny < 0 || ny >= dungeon.length || nx < 0 || nx >= dungeon[0].length) return;

        var tile = dungeon[ny][nx];

        // Check for treasure
        if (treasureChest != null && treasureChest.isVisible() &&
            Math.abs(treasureChest.getTranslateX()/TILESIZE - nx) < 1 &&
            Math.abs(treasureChest.getTranslateY()/TILESIZE - ny) < 1) {
            victoryFloor(); return;
        }
        if (tile == Tile.STAIRS_DOWN) { floor++; startFloor(); return; }
        if (!tile.isWalkable()) return;

        for (var enemy : enemies) {
            var ep = enemy.get(PositionComponent.class);
            if (ep != null && ep.x() == nx && ep.y() == ny) {
                // Show attack animation + flash
                setPlayerFrame(3);
                playerGroup.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.8, 0, 0);");
                // One-hit kill
                var eHp = enemy.get(HealthComponent.class);
                if (eHp != null) eHp.takeDamage(999);
                enemiesSlain++;
                showCombatText(nx, ny, "DEAD!");
                playerMoves--; turns++;
                // Reset to stand after longer delay
                var t = new Thread(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        playerGroup.setStyle(null);
                        if (holdDir == null && !animating) setPlayerFrame(0);
                    });
                });
                t.setDaemon(true); t.start();
                visible = fov.compute(dungeon, playerX(), playerY()); markExplored();
                return;
            }
        }

        for (var item : items) {
            var ip = item.get(PositionComponent.class);
            if (ip != null && ip.x() == nx && ip.y() == ny) { pickupItem(item); }
        }

        // Toggle walk frames
        setPlayerFrame(playerFrameIdx == 1 ? 2 : 1);
        animateMove(player, nx, ny, PLAYER_SPEED, () -> {
            playerMoves--; turns++;
            visible = fov.compute(dungeon, nx, ny); markExplored();
            if (holdDir == null) setPlayerFrame(0);
        });
    }

    private void animateMove(Entity entity, int tx, int ty, double speed, Runnable onDone) {
        animating = true;
        var ep = entity.get(PositionComponent.class);
        int fx = ep.x(), fy = ep.y();
        long duration = (long)(BASE_ANIM_MS / speed);
        var anim = new TranslateTransition(Duration.millis(duration), playerGroup);
        anim.setFromX(0); anim.setFromY(0);
        anim.setToX((tx - fx) * TILESIZE);
        anim.setToY((ty - fy) * TILESIZE);
        anim.setOnFinished(e -> {
            playerGroup.setTranslateX(0); playerGroup.setTranslateY(0);
            ep.set(tx, ty);
            renderAll();
            animating = false;
            if (onDone != null) onDone.run();
        });
        anim.play();
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
        playerMoves--; turns++;
    }

    private void dropItem(Entity item) {
        var pp = player.get(PositionComponent.class);
        item.get(PositionComponent.class).set(pp.x(), pp.y());
        items.add(item); all.add(item);
        playerMoves--; turns++;
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
                enemies.remove(enemy); all.remove(enemy); continue;
            }
            var ep = enemy.get(PositionComponent.class);
            var ec = enemy.get(EnemyComponent.class);
            var hp = enemy.get(HealthComponent.class);
            if (ep == null || ec == null || hp == null) continue;

            double speed = ec.type() != null ? ec.type().moveSpeed() : 1.0;
            int moves = (int) speed + (Math.random() < (speed % 1) ? 1 : 0);

            for (int m = 0; m < moves; m++) {
                if (!enemy.get(HealthComponent.class).isAlive()) break;
                ep = enemy.get(PositionComponent.class);
                hp = enemy.get(HealthComponent.class);
                if (ep == null || hp == null) break;

                boolean seesPlayer = fov.isVisible(playerX(), playerY()) &&
                    Math.abs(ep.x() - playerX()) <= 8 && Math.abs(ep.y() - playerY()) <= 8;
                seesPlayer = seesPlayer && lineOfSight(ep.x(), ep.y(), playerX(), playerY());

                var aiState = bt.update(seesPlayer, hp.hpRatio(), ep.x(), ep.y(), playerX(), playerY());

                switch (aiState) {
                    case ATTACK -> {
                        var evt = AttackSystem.meleeAttack(enemy, player);
                        showCombatText(playerX(), playerY(), "-" + evt.damage());
                        if (!player.get(HealthComponent.class).isAlive()) { gameOver(); return; }
                    }
                    case CHASE -> moveEnemy(enemy, ep, playerX(), playerY());
                    case FLEE -> {
                        var f = bt.getFleeTarget(ep.x(), ep.y(), playerX(), playerY());
                        moveEnemy(enemy, ep, clamp(f[0],1,GameConfig.MAP_WIDTH-2), clamp(f[1],1,GameConfig.MAP_HEIGHT-2));
                    }
                    case WANDER -> {
                        var w = bt.getWanderTarget(ep.x(), ep.y(), 2,2,GameConfig.MAP_WIDTH-3,GameConfig.MAP_HEIGHT-3);
                        moveEnemy(enemy, ep, w[0], w[1]);
                    }
                }
            }
        }
    }

    private void moveEnemy(Entity enemy, PositionComponent ep, int tx, int ty) {
        var path = pathFinder.findPath(ep.x(), ep.y(), tx, ty);
        if (!path.isEmpty()) {
            var step = path.get(0);
            if (isTileFree(step.x(), step.y())) {
                toggleEnemyAnim(enemy);
                ep.set(step.x(), step.y());
            }
        }
    }

    private void toggleEnemyAnim(Entity e) {
        int idx = enemies.indexOf(e);
        if (idx >= 0 && idx < enemyRects.size()) {
            var g = enemyRects.get(idx);
            if (g.getChildren().size() > 1) {
                var c = g.getChildren().remove(0);
                g.getChildren().add(c);
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

    private void victoryFloor() {
        if (floor >= GameConfig.MAX_FLOORS) {
            state = GameState.GAME_OVER; hud.remove(); worldGroup.setVisible(false);
            showVictory();
        } else {
            floor++; worldGroup.setVisible(false); showFloorClear();
        }
    }

    private void showFloorClear() {
        var t = new javafx.scene.text.Text("Floor Cleared!");
        t.setFont(Font.font("Monospaced", 36)); t.setFill(Color.GOLD);
        t.setX(GameConfig.SCREEN_WIDTH/2.0 - 150); t.setY(GameConfig.SCREEN_HEIGHT/2.0);
        FXGL.getGameScene().getRoot().getChildren().add(t);
        new Thread(() -> { try { Thread.sleep(1200); } catch (Exception ignored) {}
            javafx.application.Platform.runLater(() -> { FXGL.getGameScene().getRoot().getChildren().remove(t); startFloor(); });
        }).start();
    }

    private void showVictory() {
        var box = new javafx.scene.layout.VBox(20);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setLayoutX(GameConfig.SCREEN_WIDTH/2.0 - 150); box.setLayoutY(GameConfig.SCREEN_HEIGHT/2.0 - 80);
        var title = new javafx.scene.text.Text("YOU WIN!");
        title.setFont(Font.font("Monospaced", 48)); title.setFill(Color.GOLD);
        var stats = new javafx.scene.text.Text("All 3 floors conquered!");
        stats.setFont(Font.font("Monospaced", 16)); stats.setFill(Color.WHITE);
        var btn = new Button("Play Again");
        btn.setStyle("-fx-background-color: #333; -fx-text-fill: #eee; -fx-font-size: 16;");
        btn.setOnAction(e -> { FXGL.getGameScene().getRoot().getChildren().remove(box); newGame(); });
        box.getChildren().addAll(title, stats, btn);
        FXGL.getGameScene().getRoot().getChildren().add(box);
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

    private void setPlayerGroupPos(int sx, int sy) {
        playerGroup.setTranslateX(sx * TILESIZE + 1);
        playerGroup.setTranslateY(sy * TILESIZE + 1);
    }

    private void startHolding(int dx, int dy) {
        holdDir = new int[]{dx, dy}; lastMoveTime = 0;
        setPlayerFrame(1 + (int)(Math.random() * 2)); // walk frame
        tryMove(dx, dy);
    }
    private void stopHolding() { holdDir = null; setPlayerFrame(0); }

    private void showCombatText(int mx, int my, String text) {
        int sx = mx - (playerX() - COLS/2), sy = my - (playerY() - ROWS/2);
        javafx.application.Platform.runLater(() -> {
            var t = new javafx.scene.text.Text(text);
            t.setFont(Font.font("Monospaced", 14)); t.setFill(Color.YELLOW);
            t.setX(sx * TILESIZE + 4); t.setY(sy * TILESIZE - 6);
            worldGroup.getChildren().add(t);
            new Thread(() -> {
                try { Thread.sleep(600); } catch (Exception ignored) {}
                javafx.application.Platform.runLater(() -> worldGroup.getChildren().remove(t));
            }).start();
        });
    }

    private void drawPixels(Canvas c, boolean walkL, boolean walkR, boolean attack) {
        GraphicsContext g = c.getGraphicsContext2D();
        int p = 3;
        g.setFill(Color.rgb(30, 35, 30)); g.fillRect(0,0,c.getWidth(),c.getHeight());
        // Hair
        g.setFill(Color.rgb(60, 50, 30)); g.fillRect(p*4,p*0,p*3,p*1);
        g.fillRect(p*3,p*1,p*5,p*1);
        // Face
        g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p*3,p*2,p*5,p*3);
        g.setFill(Color.BLACK); g.fillRect(p*4,p*3,p*1,p*1); g.fillRect(p*6,p*3,p*1,p*1);
        // Body (green tunic)
        g.setFill(Color.rgb(30, 140, 30)); g.fillRect(p*3,p*5,p*5,p*4);
        g.setFill(Color.rgb(80, 50, 20)); g.fillRect(p*3,p*5,p*5,p*1);
        // Arms
        if (attack) {
            g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p*1,p*5,p*2,p*3);
            g.setFill(Color.rgb(200,200,200)); g.fillRect(p*9,p*3,p*1,p*5);
            g.setFill(Color.rgb(140,100,30)); g.fillRect(p*8,p*6,p*1,p*2);
            g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p*8,p*5,p*1,p*2);
        } else {
            g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p*1,p*5,p*2,p*3); g.fillRect(p*8,p*5,p*2,p*3);
        }
        // Legs
        g.setFill(Color.rgb(60, 40, 20));
        if (walkL) { g.fillRect(p*3,p*9,p*2,p*2); g.fillRect(p*6,p*9,p*2,p*1); }
        else if (walkR) { g.fillRect(p*3,p*9,p*2,p*1); g.fillRect(p*6,p*9,p*2,p*2); }
        else { g.fillRect(p*3,p*9,p*2,p*2); g.fillRect(p*6,p*9,p*2,p*2); }
        // Shoes
        g.setFill(Color.rgb(50, 30, 10)); g.fillRect(p*3,p*10,p*2,p*1); g.fillRect(p*6,p*10,p*2,p*1);
    }

    private void setPlayerFrame(int idx) {
        if (playerFrameIdx == idx) return;
        playerGroup.getChildren().remove(playerFrames[playerFrameIdx]);
        playerFrames[idx].setTranslateX(0); playerFrames[idx].setTranslateY(0);
        playerGroup.getChildren().add(0, playerFrames[idx]);
        playerFrameIdx = idx;
    }

    private void drawEnemy(Canvas c, String kind, boolean animFrame) {
        GraphicsContext g = c.getGraphicsContext2D();
        int p = 3; g.clearRect(0,0,c.getWidth(),c.getHeight());
        g.setFill(Color.rgb(30,35,30)); g.fillRect(0,0,c.getWidth(),c.getHeight());

        switch (kind) {
            case "bat" -> drawBatPixels(g, p, animFrame);
            case "skeleton" -> drawSkeletonPixels(g, p, animFrame);
            default -> drawSlimePixels(g, p, animFrame);
        }
    }

    private void drawBatPixels(GraphicsContext g, int p, boolean flap) {
        g.setFill(Color.rgb(180, 40, 40));
        g.fillRect(p*3, p*2, p*5, p*4); // body
        g.fillRect(p*4, p*1, p*3, p*2); // head
        g.setFill(Color.BLACK); g.fillRect(p*5, p*2, p*1, p*1); g.fillRect(p*6, p*2, p*1, p*1); // eyes
        if (flap) {
            g.setFill(Color.rgb(220, 60, 60)); g.fillRect(p*1, p*3, p*2, p*2); g.fillRect(p*8, p*3, p*2, p*2); // wings up
        } else {
            g.setFill(Color.rgb(220, 60, 60)); g.fillRect(p*1, p*5, p*2, p*3); g.fillRect(p*8, p*5, p*2, p*3); // wings down
        }
    }

    private void drawSkeletonPixels(GraphicsContext g, int p, boolean walk) {
        g.setFill(Color.rgb(200, 160, 200)); // light purple bone
        g.fillRect(p*4, p*0, p*3, p*2); // skull
        g.setFill(Color.BLACK); g.fillRect(p*5, p*1, p*1, p*1); g.fillRect(p*6, p*1, p*1, p*1);
        g.fillRect(p*3, p*2, p*5, p*5); // ribcage
        g.fillRect(p*2, p*3, p*1, p*3); // left arm
        g.fillRect(p*8, p*3, p*1, p*3); // right arm
        if (walk) {
            g.fillRect(p*3, p*7, p*2, p*2); g.fillRect(p*6, p*7, p*2, p*1); // legs offset
        } else {
            g.fillRect(p*3, p*7, p*2, p*2); g.fillRect(p*6, p*7, p*2, p*2); // legs even
        }
    }

    private void drawSlimePixels(GraphicsContext g, int p, boolean wobble) {
        g.setFill(Color.rgb(80, 200, 80));
        if (wobble) g.fillRect(p*2, p*3, p*7, p*6);
        else g.fillRect(p*3, p*2, p*5, p*7);
        g.setFill(Color.BLACK);
        g.fillRect(p*4, p*4, p*1, p*2); // eye
        g.fillRect(p*6, p*4, p*1, p*2); // eye
    }

    public static void main(String[] args) {
        java.lang.System.setProperty("com.sun.javafx.ime", "disabled");
        java.lang.System.setProperty("javafx.ime", "disabled");
        launch(args);
    }
}
