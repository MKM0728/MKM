package com.roguelike.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ComponentTest {

    record TestComp(int value) implements Component {}

    @Test
    void recordImplementsComponent() {
        TestComp c = new TestComp(42);
        assertTrue(c instanceof Component);
        assertEquals(42, c.value());
    }

    @Test
    void componentCanBeUsedAsType() {
        Component c = new TestComp(7);
        assertNotNull(c);
    }
}
