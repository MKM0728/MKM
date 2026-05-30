package com.roguelike.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ItemFactoryTest {

    private final Random rng = new Random(42);

    @Test
    void weaponHasCorrectType() {
        var item = ItemFactory.createWeapon(3, 5, rng);
        var ic = item.get(ItemComponent.class);
        assertEquals(ItemType.WEAPON, ic.itemType());
        assertTrue(ic.value() > 0);
    }

    @Test
    void potionHasCorrectType() {
        var item = ItemFactory.createPotion(1, 1, rng);
        assertEquals(ItemType.POTION, item.get(ItemComponent.class).itemType());
    }

    @Test
    void scrollHasCorrectType() {
        var item = ItemFactory.createScroll(7, 2, rng);
        assertEquals(ItemType.SCROLL, item.get(ItemComponent.class).itemType());
    }

    @Test
    void itemHasPositionAndRender() {
        var item = ItemFactory.createPotion(5, 9, rng);
        assertTrue(item.has(PositionComponent.class));
        assertTrue(item.has(RenderComponent.class));
        assertEquals(5, item.get(PositionComponent.class).x());
    }
}
