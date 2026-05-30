package com.roguelike.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PositionComponentTest {

    @Test
    void initialPosition() {
        var pos = new PositionComponent(5, 10);
        assertEquals(5, pos.x());
        assertEquals(10, pos.y());
    }

    @Test
    void setPosition() {
        var pos = new PositionComponent(0, 0);
        pos.set(3, 7);
        assertEquals(3, pos.x());
        assertEquals(7, pos.y());
    }

    @Test
    void chebyshevDistance() {
        var a = new PositionComponent(0, 0);
        var b = new PositionComponent(3, 5);
        assertEquals(5, a.distanceTo(b));
    }

    @Test
    void samePositionZeroDistance() {
        var a = new PositionComponent(4, 4);
        var b = new PositionComponent(4, 4);
        assertEquals(0, a.distanceTo(b));
    }
}
