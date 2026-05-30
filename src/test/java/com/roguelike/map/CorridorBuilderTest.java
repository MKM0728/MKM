package com.roguelike.map;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorridorBuilderTest {

    private Tile[][] grid;
    private CorridorBuilder builder;
    private Random rng;

    @BeforeEach
    void setUp() {
        grid = new Tile[10][10];
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                grid[y][x] = Tile.WALL;
            }
        }
        rng = new Random(42);
        builder = new CorridorBuilder(grid, rng);
    }

    @Test
    void connectCreatesPath() {
        grid[2][2] = Tile.FLOOR;
        grid[2][8] = Tile.FLOOR;
        builder.connect(2, 2, 8, 2);

        int corridors = 0;
        for (var row : grid) {
            for (var tile : row) {
                if (tile == Tile.CORRIDOR) corridors++;
            }
        }
        assertTrue(corridors > 0, "Must create corridor tiles");
    }

    @Test
    void doesNotOverwriteFloors() {
        grid[5][5] = Tile.FLOOR;
        builder.carveHorizontal(0, 10, 5);
        assertEquals(Tile.FLOOR, grid[5][5], "Floor must not be overwritten");
    }

    @Test
    void placeDoorOnCorridor() {
        grid[3][5] = Tile.CORRIDOR;
        builder.placeDoorNear(4, 5);
        assertEquals(Tile.DOOR, grid[5][3], "Door should be placed on adjacent corridor");
    }

    @Test
    void connectTwoRooms() {
        for (int y = 1; y < 4; y++) {
            for (int x = 1; x < 4; x++) {
                grid[y][x] = Tile.FLOOR;
            }
        }
        for (int y = 6; y < 9; y++) {
            for (int x = 6; x < 9; x++) {
                grid[y][x] = Tile.FLOOR;
            }
        }
        builder.connect(2, 2, 7, 7);

        boolean connected = false;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int y = 1; y < 9; y++) {
            for (int x = 1; x < 9; x++) {
                if (grid[y][x] == Tile.CORRIDOR) {
                    for (int[] d : dirs) {
                        int nx = x + d[0], ny = y + d[1];
                        if (inBounds(nx, ny) && grid[ny][nx] == Tile.FLOOR) {
                            connected = true;
                        }
                    }
                }
            }
        }
        assertTrue(connected, "Corridor must connect to floor tiles");
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }
}
