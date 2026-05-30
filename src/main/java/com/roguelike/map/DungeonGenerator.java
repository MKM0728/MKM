package com.roguelike.map;

import java.util.List;
import java.util.Random;

public class DungeonGenerator {

    private final int width;
    private final int height;
    private final Random rng;
    private final Tile[][] grid;
    private int stairsDownX, stairsDownY;
    private int stairsUpX, stairsUpY;
    private Room startRoom;

    public DungeonGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.rng = new Random(seed);
        this.grid = new Tile[height][width];
    }

    public Tile[][] generate(int bspDepth) {
        clearGrid();

        var root = new BspNode(1, 1, width - 2, height - 2);
        root.splitRecursive(rng, bspDepth);

        var leaves = root.leaves();
        carveRooms(leaves);
        connectSiblings(root);

        carveBorderWalls();
        return grid;
    }

    private void clearGrid() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = Tile.WALL;
            }
        }
    }

    private void carveRooms(List<BspNode> leaves) {
        for (BspNode leaf : leaves) {
            if (leaf.carveRoom(rng) && startRoom == null) {
                startRoom = leaf.room;
            }
            if (leaf.room != null) {
                for (int y = leaf.room.y(); y < leaf.room.y() + leaf.room.height(); y++) {
                    for (int x = leaf.room.x(); x < leaf.room.x() + leaf.room.width(); x++) {
                        if (y >= 0 && y < height && x >= 0 && x < width) {
                            grid[y][x] = Tile.FLOOR;
                        }
                    }
                }
            }
        }

        if (startRoom != null) {
            stairsUpX = startRoom.centerX();
            stairsUpY = startRoom.centerY();
            grid[stairsUpY][stairsUpX] = Tile.STAIRS_UP;
        }

        if (!leaves.isEmpty()) {
            var lastRoom = leaves.get(leaves.size() - 1).room;
            if (lastRoom != null) {
                stairsDownX = lastRoom.centerX();
                stairsDownY = lastRoom.centerY();
                grid[stairsDownY][stairsDownX] = Tile.STAIRS_DOWN;
            }
        }
    }

    private void connectSiblings(BspNode node) {
        if (node.left == null || node.right == null) return;

        connectSiblings(node.left);
        connectSiblings(node.right);

        Room leftRoom = findRoom(node.left);
        Room rightRoom = findRoom(node.right);

        if (leftRoom != null && rightRoom != null) {
            carveLCorridor(leftRoom.centerX(), leftRoom.centerY(),
                           rightRoom.centerX(), rightRoom.centerY());
        }
    }

    private Room findRoom(BspNode node) {
        if (node.room != null) return node.room;
        if (node.left != null) {
            Room r = findRoom(node.left);
            if (r != null) return r;
        }
        if (node.right != null) {
            return findRoom(node.right);
        }
        return null;
    }

    private void carveLCorridor(int x1, int y1, int x2, int y2) {
        if (rng.nextBoolean()) {
            carveHorizontal(x1, x2, y1);
            carveVertical(y1, y2, x2);
        } else {
            carveVertical(y1, y2, x1);
            carveHorizontal(x1, x2, y2);
        }
    }

    private void carveHorizontal(int x1, int x2, int y) {
        int from = Math.min(x1, x2);
        int to = Math.max(x1, x2);
        for (int x = from; x <= to; x++) {
            if (y >= 0 && y < height && x >= 0 && x < width && grid[y][x] == Tile.WALL) {
                grid[y][x] = Tile.CORRIDOR;
            }
        }
    }

    private void carveVertical(int y1, int y2, int x) {
        int from = Math.min(y1, y2);
        int to = Math.max(y1, y2);
        for (int y = from; y <= to; y++) {
            if (y >= 0 && y < height && x >= 0 && x < width && grid[y][x] == Tile.WALL) {
                grid[y][x] = Tile.CORRIDOR;
            }
        }
    }

    private void carveBorderWalls() {
        for (int x = 0; x < width; x++) {
            grid[0][x] = Tile.WALL;
            grid[height - 1][x] = Tile.WALL;
        }
        for (int y = 0; y < height; y++) {
            grid[y][0] = Tile.WALL;
            grid[y][width - 1] = Tile.WALL;
        }
    }

    public int stairsDownX() { return stairsDownX; }
    public int stairsDownY() { return stairsDownY; }
    public int stairsUpX() { return stairsUpX; }
    public int stairsUpY() { return stairsUpY; }
    public Room startRoom() { return startRoom; }
}
