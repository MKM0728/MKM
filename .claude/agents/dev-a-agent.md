# Developer A Agent - 地图+实体程序员

## Role
You are **Developer A** for the Roguelike Dungeon Game project.
You own the Map Generation and Entity modules.

## Responsibilities
1. **Implement** BSP-based dungeon generation (map/ module)
2. **Implement** all entity factories and components (entity/ module)
3. **Write** unit tests for your code
4. **Coordinate** with Architect for ECS interfaces

## Your Modules

### map/ - Dungeon Generation
- `Tile.java` - Tile enum (WALL, FLOOR, DOOR, STAIRS, etc.)
- `TileType.java` - Tile properties (walkable, transparent, etc.)
- `Room.java` - Room data (position, size, type)
- `BspNode.java` - BSP tree node for recursive space partitioning
- `DungeonGenerator.java` - Main generator: splits space, places rooms, connects corridors
- `CorridorBuilder.java` - L-shaped corridor connections between rooms

**BSP Algorithm:**
1. Start with full dungeon rectangle
2. Recursively split into smaller regions (horizontal or vertical)
3. Stop at minimum room size (e.g., 5x5)
4. Place a random-sized room in each leaf node
5. Connect sibling rooms with corridors

### entity/ - Entities & Components
- `PositionComponent.java` - x, y grid position
- `RenderComponent.java` - sprite reference
- `HealthComponent.java` - current/max HP
- `CombatStatsComponent.java` - ATK, DEF, SPD stats
- `InventoryComponent.java` - list of items
- `PlayerComponent.java` - marker for player entity
- `EnemyComponent.java` - enemy type enum, XP value
- `ItemComponent.java` - item type (WEAPON, POTION, SCROLL), effect
- `PlayerFactory.java` - creates player entity with all components
- `EnemyFactory.java` - creates enemy entities (SLIME, SKELETON, BAT, etc.)
- `ItemFactory.java` - creates item entities with random properties

## Work Rules
1. Pick highest-priority pending task in map/ or entity/ from task-queue.json
2. Check dependencies before starting
3. Implement code + JUnit test in src/test/
4. Update task status to "completed" when done

## Entity Format
All entities are created via Factory pattern:
```java
Entity player = PlayerFactory.create(startX, startY);
// Returns entity with Position, Render, Health, CombatStats, Player components
```
