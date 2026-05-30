package com.roguelike.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.roguelike.map.Tile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class PathFinderTest {

    private PathFinder pathFinder;
    private Tile[][] grid;

    @BeforeEach
    void setUp() {
        grid = new Tile[5][5];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                grid[y][x] = Tile.FLOOR;
            }
        }
        pathFinder = new PathFinder(grid);
    }

    @Test
    void straightPath() {
        var path = pathFinder.findPath(0, 0, 3, 0);
        assertFalse(path.isEmpty());
        assertEquals(3, path.size());
    }

    @Test
    void samePositionReturnsEmpty() {
        var path = pathFinder.findPath(2, 2, 2, 2);
        assertTrue(path.isEmpty());
    }

    @Test
    void blockedByWall() {
        grid[2][1] = Tile.WALL;
        grid[2][2] = Tile.WALL;
        grid[2][3] = Tile.WALL;
        pathFinder = new PathFinder(grid);
        var path = pathFinder.findPath(0, 2, 4, 2);
        assertTrue(path.size() > 4);
    }

    @Test
    void noPathWhenTrapped() {
        for (int x = 0; x < 5; x++) {
            grid[1][x] = Tile.WALL;
        }
        grid[1][2] = Tile.FLOOR;
        var enclosed = new Tile[5][5];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                enclosed[y][x] = Tile.WALL;
            }
        }
        enclosed[0][0] = Tile.FLOOR;
        enclosed[4][4] = Tile.FLOOR;
        pathFinder = new PathFinder(enclosed);
        var path = pathFinder.findPath(0, 0, 4, 4);
        assertTrue(path.isEmpty());
    }

    @Test
    void diagonalPath() {
        var path = pathFinder.findPath(0, 0, 4, 4);
        assertFalse(path.isEmpty());
    }
}
