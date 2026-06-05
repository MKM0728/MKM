package com.roguelike.core;

public final class GameConfig {
    private GameConfig() {}

    public static final String GAME_TITLE = "Roguelike Dungeon";

    public static final int SCREEN_WIDTH = 960;
    public static final int SCREEN_HEIGHT = 640;

    public static final int TILE_SIZE = 32;
    public static final int BASE_MAP_WIDTH = 30;
    public static final int BASE_MAP_HEIGHT = 20;

    public static int mapWidth(int floor) { return BASE_MAP_WIDTH * (1 << (floor - 1)); }
    public static int mapHeight(int floor) { return BASE_MAP_HEIGHT * (1 << (floor - 1)); }

    public static final int MIN_ROOM_SIZE = 5;
    public static final int MIN_AREA_SIZE = 7;
    public static final int MAX_BSP_DEPTH = 5;

    public static final int PLAYER_BASE_HP = 100;
    public static final int PLAYER_BASE_ATK = 5;
    public static final int PLAYER_BASE_DEF = 0;
    public static final int PLAYER_BASE_SPD = 10;

    public static final int MAX_FLOORS = 3;
    public static final int FOV_RADIUS = 8;

    public static int enemyCount(int floor) { return 6 + (int)(6 * 0.5 * (floor - 1)); }
    public static int ghostCount() { return 1; }
}
