package com.roguelike.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.roguelike.core.Entity;
import com.roguelike.entity.CombatStatsComponent;
import com.roguelike.entity.HealthComponent;
import com.roguelike.entity.PositionComponent;
import com.roguelike.entity.PlayerComponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttackSystemTest {

    private Entity player, enemy;

    @BeforeEach
    void setUp() {
        player = new Entity();
        player.add(new PositionComponent(5, 5));
        player.add(new HealthComponent(100, 100));
        player.add(new CombatStatsComponent(10, 3, 8));
        player.add(new PlayerComponent());

        enemy = new Entity();
        enemy.add(new PositionComponent(5, 6)); // adjacent
        enemy.add(new HealthComponent(30, 30));
        enemy.add(new CombatStatsComponent(6, 1, 5));
    }

    @Test
    void meleeAttackDealsDamage() {
        var event = AttackSystem.meleeAttack(player, enemy);
        assertTrue(event.damage() > 0);
        var hp = enemy.get(HealthComponent.class);
        assertEquals(30 - event.damage(), hp.hp());
    }

    @Test
    void attackKillsWhenHpDepleted() {
        enemy.get(HealthComponent.class).takeDamage(25); // reduce to 5 HP
        var event = AttackSystem.meleeAttack(player, enemy);
        assertTrue(event.killed());
    }

    @Test
    void outOfRangeDealsNoDamage() {
        enemy.get(PositionComponent.class).set(20, 20);
        var event = AttackSystem.meleeAttack(player, enemy);
        assertEquals(0, event.damage());
    }

    @Test
    void canMeleeAttackAdjacent() {
        assertTrue(AttackSystem.canMeleeAttack(player, enemy));
    }

    @Test
    void cannotMeleeAttackFar() {
        enemy.get(PositionComponent.class).set(10, 10);
        assertFalse(AttackSystem.canMeleeAttack(player, enemy));
    }

    @Test
    void rangedAttackReachesFurther() {
        enemy.get(PositionComponent.class).set(5, 9); // distance 4
        var event = AttackSystem.rangedAttack(player, enemy);
        assertTrue(event.damage() > 0);
    }
}
