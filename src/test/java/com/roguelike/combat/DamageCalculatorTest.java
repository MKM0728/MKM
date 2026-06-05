package com.roguelike.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.roguelike.entity.CombatStatsComponent;

import org.junit.jupiter.api.Test;

class DamageCalculatorTest {

    @Test
    void physicalDamageAtkMinusDef() {
        int dmg = DamageCalculator.calculate(10, 3, DamageType.PHYSICAL);
        assertEquals(7, dmg);
    }

    @Test
    void minimumDamageIsOne() {
        int dmg = DamageCalculator.calculate(3, 10, DamageType.PHYSICAL);
        assertEquals(1, dmg);
    }

    @Test
    void fireDealsBonusDamage() {
        int dmg = DamageCalculator.calculate(10, 0, DamageType.FIRE);
        assertEquals(15, dmg);
    }

    @Test
    void poisonDealsReducedDamage() {
        int dmg = DamageCalculator.calculate(10, 0, DamageType.POISON);
        assertEquals(8, dmg);
    }

    @Test
    void calculateWithComponents() {
        var atk = new CombatStatsComponent(12, 0, 5);
        var def = new CombatStatsComponent(5, 4, 3);
        int dmg = DamageCalculator.calculate(atk, def, DamageType.PHYSICAL);
        assertEquals(8, dmg);
    }

    @Test
    void combatEventRecord() {
        var evt = CombatEvent.attack("Slime", "Player", 5, DamageType.PHYSICAL, false);
        assertEquals("Slime", evt.attackerName());
        assertEquals(5, evt.damage());
        assertFalse(evt.killed());
    }
}
