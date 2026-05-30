package com.roguelike.save;

import java.util.List;

public class SaveData {
    public int version = 1;
    public long timestamp;
    public int playerX, playerY, playerHp, playerMaxHp, playerAtk, playerDef, playerSpd;
    public int floor;
    public long seed;
    public List<ItemEntry> inventory;
    public String equippedWeapon;

    public static class ItemEntry {
        public String type, name;
        public int value;
    }
}
