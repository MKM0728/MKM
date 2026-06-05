package com.roguelike.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HudOverlayTest {

    @Test
    void hudClassHasUpdateMethod() {
        assertDoesNotThrow(() -> {
            var method = HudOverlay.class.getMethod("update", int.class, int.class, int.class, int.class, String.class);
            assertNotNull(method);
        });
    }

    @Test
    void hudClassHasRemoveMethod() {
        assertDoesNotThrow(() -> {
            var method = HudOverlay.class.getMethod("remove");
            assertNotNull(method);
        });
    }
}
