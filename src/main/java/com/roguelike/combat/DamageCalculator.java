package com.roguelike.combat;

import com.roguelike.entity.CombatStatsComponent;

public final class DamageCalculator {

    private DamageCalculator() {}

    public static int calculate(int attackerAtk, int defenderDef, DamageType type) {
        int raw = attackerAtk - defenderDef;
        raw = switch (type) {
            case FIRE -> (int) (raw * 1.5);
            case POISON -> (int) (raw * 0.8);
            default -> raw;
        };
        return Math.max(1, raw);
    }

    public static int calculate(CombatStatsComponent attacker, CombatStatsComponent defender, DamageType type) {
        return calculate(attacker.atk(), defender.def(), type);
    }

    public static int physicalDamage(int atk, int def) {
        return calculate(atk, def, DamageType.PHYSICAL);
    }
}
