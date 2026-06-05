package com.roguelike.save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguelike.core.Entity;
import com.roguelike.entity.CombatStatsComponent;
import com.roguelike.entity.HealthComponent;
import com.roguelike.entity.PositionComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class SaveManager {

    private static final Path SAVE_DIR = Paths.get(java.lang.System.getProperty("user.home"), ".roguelike-dungeon");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save(Entity player, int floor, long seed, int turns, int enemiesSlain,
                            String equippedWeapon, java.util.List<SaveData.EnemyEntry> enemies,
                            java.util.List<SaveData.WeaponEntry> groundWeapons,
                            boolean ghostAlive, int ghostRoomX, int ghostRoomY, int ghostRoomW, int ghostRoomH,
                            boolean ghostRoomLocked, String slot) throws IOException {
        Files.createDirectories(SAVE_DIR);
        var sd = new SaveData();
        sd.timestamp = java.lang.System.currentTimeMillis();
        sd.floor = floor;
        sd.seed = seed;
        sd.turns = turns;
        sd.enemiesSlain = enemiesSlain;
        sd.equippedWeapon = equippedWeapon;
        sd.enemies = enemies;
        sd.groundWeapons = groundWeapons;
        sd.ghostAlive = ghostAlive;
        sd.ghostRoomX = ghostRoomX;
        sd.ghostRoomY = ghostRoomY;
        sd.ghostRoomW = ghostRoomW;
        sd.ghostRoomH = ghostRoomH;
        sd.ghostRoomLocked = ghostRoomLocked;

        var pos = player.get(PositionComponent.class);
        var hp = player.get(HealthComponent.class);
        var stats = player.get(CombatStatsComponent.class);

        sd.playerX = pos.x(); sd.playerY = pos.y();
        sd.playerHp = hp.hp(); sd.playerMaxHp = hp.maxHp();
        sd.playerAtk = stats.atk(); sd.playerDef = stats.def(); sd.playerSpd = stats.spd();

        sd.inventory = new ArrayList<>();

        var json = GSON.toJson(sd);
        Files.writeString(SAVE_DIR.resolve(slot + ".json"), json);
    }

    public static SaveData load(String slot) throws IOException {
        var path = SAVE_DIR.resolve(slot + ".json");
        if (!Files.exists(path)) return null;
        return GSON.fromJson(Files.readString(path), SaveData.class);
    }

    public static boolean slotExists(String slot) {
        return Files.exists(SAVE_DIR.resolve(slot + ".json"));
    }

    public static void deleteSlot(String slot) throws IOException {
        var path = SAVE_DIR.resolve(slot + ".json");
        Files.deleteIfExists(path);
    }

    public static Path saveDir() { return SAVE_DIR; }
}
