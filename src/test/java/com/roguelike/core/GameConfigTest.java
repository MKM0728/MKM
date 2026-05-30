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
        assertTrue(GameConfig.MAP_WIDTH > 0);
        assertTrue(GameConfig.MAP_HEIGHT > 0);
    }
}
