package com.roguelike.core;

import java.util.Collections;
import java.util.List;

public abstract class System {

    private final List<Class<? extends Component>> requiredComponents;

    @SafeVarargs
    protected System(Class<? extends Component>... required) {
        this.requiredComponents = List.of(required);
    }

    public List<Class<? extends Component>> requiredComponents() {
        return Collections.unmodifiableList(requiredComponents);
    }

    public boolean matches(Entity entity) {
        for (var type : requiredComponents) {
            if (!entity.has(type)) return false;
        }
        return true;
    }

    public abstract void update(List<Entity> entities, double tpf);
}
