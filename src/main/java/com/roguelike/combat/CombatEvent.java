package com.roguelike.combat;

public record CombatEvent(
    String attackerName,
    String defenderName,
    int damage,
    DamageType type,
    boolean killed
) {
    public static CombatEvent attack(String from, String to, int dmg, DamageType type, boolean killed) {
        return new CombatEvent(from, to, dmg, type, killed);
    }
}
