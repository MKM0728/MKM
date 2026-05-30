package com.roguelike.save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguelike.core.Entity;
import com.roguelike.entity.CombatStatsComponent;
import com.roguelike.entity.HealthComponent;
import com.roguelike.entity.ItemComponent;
import com.roguelike.entity.PlayerComponent;
import com.roguelike.entity.PositionComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class SaveManager {

    private static final Path SAVE_DIR = Paths.get(System.getProperty("user.home"), ".roguelike-dungeon");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save(Entity player, int floor, long seed, String slot) throws IOException {
        Files.createDirectories(SAVE_DIR);
        var sd = new SaveData();
        sd.timestamp = System.currentTimeMillis();
        sd.floor = floor;
        sd.seed = seed;

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

    public static Path saveDir() { return SAVE_DIR; }
}
