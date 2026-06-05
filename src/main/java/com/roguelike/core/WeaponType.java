package com.roguelike.core;

public enum WeaponType {
    FISTS("Fists", 5),
    SICKLE("Sickle", 10),
    SWORD("Sword", 15),
    AXE("Axe", 30);

    private final String label;
    private final int damage;

    WeaponType(String label, int damage) {
        this.label = label;
        this.damage = damage;
    }

    public String label() { return label; }
    public int damage() { return damage; }
}
