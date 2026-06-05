package com.roguelike.entity;

import com.roguelike.core.Component;

public final class EnemyComponent implements Component {
    private final EnemyType type;
    public EnemyComponent(EnemyType type) { this.type = type; }
    public EnemyType type() { return type; }
    public int xpValue() { return type.xpValue(); }
}
