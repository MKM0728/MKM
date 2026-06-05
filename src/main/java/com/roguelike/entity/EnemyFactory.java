package com.roguelike.entity;

import com.roguelike.core.Entity;

public final class EnemyFactory {

    private EnemyFactory() {}

    public static Entity create(EnemyType type, int x, int y) {
        var entity = new Entity();
        entity.add(new PositionComponent(x, y));
        entity.add(new RenderComponent(type.name().toLowerCase()));
        entity.add(new HealthComponent(type.baseHp(), type.maxBaseHp()));
        entity.add(new CombatStatsComponent(type.baseAtk(), type.baseDef(), type.baseSpd()));
        entity.add(new EnemyComponent(type));
        return entity;
    }
}
