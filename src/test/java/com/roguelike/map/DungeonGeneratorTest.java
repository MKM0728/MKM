package com.roguelike.map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DungeonGeneratorTest {

    @Test
    void generateCreatesDungeon() {
        var gen = new DungeonGenerator(60, 40, 12345);
        var grid = gen.generate(4);

        assertEquals(40, grid.length);
        assertEquals(60, grid[0].length);

        int floorCount = 0;
        int wallCount = 0;
        for (var row : grid) {
            for (var tile : row) {
                if (tile.isWalkable()) floorCount++;
                if (tile == Tile.WALL) wallCount++;
            }
        }
        assertTrue(floorCount > 0, "Dungeon must have walkable tiles");
        assertTrue(wallCount > 0, "Dungeon must have walls");
        assertTrue(floorCount < 60 * 40 * 0.6, "Dungeon should not be entirely floors");
    }

    @Test
    void stairsArePlaced() {
        var gen = new DungeonGenerator(50, 30, 42);
        gen.generate(4);

        boolean hasUp = false, hasDown = false;
        for (var row : gen.generate(4)) {
            for (var tile : row) {
                if (tile == Tile.STAIRS_UP) hasUp = true;
                if (tile == Tile.STAIRS_DOWN) hasDown = true;
            }
        }
        assertTrue(hasUp, "Stairs up must exist");
        assertTrue(hasDown, "Stairs down must exist");
    }

    @Test
    void deterministicWithSameSeed() {
        var gen1 = new DungeonGenerator(50, 30, 999);
        var grid1 = gen1.generate(4);

        var gen2 = new DungeonGenerator(50, 30, 999);
        var grid2 = gen2.generate(4);

        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 50; x++) {
                assertEquals(grid1[y][x], grid2[y][x],
                    "Same seed must produce identical dungeon");
            }
        }
    }

    @Test
    void differentSeedProducesDifferentDungeon() {
        var gen1 = new DungeonGenerator(50, 30, 111);
        var grid1 = gen1.generate(4);

        var gen2 = new DungeonGenerator(50, 30, 222);
        var grid2 = gen2.generate(4);

        int diff = 0;
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 50; x++) {
                if (grid1[y][x] != grid2[y][x]) diff++;
            }
        }
        assertTrue(diff > 0, "Different seeds should produce different dungeons");
    }

    @Test
    void bordersAreWalls() {
        var gen = new DungeonGenerator(40, 30, 7);
        var grid = gen.generate(4);

        for (int x = 0; x < 40; x++) {
            assertEquals(Tile.WALL, grid[0][x], "Top border must be wall");
            assertEquals(Tile.WALL, grid[29][x], "Bottom border must be wall");
        }
        for (int y = 0; y < 30; y++) {
            assertEquals(Tile.WALL, grid[y][0], "Left border must be wall");
            assertEquals(Tile.WALL, grid[y][39], "Right border must be wall");
        }
    }
}
