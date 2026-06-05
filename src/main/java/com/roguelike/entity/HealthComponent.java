package com.roguelike.entity;

import com.roguelike.core.Component;

public final class HealthComponent implements Component {
    private int hp;
    private final int maxHp;

    public HealthComponent(int hp, int maxHp) {
        this.hp = Math.min(hp, maxHp);
        this.maxHp = maxHp;
    }

    public int hp() { return hp; }
    public int maxHp() { return maxHp; }
    public boolean isAlive() { return hp > 0; }

    public int takeDamage(int amount) {
        int actual = Math.min(amount, hp);
        hp -= actual;
        return actual;
    }

    public int heal(int amount) {
        int maxHeal = maxHp - hp;
        int actual = Math.min(amount, maxHeal);
        hp += actual;
        return actual;
    }

    public float hpRatio() { return (float) hp / maxHp; }
}
