package com.roguelike.save;

import static org.junit.jupiter.api.Assertions.*;

import com.roguelike.core.Entity;
import com.roguelike.entity.CombatStatsComponent;
import com.roguelike.entity.HealthComponent;
import com.roguelike.entity.PlayerComponent;
import com.roguelike.entity.PositionComponent;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

class SaveManagerTest {

    private Entity player;

    @BeforeEach
    void setUp() {
        player = new Entity();
        player.add(new PositionComponent(5, 10));
        player.add(new HealthComponent(80, 100));
        player.add(new CombatStatsComponent(12, 5, 8));
        player.add(new PlayerComponent());
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        SaveManager.save(player, 3, 12345L, 0, 0, "FISTS",
            new ArrayList<>(), new ArrayList<>(), false, 0, 0, 0, 0, false, "test-slot");
        var data = SaveManager.load("test-slot");
        assertNotNull(data);
        assertEquals(5, data.playerX);
        assertEquals(10, data.playerY);
        assertEquals(80, data.playerHp);
        assertEquals(100, data.playerMaxHp);
        assertEquals(3, data.floor);
    }

    @Test
    void slotExistsAfterSave() throws IOException {
        SaveManager.save(player, 1, 99L, 0, 0, "FISTS",
            new ArrayList<>(), new ArrayList<>(), false, 0, 0, 0, 0, false, "test-exists");
        assertTrue(SaveManager.slotExists("test-exists"));
    }

    @Test
    void missingSlotReturnsNull() throws IOException {
        var data = SaveManager.load("nonexistent-xyz");
        assertNull(data);
    }

    @AfterAll
    static void cleanup() throws IOException {
        var dir = SaveManager.saveDir();
        if (Files.exists(dir)) {
            try (var files = Files.list(dir)) {
                files.filter(f -> f.toString().endsWith(".json")).forEach(f -> {
                    try { Files.deleteIfExists(f); } catch (IOException ignored) {}
                });
            }
        }
    }
}
