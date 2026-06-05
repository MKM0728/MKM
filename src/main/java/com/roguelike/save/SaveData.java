package com.roguelike.save;

import java.util.List;

public class SaveData {
    public int version = 2;
    public long timestamp;
    public int playerX, playerY, playerHp, playerMaxHp, playerAtk, playerDef, playerSpd;
    public int floor;
    public long seed;
    public int turns, enemiesSlain;
    public String equippedWeapon;
    public List<ItemEntry> inventory;
    public List<EnemyEntry> enemies;
    public List<WeaponEntry> groundWeapons;
    public boolean ghostAlive;
    public int ghostRoomX, ghostRoomY, ghostRoomW, ghostRoomH;
    public boolean ghostRoomLocked;

    public static class ItemEntry {
        public String type, name;
        public int value;
    }

    public static class EnemyEntry {
        public String type;
        public int x, y, hp;
        public boolean alive;
    }

    public static class WeaponEntry {
        public String type;
        public int x, y;
    }
}
