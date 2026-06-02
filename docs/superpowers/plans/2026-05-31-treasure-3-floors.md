# 三层关卡 + 宝箱收集通关 实现计划

> **For agentic workers:** 使用 superpowers:subagent-driven-development 逐任务实现。步骤用 checkbox (`- [ ]`) 追踪。

**Goal:** 实现三层递增难度关卡，每层有随机分布的像素宝箱，玩家收集宝箱后通关进入下一层，三层全部通过后显示胜利画面。

**Architecture:** 在 GameConfig 添加关卡参数方法（地图尺寸、敌人数量），在 GameApp 中添加宝箱生成/渲染/检测、楼层切换画面、胜利画面。GameState 增加 `VICTORY` 状态。不引入新文件，所有改动集中在现有类中。

**Tech Stack:** Java 17, FXGL 17.3, JavaFX Canvas (像素绘制)

---

### Task 1: GameConfig 关卡参数化

**Files:**
- Modify: `src/main/java/com/roguelike/core/GameConfig.java`

- [ ] **Step 1: 添加强关卡参数方法**

将当前静态常量 `ENEMY_COUNT` 和 `MAP_WIDTH/MAP_HEIGHT` 改为按楼层返回的方法，同时添加 `MAX_FLOORS=3`：

```java
package com.roguelike.core;

public final class GameConfig {
    private GameConfig() {}

    public static final String GAME_TITLE = "Roguelike Dungeon";
    public static final int SCREEN_WIDTH = 960;
    public static final int SCREEN_HEIGHT = 640;
    public static final int TILE_SIZE = 32;

    public static final int MIN_ROOM_SIZE = 5;
    public static final int MIN_AREA_SIZE = 7;
    public static final int MAX_BSP_DEPTH = 5;

    public static final int PLAYER_BASE_HP = 100;
    public static final int PLAYER_BASE_ATK = 8;
    public static final int PLAYER_BASE_DEF = 0;
    public static final int PLAYER_BASE_SPD = 10;

    public static final int MAX_FLOORS = 3;
    public static final int FOV_RADIUS = 8;

    public static int mapWidth(int floor) { return 30 + floor * 20; }
    public static int mapHeight(int floor) { return 20 + floor * 10; }
    public static int enemyCount(int floor) { return 5 * (int)Math.pow(5, floor - 1); }
}
```

- [ ] **Step 2: 编译验证**

```powershell
gradle compileJava
```
Expected: COMPILE SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/roguelike/core/GameConfig.java
git commit -m "feat: add floor-based map size and enemy count to GameConfig"
```

---

### Task 2: 宝箱像素绘制

**Files:**
- Modify: `src/main/java/com/roguelike/core/GameApp.java`

- [ ] **Step 1: 在 `makePlayerFrame` 方法后添加宝箱绘制方法**

```java
private Canvas makeTreasureChest() {
    Canvas c = new Canvas(TILESIZE, TILESIZE);
    GraphicsContext g = c.getGraphicsContext2D();
    int p = 3;
    g.setFill(Color.rgb(30, 35, 30)); g.fillRect(0, 0, TILESIZE, TILESIZE);
    // Chest body
    g.setFill(Color.rgb(139, 100, 30)); g.fillRect(p * 2, p * 3, p * 7, p * 6);
    // Gold lid
    g.setFill(Color.rgb(200, 170, 50)); g.fillRect(p * 3, p * 2, p * 5, p * 2);
    // Lock
    g.setFill(Color.rgb(255, 215, 0)); g.fillRect(p * 5, p * 5, p * 2, p * 2);
    // Sparkle
    g.setFill(Color.rgb(255, 255, 200)); g.fillRect(p * 6, p * 6, p * 1, p * 1);
    return c;
}
```

- [ ] **Step 2: 编译验证**

```powershell
gradle compileJava
```
Expected: COMPILE SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/roguelike/core/GameApp.java
git commit -m "feat: add treasure chest pixel art method"
```

---

### Task 3: startFloor 支持动态地图 + 宝箱生成

**Files:**
- Modify: `src/main/java/com/roguelike/core/GameApp.java` (startFloor, spawnEnemies 方法)

- [ ] **Step 1: 添加宝箱字段**

在 `private final List<Group> enemyGroups` 后添加：

```java
private Group chestGroup;
```

- [ ] **Step 2: 重写 spawnEnemies 使用 floor 参数**

替换 `spawnEnemies()`：

```java
private void spawnEnemies() {
    int mw = GameConfig.mapWidth(floor), mh = GameConfig.mapHeight(floor);
    int count = GameConfig.enemyCount(floor) / 2;
    for (int i = 0; i < count; i++) {
        int x, y;
        do { x = 2 + (int)(Math.random() * (mw - 4)); y = 2 + (int)(Math.random() * (mh - 4));
        } while (dungeon[y][x] != Tile.FLOOR || (x == playerX() && y == playerY()));
        enemies.add(EnemyFactory.create(EnemyType.BAT, x, y));
    }
    for (int i = 0; i < count; i++) {
        int x, y;
        do { x = 2 + (int)(Math.random() * (mw - 4)); y = 2 + (int)(Math.random() * (mh - 4));
        } while (dungeon[y][x] != Tile.FLOOR || (x == playerX() && y == playerY()));
        enemies.add(EnemyFactory.create(EnemyType.SKELETON, x, y));
    }
}
```

- [ ] **Step 3: 重写 startFloor 支持动态地图和宝箱**

替换 `startFloor()`：

```java
private void startFloor() {
    enemies.clear();
    enemyGroups.forEach(g -> worldGroup.getChildren().remove(g));
    enemyGroups.clear();
    if (chestGroup != null) worldGroup.getChildren().remove(chestGroup);

    int mw = GameConfig.mapWidth(floor), mh = GameConfig.mapHeight(floor);
    generator = new DungeonGenerator(mw, mh, seed + floor);
    dungeon = generator.generate(GameConfig.MAX_BSP_DEPTH + floor / 2);
    pathFinder = new PathFinder(dungeon);
    explored = new boolean[mh][mw];

    player = PlayerFactory.create(generator.stairsUpX(), generator.stairsUpY());
    spawnEnemies();

    for (var e : enemies) {
        var ec = e.get(EnemyComponent.class);
        String kind = ec != null ? ec.type().kind() : "slime";
        var c0 = makeEnemyFrame(kind, false);
        var g = new Group(c0); g.setVisible(false);
        enemyGroups.add(g); worldGroup.getChildren().add(g);
    }

    // Spawn treasure chest
    int cx, cy;
    do { cx = 2 + (int)(Math.random() * (mw - 4)); cy = 2 + (int)(Math.random() * (mh - 4));
    } while (dungeon[cy][cx] != Tile.FLOOR || (cx == playerX() && cy == playerY()));
    chestGroup = new Group(makeTreasureChest());
    chestGroup.setTranslateX(cx * TILESIZE); chestGroup.setTranslateY(cy * TILESIZE);
    worldGroup.getChildren().add(chestGroup);

    visible = fov.compute(dungeon, playerX(), playerY()); markExplored();
    worldGroup.setVisible(true); playerGroup.toFront();
    if (chestGroup != null) chestGroup.toFront();
    state = GameState.PLAYING; playerMoves = PLAYER_SPEED;
    renderAll(); updateHud();
}
```

- [ ] **Step 4: 更新 tryMove 检测宝箱**

在 `tryMove` 中，`isWalkable` 检查之后、攻击检查之前添加：

```java
// Check for treasure chest
if (chestGroup != null && chestGroup.isVisible() &&
    (int)(chestGroup.getTranslateX() / TILESIZE) == nx &&
    (int)(chestGroup.getTranslateY() / TILESIZE) == ny) {
    collectChest(); return;
}
```

- [ ] **Step 5: 添加 collectChest 方法**

```java
private void collectChest() {
    worldGroup.getChildren().remove(chestGroup);
    chestGroup = null;
    if (floor >= GameConfig.MAX_FLOORS) {
        state = GameState.GAME_OVER;
        hud.remove(); worldGroup.setVisible(false);
        showVictory();
    } else {
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
    new Thread(() -> {
        try { Thread.sleep(1500); } catch (Exception ignored) {}
        javafx.application.Platform.runLater(() -> {
            FXGL.getGameScene().getRoot().getChildren().remove(t);
            startFloor();
        });
    }).start();
}

private void showVictory() {
    var box = new VBox(20);
    box.setAlignment(javafx.geometry.Pos.CENTER);
    box.setLayoutX(GameConfig.SCREEN_WIDTH / 2.0 - 150);
    box.setLayoutY(GameConfig.SCREEN_HEIGHT / 2.0 - 80);
    var title = new javafx.scene.text.Text("YOU WIN!");
    title.setFont(Font.font("Monospaced", FontWeight.BOLD, 48));
    title.setFill(Color.GOLD);
    var stats = new javafx.scene.text.Text("All 3 floors conquered!");
    stats.setFont(Font.font("Monospaced", 16)); stats.setFill(Color.WHITE);
    var btn = new Button("Play Again");
    btn.setFont(Font.font(16));
    btn.setStyle("-fx-background-color: #333; -fx-text-fill: #eee; -fx-font-size: 16;");
    btn.setOnAction(e -> { FXGL.getGameScene().getRoot().getChildren().remove(box); newGame(); });
    box.getChildren().addAll(title, stats, btn);
    FXGL.getGameScene().getRoot().getChildren().add(box);
}
```

- [ ] **Step 6: 添加 VBox import**

在文件顶部 imports 添加：
```java
import javafx.scene.layout.VBox;
```

- [ ] **Step 7: 编译测试**

```powershell
gradle test fatJar
```
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/roguelike/core/GameApp.java
git commit -m "feat: dynamic map scaling, treasure chests, 3-floor victory system"
```

---

### Task 4: 渲染宝箱在 FOV 内可见

**Files:**
- Modify: `src/main/java/com/roguelike/core/GameApp.java` (renderAll 方法)

- [ ] **Step 1: 在 renderAll 末尾添加宝箱可见性控制**

在 `renderAll()` 方法末尾添加：

```java
// Treasure chest visibility
if (chestGroup != null) {
    int cx = (int)(chestGroup.getTranslateX() / TILESIZE);
    int cy = (int)(chestGroup.getTranslateY() / TILESIZE);
    chestGroup.setVisible(visible[cy][cx] || explored[cy][cx]);
    if (visible[cy][cx]) chestGroup.toFront();
}
```

- [ ] **Step 2: 编译测试 + 提交**

```powershell
gradle test fatJar && java -jar build/libs/roguelike-dungeon-0.1.0.jar
```
```bash
git add src/main/java/com/roguelike/core/GameApp.java
git commit -m "fix: treasure chest visibility tied to FOV"
```

---

### Task 5: 验收测试

- [ ] **Step 1: 三层通关完整流程**

Test: 启动游戏 → New Game → 第一层击杀敌人 → 找到宝箱(金色箱子) → 点击 → "Floor 2" 过渡画面 → 第二层(更大的地图) → 找宝箱 → 第三层 → 宝箱 → "YOU WIN" 胜利画面 → Play Again 重新开始

Expected: 全程无异常、无乱码、动画正常、按钮正常

- [ ] **Step 2: Commit final**

```bash
git add -A
git commit -m "chore: final acceptance test passed - 3-floor treasure system complete"
```

---

## Self-Review

**1. Spec coverage:**
- ✅ 三层关卡 (Task 3: MAX_FLOORS=3)
- ✅ 宝箱像素 (Task 2: makeTreasureChest)
- ✅ 随机位置 (Task 3: random cx,cy)
- ✅ 收集通关 (Task 3: collectChest)
- ✅ 进入下一层 (Task 3: showFloorTransition)
- ✅ 三层胜利 (Task 3: showVictory)
- ✅ 地图随关卡扩大 (Task 3: mapWidth/mapHeight)
- ✅ 敌人指数增长 (Task 3: enemyCount = 5^floor)

**2. Placeholder scan:** No TBD/TODO/placeholders found.

**3. Type consistency:** `floor` is `int`, `GameConfig.mapWidth(int)` returns `int`, `DungeonGenerator(int,int,long)` matches. `chestGroup` is `Group`, contains `Canvas` from `makeTreasureChest()`.
