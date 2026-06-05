package com.roguelike.combat;

import com.roguelike.core.Entity;
import com.roguelike.entity.CombatStatsComponent;
import com.roguelike.entity.HealthComponent;
import com.roguelike.entity.PositionComponent;

public final class AttackSystem {

    private static final int MELEE_RANGE = 1;
    private static final int RANGED_RANGE = 5;

    private AttackSystem() {}

    public static CombatEvent meleeAttack(Entity attacker, Entity defender) {
        return attack(attacker, defender, DamageType.PHYSICAL, MELEE_RANGE);
    }

    public static CombatEvent rangedAttack(Entity attacker, Entity defender) {
        return attack(attacker, defender, DamageType.PHYSICAL, RANGED_RANGE);
    }

    public static CombatEvent specialAttack(Entity attacker, Entity defender, DamageType type) {
        return attack(attacker, defender, type, MELEE_RANGE);
    }

    private static CombatEvent attack(Entity attacker, Entity defender, DamageType type, int maxRange) {
        var atkStats = attacker.get(CombatStatsComponent.class);
        var defStats = defender.get(CombatStatsComponent.class);
        var defHealth = defender.get(HealthComponent.class);
        var atkPos = attacker.get(PositionComponent.class);
        var defPos = defender.get(PositionComponent.class);

        var atkName = attacker.toString();
        var defName = defender.toString();

        if (atkStats == null || defHealth == null) {
            return new CombatEvent(atkName, defName, 0, type, false);
        }

        if (atkPos != null && defPos != null && atkPos.distanceTo(defPos) > maxRange) {
            return new CombatEvent(atkName, defName, 0, type, false);
        }

        int def = defStats != null ? defStats.def() : 0;
        int dmg = DamageCalculator.calculate(atkStats.atk(), def, type);
        defHealth.takeDamage(dmg);

        return new CombatEvent(atkName, defName, dmg, type, !defHealth.isAlive());
    }

    public static boolean canMeleeAttack(Entity attacker, Entity defender) {
        var ap = attacker.get(PositionComponent.class);
        var dp = defender.get(PositionComponent.class);
        if (ap == null || dp == null) return false;
        return ap.distanceTo(dp) <= MELEE_RANGE;
    }
}
