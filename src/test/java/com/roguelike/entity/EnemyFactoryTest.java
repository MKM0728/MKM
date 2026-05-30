package com.roguelike.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EnemyFactoryTest {

    @Test
    void createsAllThreeTypes() {
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
    void slimeStatsAreCorrect() {
        var slime = EnemyFactory.create(EnemyType.SLIME, 0, 0);
        var hp = slime.get(HealthComponent.class);
        assertEquals(10, hp.hp());
        assertEquals(20, hp.maxHp());
    }
}
