package com.roguelike.entity;

import com.roguelike.core.Component;

public final class ItemComponent implements Component {
    private final ItemType itemType;
    private final String name;
    private final int value;

    public ItemComponent(ItemType type, String name, int value) {
        this.itemType = type; this.name = name; this.value = value;
    }

    public ItemType itemType() { return itemType; }
    public String name() { return name; }
    public int value() { return value; }
}
