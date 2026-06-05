package com.roguelike.entity;

import com.roguelike.core.Component;

public final class RenderComponent implements Component {
    private final String spriteName;

    public RenderComponent(String spriteName) {
        this.spriteName = spriteName;
    }

    public String spriteName() { return spriteName; }
}
