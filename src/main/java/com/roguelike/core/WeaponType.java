package com.roguelike.core;

public enum WeaponType {
    FISTS("Fists", 15),
    SICKLE("Sickle", 20),
    SWORD("Sword", 25),
    AXE("Axe", 40);

    private final String label;
    private final int damage;

    WeaponType(String label, int damage) {
        this.label = label;
        this.damage = damage;
    }

    public String label() { return label; }
    public int damage() { return damage; }
}
