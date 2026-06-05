package com.roguelike.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Entity {

    private final UUID id;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    public Entity() {
        this.id = UUID.randomUUID();
    }

    public UUID id() { return id; }

    public <T extends Component> T add(T component) {
        components.put(component.getClass(), component);
        return component;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        return (T) components.get(type);
    }

    public boolean has(Class<? extends Component> type) {
        return components.containsKey(type);
    }

    public <T extends Component> T remove(Class<T> type) {
        return type.cast(components.remove(type));
    }

    public boolean hasAll(Class<? extends Component>... types) {
        for (var type : types) {
            if (!components.containsKey(type)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Entity[" + id.toString().substring(0, 8) + "]";
    }
}
