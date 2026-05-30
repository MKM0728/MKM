# Roguelike Dungeon

A 2D pixel-art turn-based Roguelike dungeon crawler built with **Java 17 + FXGL**.

[![CI](https://github.com/rullerzhou-afk/roguelike-dungeon/actions/workflows/build.yml/badge.svg)](https://github.com/rullerzhou-afk/roguelike-dungeon/actions/workflows/build.yml)

## Gameplay

- Procedurally generated dungeons (BSP algorithm)
- Turn-based movement & combat
- 3 enemy types: Slime, Skeleton, Bat
- Items: Weapons, Potions, Scrolls
- Field of View + Fog of War
- A* pathfinding enemy AI with behavior tree
- 20 floors to conquer

## Controls

| Key | Action |
|-----|--------|
| WASD | Move / Attack |
| I | Inventory |
| ESC | Pause |

## Build & Run

```bash
# Build fat jar
./gradlew shadowJar

# Run
java -jar build/libs/roguelike-dungeon-0.1.0.jar
```

Requires **JDK 17+**.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Engine | [FXGL](https://github.com/AlmasB/FXGL) 21.1 |
| Build | Gradle + ShadowJar |
| Save | Gson JSON |
| Test | JUnit 5 |
| CI | GitHub Actions |

## Project Structure

```
src/main/java/com/roguelike/
├── core/       # ECS framework, game loop, state machine
├── map/        # BSP dungeon generation, tiles, rooms
├── entity/     # Player, enemy, item factories & components
├── combat/     # Turn manager, damage calc, attack system
├── ai/         # A* pathfinding, behavior tree, FOV
├── ui/         # HUD, inventory, menus
└── save/       # JSON save/load
```

---

# Roguelike Dungeon (中文)

基于 **Java 17 + FXGL** 的 2D 像素风回合制 Roguelike 地牢探险游戏。

## 玩法

- 程序化随机生成地牢（BSP 算法）
- 回合制移动与战斗
- 3 种敌人：史莱姆、骷髅、蝙蝠
- 道具：武器、药水、卷轴
- 视野 + 战争迷雾
- A* 寻路 + 行为树 AI
- 共 20 层

## 操作

| 按键 | 动作 |
|------|------|
| WASD | 移动/攻击 |
| I | 背包 |
| ESC | 暂停 |

## 构建运行

```bash
./gradlew shadowJar
java -jar build/libs/roguelike-dungeon-0.1.0.jar
```

需要 **JDK 17+**。

## 技术栈

| 层级 | 技术 |
|------|------|
| 引擎 | FXGL 21.1 |
| 构建 | Gradle + ShadowJar |
| 存档 | Gson JSON |
| 测试 | JUnit 5 |
| CI | GitHub Actions |

## License

MIT
