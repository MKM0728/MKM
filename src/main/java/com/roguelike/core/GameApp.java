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
import com.roguelike.ui.MenuScreen;

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
    private HBox dpad;

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
            makePlayerFrame(false, false, false),
            makePlayerFrame(true, false, false),
            makePlayerFrame(false, true, false),
            makePlayerFrame(false, false, true)
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

        showMenu();
    }

    @Override
    protected void onUpdate(double tpf) {
        if (dpad != null) dpad.toFront();
        if (state != GameState.PLAYING || animating) return;

        if (holdDir != null && playerMoves > 0) {
            long now = java.lang.System.currentTimeMillis();
            if (now - lastMoveTime > 120) { tryMove(holdDir[0], holdDir[1]); lastMoveTime = now; }
        }

        if (playerMoves <= 0) {
            enemyTurns(); playerMoves = PLAYER_SPEED;
            renderAll(); updateHud(); checkState();
        }
    }

    // --- Pixel art ---
    private Canvas makePlayerFrame(boolean wl, boolean wr, boolean atk) {
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
        // Arms
        if (atk) {
            g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 1, p * 5, p * 2, p * 3);
            g.setFill(Color.rgb(200, 200, 200)); g.fillRect(p * 9, p * 3, p * 1, p * 5);
            g.setFill(Color.rgb(140, 100, 30)); g.fillRect(p * 8, p * 6, p * 1, p * 2);
            g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 8, p * 5, p * 1, p * 2);
        } else {
            g.setFill(Color.rgb(255, 220, 180)); g.fillRect(p * 1, p * 5, p * 2, p * 3); g.fillRect(p * 8, p * 5, p * 2, p * 3);
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
                g.setFill(Color.rgb(180, 40, 40)); g.fillRect(p * 3, p * 2, p * 5, p * 4);
                g.fillRect(p * 4, p * 1, p * 3, p * 2);
                g.setFill(Color.BLACK); g.fillRect(p * 5, p * 2, p * 2, p * 1);
                if (alt) {
                    g.setFill(Color.rgb(220, 60, 60)); g.fillRect(p * 1, p * 3, p * 2, p * 2);
                } else {
                    g.setFill(Color.rgb(220, 60, 60)); g.fillRect(p * 1, p * 5, p * 2, p * 3);
                }
                g.fillRect(p * 8, alt ? p * 3 : p * 5, p * 2, alt ? p * 2 : p * 3);
            }
            case "skeleton" -> {
                g.setFill(Color.rgb(200, 160, 200));
                g.fillRect(p * 4, p * 0, p * 3, p * 2);
                g.setFill(Color.BLACK); g.fillRect(p * 5, p * 1, p * 1, p * 1); g.fillRect(p * 6, p * 1, p * 1, p * 1);
                g.fillRect(p * 3, p * 2, p * 5, p * 5); g.fillRect(p * 2, p * 3, p * 1, p * 3); g.fillRect(p * 8, p * 3, p * 1, p * 3);
                if (alt) { g.fillRect(p * 3, p * 7, p * 2, p * 2); g.fillRect(p * 6, p * 7, p * 2, p * 1); }
                else { g.fillRect(p * 3, p * 7, p * 2, p * 2); g.fillRect(p * 6, p * 7, p * 2, p * 2); }
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
        state = GameState.MENU; GameOverScreen.hide(); hud.remove();
        worldGroup.setVisible(false);
        MenuScreen.show(this::newGame, () -> loadGame("auto"), () -> FXGL.getGameController().exit());
    }

    private void newGame() {
        MenuScreen.hide(); GameOverScreen.hide();
        seed = java.lang.System.currentTimeMillis(); floor = 1; turns = 0; enemiesSlain = 0;
        startFloor();
    }

    private void startFloor() {
        enemies.clear(); enemyGroups.forEach(g -> worldGroup.getChildren().remove(g)); enemyGroups.clear();
        if (chestGroup != null) { worldGroup.getChildren().remove(chestGroup); chestGroup = null; }

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
        int count = GameConfig.ENEMY_COUNT;
        var types = EnemyType.values();
        for (int i = 0; i < count; i++) {
            int x, y;
            do { x = 2 + (int)(Math.random() * (GameConfig.mapWidth(floor) - 4)); y = 2 + (int)(Math.random() * (GameConfig.mapHeight(floor) - 4));
            } while (dungeon[y][x] != Tile.FLOOR || (x == playerX() && y == playerY()));
            enemies.add(EnemyFactory.create(types[(int)(Math.random() * types.length)], x, y));
        }
    }

    // --- Player actions ---
    private void tryMove(int dx, int dy) {
        if (state != GameState.PLAYING || playerMoves <= 0 || animating) return;
        int nx = playerX() + dx, ny = playerY() + dy;
        if (ny < 0 || ny >= dungeon.length || nx < 0 || nx >= dungeon[0].length) return;
        if (dungeon[ny][nx] == Tile.STAIRS_DOWN) { floor++; startFloor(); return; }
        if (!dungeon[ny][nx].isWalkable()) return;

        // Check treasure chest
        if (chestGroup != null && chestGroup.isVisible() && chestX == nx && chestY == ny) {
            collectChest(); return;
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
        setPlayerFrame(3);
        playerGroup.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.8, 0, 0);");
        var hp = enemy.get(HealthComponent.class);
        if (hp != null) hp.takeDamage(999);
        enemiesSlain++;
        showFloatingText(nx, ny, "DEAD!");
        playerMoves--; turns++;
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
        var anim = new TranslateTransition(Duration.millis(dur), playerGroup);
        anim.setFromX(0); anim.setFromY(0);
        anim.setToX((nx - fx) * TILESIZE); anim.setToY((ny - fy) * TILESIZE);
        anim.setOnFinished(e -> {
            playerGroup.setTranslateX(0); playerGroup.setTranslateY(0);
            player.get(PositionComponent.class).set(nx, ny);
            visible = fov.compute(dungeon, nx, ny); markExplored();
            playerMoves--; turns++;
            renderAll();
            animating = false;
            if (holdDir == null) setPlayerFrame(0);
        });
        anim.play();
    }

    private void showFloatingText(int mx, int my, String text) {
        int sx = mx - (playerX() - COLS / 2), sy = my - (playerY() - ROWS / 2);
        javafx.application.Platform.runLater(() -> {
            var t = new javafx.scene.text.Text(text);
            t.setFont(Font.font("Monospaced", FontWeight.BOLD, 14)); t.setFill(Color.YELLOW);
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
    }

    private void markExplored() {
        for (int y = 0; y < GameConfig.mapHeight(floor); y++)
            for (int x = 0; x < GameConfig.mapWidth(floor); x++)
                if (visible[y][x]) explored[y][x] = true;
    }

    // --- Enemy AI ---
    private void enemyTurns() {
        var bt = new BehaviorTree();
        for (var e : new ArrayList<>(enemies)) {
            if (!e.get(HealthComponent.class).isAlive()) { enemies.remove(e); continue; }
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

    private boolean isTileFree(int x, int y) {
        if (y < 0 || y >= dungeon.length || x < 0 || x >= dungeon[0].length) return false;
        if (x == playerX() && y == playerY()) return false;
        for (var e : enemies) { var ep = e.get(PositionComponent.class); if (ep != null && ep.x() == x && ep.y() == y) return false; }
        return dungeon[y][x].isWalkable();
    }

    // --- Save/Load ---
    private void loadGame(String s) { try { var d = SaveManager.load(s); if (d == null) { newGame(); return; } floor = d.floor; seed = d.seed; startFloor(); player.get(PositionComponent.class).set(d.playerX, d.playerY); player.get(HealthComponent.class).takeDamage(player.get(HealthComponent.class).hp() - d.playerHp); } catch (Exception ex) { newGame(); } }

    private void collectChest() {
        worldGroup.getChildren().remove(chestGroup);
        chestGroup = null;
        holdDir = null; // clear any held direction
        if (floor >= GameConfig.MAX_FLOORS) {
            state = GameState.GAME_OVER;
            hud.remove(); worldGroup.setVisible(false);
            showVictory();
        } else {
            state = GameState.MENU; // block game loop during transition
            floor++;
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
        FXGL.getGameScene().getRoot().getChildren().add(box);
    }

    private void gameOver() { state = GameState.GAME_OVER; hud.remove(); worldGroup.setVisible(false); GameOverScreen.show(floor, enemiesSlain, turns, this::newGame, this::showMenu); }
    private void checkState() { if (!player.get(HealthComponent.class).isAlive()) gameOver(); }
    private void updateHud() { var hp = player.get(HealthComponent.class); hud.update(hp.hp(), hp.maxHp(), floor, turns); }
    private int playerX() { return player.get(PositionComponent.class).x(); }
    private int playerY() { return player.get(PositionComponent.class).y(); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public static void main(String[] args) { launch(args); }
}
