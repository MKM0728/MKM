package com.roguelike.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InventoryPanelTest {

    @Test
    void inventoryPanelExists() {
        var panel = new InventoryPanel();
        assertNotNull(panel);
        assertFalse(panel.isVisible());
    }

    @Test
    void setHandlersDoesNotThrow() {
        assertDoesNotThrow(() -> {
            var panel = new InventoryPanel();
            panel.setOnUse(e -> {});
            panel.setOnDrop(e -> {});
        });
    }
}
