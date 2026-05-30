package com.roguelike.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.roguelike.map.Tile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FovSystemTest {

    private Tile[][] grid;
    private FovSystem fov;

    @BeforeEach
    void setUp() {
        grid = new Tile[20][20];
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                grid[y][x] = Tile.FLOOR;
            }
        }
        fov = new FovSystem(8);
    }

    @Test
    void originIsAlwaysVisible() {
        var vis = fov.compute(grid, 10, 10);
        assertTrue(vis[10][10]);
    }

    @Test
    void nearbyTilesVisible() {
        var vis = fov.compute(grid, 10, 10);
        assertTrue(vis[10][11]);
        assertTrue(vis[11][10]);
    }

    @Test
    void wallBlocksVision() {
        grid[10][12] = Tile.WALL;
        fov = new FovSystem(8);
        var vis = fov.compute(grid, 10, 10);
        assertTrue(vis[10][11]);
        assertTrue(vis[10][12]);  // wall itself is visible
        assertFalse(vis[10][13]); // behind wall, blocked
    }

    @Test
    void outOfRangeNotVisible() {
        var vis = fov.compute(grid, 2, 2);
        assertFalse(vis[2][19]);
    }

    @Test
    void openAreaFullyVisibleWithinRadius() {
        fov = new FovSystem(3);
        var vis = fov.compute(grid, 10, 10);
        int count = 0;
        for (int y = 7; y <= 13; y++) {
            for (int x = 7; x <= 13; x++) {
                if (vis[y][x]) count++;
            }
        }
        assertTrue(count > 20, "Most tiles within radius should be visible");
    }
}
