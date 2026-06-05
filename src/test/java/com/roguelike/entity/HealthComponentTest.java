package com.roguelike.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HealthComponentTest {

    @Test
    void aliveByDefault() {
        var h = new HealthComponent(100, 100);
        assertTrue(h.isAlive());
        assertEquals(100, h.hp());
    }

    @Test
    void takeDamage() {
        var h = new HealthComponent(100, 100);
        int dmg = h.takeDamage(30);
        assertEquals(30, dmg);
        assertEquals(70, h.hp());
    }

    @Test
    void damageCantKillBeyondZero() {
        var h = new HealthComponent(10, 100);
        int dmg = h.takeDamage(50);
        assertEquals(10, dmg);
        assertEquals(0, h.hp());
        assertFalse(h.isAlive());
    }

    @Test
    void healRestoresHp() {
        var h = new HealthComponent(50, 100);
        int healed = h.heal(30);
        assertEquals(30, healed);
        assertEquals(80, h.hp());
    }

    @Test
    void healCantExceedMax() {
        var h = new HealthComponent(90, 100);
        int healed = h.heal(30);
        assertEquals(10, healed);
        assertEquals(100, h.hp());
    }

    @Test
    void hpRatio() {
        var h = new HealthComponent(75, 100);
        assertEquals(0.75f, h.hpRatio(), 0.01f);
    }
}
