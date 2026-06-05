package com.roguelike.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CombatStatsComponentTest {

    @Test
    void statsAreSet() {
        var s = new CombatStatsComponent(12, 5, 8);
        assertEquals(12, s.atk());
        assertEquals(5, s.def());
        assertEquals(8, s.spd());
    }

    @Test
    void initiativeRollWithinRange() {
        var s = new CombatStatsComponent(10, 5, 8);
        for (int i = 0; i < 100; i++) {
            int roll = s.initiativeRoll();
            assertTrue(roll >= 8);
            assertTrue(roll < 18);
        }
    }
}
