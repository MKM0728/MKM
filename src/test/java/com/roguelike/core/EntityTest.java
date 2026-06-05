package com.roguelike.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class EntityTest {

    record TestCompA(int value) implements Component {}
    record TestCompB(String label) implements Component {}

    private Entity entity;

    @BeforeEach
    void setUp() {
        entity = new Entity();
    }

    @Test
    void hasUniqueId() {
        var e1 = new Entity();
        var e2 = new Entity();
        assertNotEquals(e1.id(), e2.id());
    }

    @Test
    void addAndGetComponent() {
        entity.add(new TestCompA(42));
        var a = entity.get(TestCompA.class);
        assertNotNull(a);
        assertEquals(42, a.value());
    }

    @Test
    void hasComponent() {
        assertFalse(entity.has(TestCompA.class));
        entity.add(new TestCompA(1));
        assertTrue(entity.has(TestCompA.class));
    }

    @Test
    void removeComponent() {
        entity.add(new TestCompB("hello"));
        var removed = entity.remove(TestCompB.class);
        assertNotNull(removed);
        assertEquals("hello", removed.label());
        assertFalse(entity.has(TestCompB.class));
    }

    @Test
    void hasAllComponents() {
        entity.add(new TestCompA(1));
        entity.add(new TestCompB("x"));
        assertTrue(entity.hasAll(TestCompA.class, TestCompB.class));
        assertFalse(entity.hasAll(TestCompA.class, TestCompB.class, Component.class));
    }
}
