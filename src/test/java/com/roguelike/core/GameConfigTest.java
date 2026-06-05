package com.roguelike.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameConfigTest {

    @Test
    void tileSizeIsPositive() {
        assertTrue(GameConfig.TILE_SIZE > 0);
    }

    @Test
    void mapDimensionsArePositive() {
        assertTrue(GameConfig.BASE_MAP_WIDTH > 0);
        assertTrue(GameConfig.BASE_MAP_HEIGHT > 0);
    }

    @Test
    void mapSizeScalesWithFloor() {
        assertEquals(30, GameConfig.mapWidth(1));
        assertEquals(60, GameConfig.mapWidth(2));
        assertEquals(120, GameConfig.mapWidth(3));
        assertEquals(20, GameConfig.mapHeight(1));
        assertEquals(40, GameConfig.mapHeight(2));
        assertEquals(80, GameConfig.mapHeight(3));
    }
}
