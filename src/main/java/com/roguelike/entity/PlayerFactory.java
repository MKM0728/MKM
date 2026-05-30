package com.roguelike.entity;

import com.roguelike.core.Entity;
import com.roguelike.core.GameConfig;

public final class PlayerFactory {

    private PlayerFactory() {}

    public static Entity create(int x, int y) {
        var entity = new Entity();
        entity.add(new PositionComponent(x, y));
        entity.add(new RenderComponent("player"));
        entity.add(new HealthComponent(GameConfig.PLAYER_BASE_HP, GameConfig.PLAYER_BASE_HP));
        entity.add(new CombatStatsComponent(
            GameConfig.PLAYER_BASE_ATK,
            GameConfig.PLAYER_BASE_DEF,
            GameConfig.PLAYER_BASE_SPD
        ));
        entity.add(new PlayerComponent());
        return entity;
    }
}
