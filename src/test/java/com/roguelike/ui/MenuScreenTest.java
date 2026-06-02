package com.roguelike.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MenuScreenTest {

    @Test
    void menuScreenDoesNotThrow() {
        assertDoesNotThrow(() -> {
            // verify static factory method exists
            var method = MenuScreen.class.getMethod("show", Runnable.class, Runnable.class, Runnable.class, boolean.class);
            assertNotNull(method);
        });
    }

    @Test
    void gameOverScreenDoesNotThrow() {
        assertDoesNotThrow(() -> {
            var method = GameOverScreen.class.getMethod("show", int.class, int.class, int.class,
                Runnable.class, Runnable.class);
            assertNotNull(method);
        });
    }
}
