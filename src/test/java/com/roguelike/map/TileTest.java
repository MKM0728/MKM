package com.roguelike.map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TileTest {

    @Test
    void walkableTiles() {
        assertTrue(Tile.FLOOR.isWalkable());
        assertTrue(Tile.CORRIDOR.isWalkable());
        assertTrue(Tile.STAIRS_DOWN.isWalkable());
        assertTrue(Tile.STAIRS_UP.isWalkable());
    }

    @Test
    void nonWalkableTiles() {
        assertFalse(Tile.WALL.isWalkable());
        assertFalse(Tile.VOID.isWalkable());
        assertFalse(Tile.WATER.isWalkable());
    }

    @Test
    void blocksVision() {
        assertTrue(Tile.WALL.isBlockingVision());
        assertFalse(Tile.FLOOR.isBlockingVision());
    }

    @Test
    void doorIsOpenable() {
        assertTrue(Tile.DOOR.isOpenable());
        assertFalse(Tile.WALL.isOpenable());
    }

    @Test
    void glyphRoundTrip() {
        for (Tile t : Tile.values()) {
            assertEquals(t, Tile.fromGlyph(t.glyph()));
        }
    }

    @Test
    void unknownGlyphReturnsVoid() {
        assertEquals(Tile.VOID, Tile.fromGlyph('?'));
    }
}
