package com.roguelike.entity;

public enum EnemyType {
    BAT(30, 30, 5, 0, 7, 8, 1.5, "RED", "bat"),
    SKELETON(60, 60, 10, 2, 5, 12, 1.0, "PURPLE", "skeleton"),
    GHOST(120, 120, 15, 0, 6, 25, 0.8, "WHITE", "ghost");

    private final int baseHp, maxBaseHp, baseAtk, baseDef, baseSpd, xpValue;
    private final double moveSpeed;
    private final String color, kind;

    EnemyType(int hp, int maxHp, int atk, int def, int spd, int xp, double speed, String color, String kind) {
        this.baseHp = hp; this.maxBaseHp = maxHp; this.baseAtk = atk;
        this.baseDef = def; this.baseSpd = spd; this.xpValue = xp;
        this.moveSpeed = speed; this.color = color; this.kind = kind;
    }

    public int baseHp() { return baseHp; }
    public int maxBaseHp() { return maxBaseHp; }
    public int baseAtk() { return baseAtk; }
    public int baseDef() { return baseDef; }
    public int baseSpd() { return baseSpd; }
    public int xpValue() { return xpValue; }
    public double moveSpeed() { return moveSpeed; }
    public String color() { return color; }
    public String kind() { return kind; }
}
