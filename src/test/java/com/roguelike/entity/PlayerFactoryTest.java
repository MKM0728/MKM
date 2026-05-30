package com.roguelike.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlayerFactoryTest {

    @Test
    void createsPlayerWithAllComponents() {
        var player = PlayerFactory.create(5, 10);

        assertTrue(player.has(PositionComponent.class));
        assertTrue(player.has(RenderComponent.class));
        assertTrue(player.has(HealthComponent.class));
        assertTrue(player.has(CombatStatsComponent.class));
        assertTrue(player.has(PlayerComponent.class));

        var pos = player.get(PositionComponent.class);
        assertEquals(5, pos.x());
        assertEquals(10, pos.y());

        var health = player.get(HealthComponent.class);
        assertEquals(100, health.maxHp());
        assertTrue(health.isAlive());
    }
}
