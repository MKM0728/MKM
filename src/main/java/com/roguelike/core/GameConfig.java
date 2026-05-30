package com.roguelike.core;

public final class GameConfig {
    private GameConfig() {}

    public static final String GAME_TITLE = "Roguelike Dungeon";

    public static final int SCREEN_WIDTH = 960;
    public static final int SCREEN_HEIGHT = 640;

    public static final int TILE_SIZE = 32;
    public static final int MAP_WIDTH = 80;
    public static final int MAP_HEIGHT = 50;

    public static final int MIN_ROOM_SIZE = 5;
    public static final int MIN_AREA_SIZE = 7;
    public static final int MAX_BSP_DEPTH = 5;

    public static final int PLAYER_BASE_HP = 100;
    public static final int PLAYER_BASE_ATK = 8;
    public static final int PLAYER_BASE_DEF = 0;
    public static final int PLAYER_BASE_SPD = 10;

    public static final int MAX_FLOORS = 20;
    public static final int FOV_RADIUS = 8;
    public static final int ENEMY_COUNT = 10;
}
