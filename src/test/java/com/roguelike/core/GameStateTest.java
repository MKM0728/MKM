package com.roguelike.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameStateTest {

    @Test
    void fourStatesExist() {
        assertEquals(4, GameState.values().length);
    }

    @Test
    void playingIsTrueOnlyForPlaying() {
        assertTrue(GameState.PLAYING.isPlaying());
        assertFalse(GameState.MENU.isPlaying());
        assertFalse(GameState.PAUSED.isPlaying());
        assertFalse(GameState.GAME_OVER.isPlaying());
    }
}
