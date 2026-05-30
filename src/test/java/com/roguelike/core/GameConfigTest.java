package com.roguelike.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameConfigTest {

    @Test
    void tileSizeIsPositive() {
        assertTrue(GameConfig.TILE_SIZE > 0);
    }

    @Test
    void screenDividesEvenlyIntoMap() {
        assertEquals(30, GameConfig.MAP_WIDTH);
        assertEquals(20, GameConfig.MAP_HEIGHT);
    }
}
