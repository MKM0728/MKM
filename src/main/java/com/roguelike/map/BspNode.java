package com.roguelike.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BspNode {

    private static final int MIN_SIZE = 7;
    private static final int MIN_ROOM_SIZE = 5;

    public final int x, y, width, height;
    public BspNode left, right;
    public Room room;

    public BspNode(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean split(Random rng) {
        if (left != null || right != null) return false;

        boolean splitH = rng.nextBoolean();
        int maxSplit = (splitH ? height : width) - MIN_SIZE;

        if (maxSplit < MIN_SIZE) return false;

        int splitAt = MIN_SIZE + rng.nextInt(maxSplit - MIN_SIZE + 1);

        if (splitH) {
            left = new BspNode(x, y, width, splitAt);
            right = new BspNode(x, y + splitAt, width, height - splitAt);
        } else {
            left = new BspNode(x, y, splitAt, height);
            right = new BspNode(x + splitAt, y, width - splitAt, height);
        }
        return true;
    }

    public void splitRecursive(Random rng, int depth) {
        if (depth <= 0) return;
        if (split(rng)) {
            left.splitRecursive(rng, depth - 1);
            right.splitRecursive(rng, depth - 1);
        }
    }

    public List<BspNode> leaves() {
        var result = new ArrayList<BspNode>();
        if (left == null && right == null) {
            result.add(this);
        } else {
            if (left != null) result.addAll(left.leaves());
            if (right != null) result.addAll(right.leaves());
        }
        return result;
    }

    public boolean carveRoom(Random rng) {
        if (left != null || right != null) return false;
        int rw = MIN_ROOM_SIZE + rng.nextInt(Math.max(1, width - MIN_ROOM_SIZE));
        int rh = MIN_ROOM_SIZE + rng.nextInt(Math.max(1, height - MIN_ROOM_SIZE));
        int rx = x + rng.nextInt(Math.max(1, width - rw));
        int ry = y + rng.nextInt(Math.max(1, height - rh));
        room = new Room(rx, ry, rw, rh);
        return true;
    }
}
