package com.roguelike.entity;

public enum EnemyType {
    SLIME(15, 30, 5, 1, 4, 5),
    SKELETON(25, 45, 8, 3, 6, 12),
    BAT(10, 20, 6, 0, 9, 8);

    private final int baseHp, maxBaseHp, baseAtk, baseDef, baseSpd, xpValue;

    EnemyType(int hp, int maxHp, int atk, int def, int spd, int xp) {
        this.baseHp = hp; this.maxBaseHp = maxHp; this.baseAtk = atk;
        this.baseDef = def; this.baseSpd = spd; this.xpValue = xp;
    }

    public int baseHp() { return baseHp; }
    public int maxBaseHp() { return maxBaseHp; }
    public int baseAtk() { return baseAtk; }
    public int baseDef() { return baseDef; }
    public int baseSpd() { return baseSpd; }
    public int xpValue() { return xpValue; }
}
