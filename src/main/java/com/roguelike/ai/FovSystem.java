package com.roguelike.ai;

import com.roguelike.map.Tile;

public class FovSystem {

    private final int radius;
    private boolean[][] visible;
    private int width;
    private int height;

    public FovSystem(int radius) {
        this.radius = radius;
    }

    public boolean[][] compute(Tile[][] grid, int originX, int originY) {
        this.height = grid.length;
        this.width = height > 0 ? grid[0].length : 0;
        this.visible = new boolean[height][width];

        visible[originY][originX] = true;

        for (int angle = 0; angle < 360; angle++) {
            castRay(grid, originX, originY, Math.toRadians(angle));
        }

        return visible;
    }

    private void castRay(Tile[][] grid, int ox, int oy, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = ox + 0.5;
        double y = oy + 0.5;

        for (int step = 0; step < radius; step++) {
            x += cos;
            y += sin;

            int tileX = (int) Math.floor(x);
            int tileY = (int) Math.floor(y);

            if (!inBounds(tileX, tileY)) break;

            visible[tileY][tileX] = true;

            if (grid[tileY][tileX].isBlockingVision()) break;
        }
    }

    public boolean isVisible(int x, int y) {
        if (!inBounds(x, y)) return false;
        return visible[y][x];
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean[][] lastResult() { return visible; }
}
