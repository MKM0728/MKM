package com.roguelike.map;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class BspNodeTest {

    private final Random rng = new Random(42);

    @Test
    void splitCreatesChildren() {
        var node = new BspNode(0, 0, 30, 30);
        boolean split = node.split(rng);
        assertTrue(split);
        assertNotNull(node.left);
        assertNotNull(node.right);
    }

    @Test
    void tooSmallDoesNotSplit() {
        var node = new BspNode(0, 0, 5, 5);
        boolean split = node.split(rng);
        assertFalse(split);
    }

    @Test
    void splitRecursiveCreatesLeaves() {
        var root = new BspNode(0, 0, 50, 50);
        root.splitRecursive(rng, 4);
        var leaves = root.leaves();
        assertTrue(leaves.size() >= 4);
        for (BspNode leaf : leaves) {
            assertNull(leaf.left);
            assertNull(leaf.right);
        }
    }

    @Test
    void carveRoomPlacesWithinBounds() {
        var node = new BspNode(5, 5, 20, 20);
        node.splitRecursive(rng, 2);
        for (BspNode leaf : node.leaves()) {
            assertTrue(leaf.carveRoom(rng));
            assertNotNull(leaf.room);
            assertTrue(leaf.room.width() >= 5);
            assertTrue(leaf.room.height() >= 5);
            assertTrue(leaf.room.x() >= leaf.x);
            assertTrue(leaf.room.y() >= leaf.y);
        }
    }

    @Test
    void leavesChildrenCoverFullArea() {
        var root = new BspNode(0, 0, 40, 40);
        root.splitRecursive(rng, 3);
        int totalArea = 0;
        for (BspNode leaf : root.leaves()) {
            totalArea += leaf.width * leaf.height;
        }
        assertEquals(1600, totalArea);
    }
}
