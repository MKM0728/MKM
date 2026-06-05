package com.roguelike.map;

import java.util.Map;
import java.util.Set;

public enum Tile {
    VOID(' ', false, false, false),
    WALL('#', false, true, false),
    FLOOR('.', true, false, false),
    CORRIDOR(',', true, false, false),
    DOOR('+', true, true, true),
    STAIRS_DOWN('>', true, false, false),
    STAIRS_UP('<', true, false, false),
    WATER('~', false, false, false);

    private final char glyph;
    private final boolean walkable;
    private final boolean blocksVision;
    private final boolean openable;

    private static final Map<Character, Tile> GLYPH_MAP = Map.ofEntries(
        Map.entry(' ', VOID),
        Map.entry('#', WALL),
        Map.entry('.', FLOOR),
        Map.entry(',', CORRIDOR),
        Map.entry('+', DOOR),
        Map.entry('>', STAIRS_DOWN),
        Map.entry('<', STAIRS_UP),
        Map.entry('~', WATER)
    );

    private static final Set<Tile> WALKABLE_TILES = Set.of(FLOOR, CORRIDOR, STAIRS_DOWN, STAIRS_UP);
    private static final Set<Tile> TRANSPARENT_TILES = Set.of(FLOOR, CORRIDOR, STAIRS_DOWN, STAIRS_UP, DOOR, WATER);

    Tile(char glyph, boolean walkable, boolean blocksVision, boolean openable) {
        this.glyph = glyph;
        this.walkable = walkable;
        this.blocksVision = blocksVision;
        this.openable = openable;
    }

    public char glyph() { return glyph; }

    public boolean isWalkable() { return walkable; }

    public boolean isBlockingVision() { return blocksVision; }

    public boolean isOpenable() { return openable; }

    public static Tile fromGlyph(char c) { return GLYPH_MAP.getOrDefault(c, VOID); }

    public static boolean isWalkable(Tile t) { return WALKABLE_TILES.contains(t); }

    public static boolean isTransparent(Tile t) { return TRANSPARENT_TILES.contains(t); }
}
