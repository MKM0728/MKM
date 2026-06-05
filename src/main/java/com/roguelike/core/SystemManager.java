package com.roguelike.core;

import java.util.ArrayList;
import java.util.List;

public class SystemManager {

    private final List<System> systems = new ArrayList<>();

    public void register(System system) {
        systems.add(system);
    }

    public void update(List<Entity> entities, double tpf) {
        for (var system : systems) {
            var matched = entities.stream()
                .filter(system::matches)
                .toList();
            system.update(matched, tpf);
        }
    }

    public int systemCount() {
        return systems.size();
    }
}
