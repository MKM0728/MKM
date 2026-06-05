package com.roguelike.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EnemyFactoryTest {

    @Test
    void createsAllTypes() {
        for (var type : EnemyType.values()) {
            var enemy = EnemyFactory.create(type, 3, 7);
            assertTrue(enemy.has(PositionComponent.class));
            assertTrue(enemy.has(HealthComponent.class));
            assertTrue(enemy.has(CombatStatsComponent.class));
            assertTrue(enemy.has(EnemyComponent.class));
            assertEquals(type, enemy.get(EnemyComponent.class).type());
        }
    }

    @Test
    void batStatsAreCorrect() {
        var bat = EnemyFactory.create(EnemyType.BAT, 0, 0);
        var hp = bat.get(HealthComponent.class);
        assertEquals(30, hp.hp());
        assertEquals(30, hp.maxHp());
        var cs = bat.get(CombatStatsComponent.class);
        assertEquals(5, cs.atk());
    }

    @Test
    void skeletonStatsAreCorrect() {
        var skel = EnemyFactory.create(EnemyType.SKELETON, 0, 0);
        var hp = skel.get(HealthComponent.class);
        assertEquals(60, hp.hp());
        assertEquals(60, hp.maxHp());
        var cs = skel.get(CombatStatsComponent.class);
        assertEquals(10, cs.atk());
    }
}
