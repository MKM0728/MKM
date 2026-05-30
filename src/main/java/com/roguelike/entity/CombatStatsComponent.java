package com.roguelike.entity;

import com.roguelike.core.Component;

public final class CombatStatsComponent implements Component {
    private final int atk;
    private final int def;
    private final int spd;

    public CombatStatsComponent(int atk, int def, int spd) {
        this.atk = atk;
        this.def = def;
        this.spd = spd;
    }

    public int atk() { return atk; }
    public int def() { return def; }
    public int spd() { return spd; }

    public int initiativeRoll() {
        return spd + (int) (Math.random() * 10);
    }
}
