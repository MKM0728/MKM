package com.roguelike.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.roguelike.core.Entity;
import com.roguelike.entity.CombatStatsComponent;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TurnManagerTest {

    private TurnManager tm;
    private Entity a, b;

    @BeforeEach
    void setUp() {
        tm = new TurnManager();
        a = new Entity();
        a.add(new CombatStatsComponent(5, 3, 10));
        b = new Entity();
        b.add(new CombatStatsComponent(3, 5, 5));
        tm.init(List.of(a, b));
    }

    @Test
    void initCreatesQueue() {
        assertEquals(2, tm.size());
    }

    @Test
    void currentReturnsEntity() {
        assertNotNull(tm.current());
    }

    @Test
    void nextAdvances() {
        var first = tm.current();
        var second = tm.next();
        assertNotEquals(first.id(), second.id());
    }

    @Test
    void fullCycleIncrementsRound() {
        tm.next();
        assertEquals(0, tm.round());
        tm.next();
        assertEquals(1, tm.round());
    }

    @Test
    void removeEntity() {
        tm.remove(a);
        assertEquals(1, tm.size());
        assertTrue(tm.queue().contains(b));
    }

    @Test
    void roundStartsAtZero() {
        assertEquals(0, tm.round());
    }
}
