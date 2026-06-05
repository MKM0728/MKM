package com.roguelike.entity;

import com.roguelike.core.Entity;

import java.util.Random;

public final class ItemFactory {

    private static final String[] WEAPONS = {"Rusty Sword", "Iron Axe", "Flame Staff"};
    private static final String[] POTIONS = {"Health Potion", "Mana Vial", "Elixir"};
    private static final String[] SCROLLS = {"Fire Scroll", "Poison Scroll", "Teleport Scroll"};

    private ItemFactory() {}

    public static Entity createWeapon(int x, int y, Random rng) {
        return create(ItemType.WEAPON, pick(WEAPONS, rng), 5 + rng.nextInt(10), x, y);
    }

    public static Entity createPotion(int x, int y, Random rng) {
        return create(ItemType.POTION, pick(POTIONS, rng), 10 + rng.nextInt(20), x, y);
    }

    public static Entity createScroll(int x, int y, Random rng) {
        return create(ItemType.SCROLL, pick(SCROLLS, rng), 8 + rng.nextInt(12), x, y);
    }

    private static Entity create(ItemType type, String name, int value, int x, int y) {
        var entity = new Entity();
        entity.add(new PositionComponent(x, y));
        entity.add(new RenderComponent(type.name().toLowerCase()));
        entity.add(new ItemComponent(type, name, value));
        return entity;
    }

    private static String pick(String[] arr, Random rng) {
        return arr[rng.nextInt(arr.length)];
    }
}
