package com.roguelike.entity;

import com.roguelike.core.Component;

public final class PositionComponent implements Component {
    private int x;
    private int y;

    public PositionComponent(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }
    public int y() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int distanceTo(PositionComponent other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }
}
