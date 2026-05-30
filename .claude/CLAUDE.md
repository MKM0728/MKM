# Roguelike Dungeon Game - Agent Workflow

## Project Overview
A 2D pixel-art turn-based Roguelike dungeon crawler built with Java 17 + FXGL game engine.
Target: single fat-jar executable, open-sourced on GitHub.

## Tech Stack
- Java 17+ (LTS)
- FXGL game engine (MIT license)
- Gradle build system with shadowJar plugin
- JSON for save data
- Tiled (.tmx) for map definitions
- 16x16 pixel art sprites

## Project Structure
```
src/main/java/com/roguelike/
├── core/       # Game loop, ECS framework, state machine
├── map/        # BSP dungeon generation, tiles, rooms
├── entity/     # Player, enemy, item factories + components
├── combat/     # Turn manager, damage calc, attack system
├── ai/         # A* pathfinding, behavior tree, FOV
├── ui/         # HUD, inventory, menus (FXGL UI)
├── save/       # Game state serialization
└── asset/      # Resource loading helpers

src/main/resources/assets/
├── sprites/    # 16x16 pixel art PNGs
├── sounds/     # SFXR-generated 8-bit WAVs
└── maps/       # .tmx template maps
```

## Agent System Architecture
5 specialized agents work 24/7 on rotating schedules:

| Role | Agent Name | Module | Interval |
|------|-----------|--------|----------|
| PM | pm-agent | Task coordination | 30 min |
| Architect | architect-agent | core/ interfaces | 45 min |
| Dev A | dev-a-agent | map/ + entity/ | 20 min |
| Dev B | dev-b-agent | combat/ + ai/ | 20 min |
| Dev C | dev-c-agent | ui/ + save/ | 20 min |

## Coding Conventions
- Java 17 record types for DTOs
- Builder pattern for entity creation
- ECS architecture: Entity = bag of Components, Systems process components
- All game constants in a single GameConfig class
- No Lombok - use vanilla Java
- JUnit 5 for tests
- Log with SLF4J (bundled with FXGL)
