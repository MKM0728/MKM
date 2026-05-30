package com.roguelike.core;

public enum GameState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER;

    public boolean isPlaying() { return this == PLAYING; }
    public boolean isPaused() { return this == PAUSED; }
}
