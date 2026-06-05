package com.roguelike.map;

import java.util.Random;

public class CorridorBuilder {

    private final Tile[][] grid;
    private final int width;
    private final int height;
    private final Random rng;

    public CorridorBuilder(Tile[][] grid, Random rng) {
        this.grid = grid;
        this.height = grid.length;
        this.width = height > 0 ? grid[0].length : 0;
        this.rng = rng;
    }

    public void connect(int x1, int y1, int x2, int y2) {
        if (rng.nextBoolean()) {
            carveHorizontal(x1, x2, y1);
            carveVertical(y1, y2, x2);
        } else {
            carveVertical(y1, y2, x1);
            carveHorizontal(x1, x2, y2);
        }
    }

    public void connectWithDoor(int roomCx, int roomCy, int targetX, int targetY) {
        connect(roomCx, roomCy, targetX, targetY);
        placeDoorNear(roomCx, roomCy);
    }

    public void carveHorizontal(int x1, int x2, int y) {
        int from = Math.min(x1, x2);
        int to = Math.max(x1, x2);
        for (int x = from; x <= to; x++) {
            if (inBounds(x, y) && grid[y][x] == Tile.WALL) {
                grid[y][x] = Tile.CORRIDOR;
            }
        }
    }

    public void carveVertical(int y1, int y2, int x) {
        int from = Math.min(y1, y2);
        int to = Math.max(y1, y2);
        for (int y = from; y <= to; y++) {
            if (inBounds(x, y) && grid[y][x] == Tile.WALL) {
                grid[y][x] = Tile.CORRIDOR;
            }
        }
    }

    public void placeDoorNear(int cx, int cy) {
        int[][] candidates = {{cx-1, cy}, {cx+1, cy}, {cx, cy-1}, {cx, cy+1}};
        for (int[] pos : candidates) {
            int x = pos[0], y = pos[1];
            if (inBounds(x, y) && grid[y][x] == Tile.CORRIDOR) {
                grid[y][x] = Tile.DOOR;
                return;
            }
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
