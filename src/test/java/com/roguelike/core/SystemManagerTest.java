package com.roguelike.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SystemManagerTest {

    record CompA(int v) implements Component {}
    record CompB(String s) implements Component {}

    private SystemManager manager;
    private int updateCount;

    @BeforeEach
    void setUp() {
        manager = new SystemManager();
        updateCount = 0;
    }

    @Test
    void registerSystem() {
        var sys = new System(CompA.class) {
            @Override
            public void update(List<Entity> entities, double tpf) {}
        };
        manager.register(sys);
        assertEquals(1, manager.systemCount());
    }

    @Test
    void matchingEntityProcessed() {
        var sys = new System(CompA.class) {
            @Override
            public void update(List<Entity> entities, double tpf) {
                updateCount += entities.size();
            }
        };
        manager.register(sys);

        var entity = new Entity();
        entity.add(new CompA(1));
        manager.update(List.of(entity), 0.016);
        assertEquals(1, updateCount);
    }

    @Test
    void nonMatchingEntitySkipped() {
        var sys = new System(CompA.class) {
            @Override
            public void update(List<Entity> entities, double tpf) {
                updateCount += entities.size();
            }
        };
        manager.register(sys);

        var entity = new Entity();
        entity.add(new CompB("x"));
        manager.update(List.of(entity), 0.016);
        assertEquals(0, updateCount);
    }

    @Test
    void multipleRequiresAllMatch() {
        var sys = new System(CompA.class, CompB.class) {
            @Override
            public void update(List<Entity> entities, double tpf) {
                updateCount = entities.size();
            }
        };
        var entity = new Entity();
        entity.add(new CompA(1));
        entity.add(new CompB("y"));
        assertTrue(sys.matches(entity));
    }

    @Test
    void missingOneRequiredFailsMatch() {
        var sys = new System(CompA.class, CompB.class) {
            @Override
            public void update(List<Entity> entities, double tpf) {}
        };
        var entity = new Entity();
        entity.add(new CompA(1));
        assertFalse(sys.matches(entity));
    }
}
