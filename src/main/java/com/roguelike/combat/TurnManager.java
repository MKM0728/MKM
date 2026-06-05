package com.roguelike.combat;

import com.roguelike.core.Entity;
import com.roguelike.entity.CombatStatsComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TurnManager {

    private final List<Entity> queue = new ArrayList<>();
    private int currentIndex = 0;
    private int round = 0;

    public void init(List<Entity> combatants) {
        queue.clear();
        queue.addAll(combatants);
        rollInitiative();
        currentIndex = 0;
        round = 0;
    }

    private void rollInitiative() {
        queue.sort(Comparator.comparingInt(e -> {
            var stats = e.get(CombatStatsComponent.class);
            return stats != null ? -stats.initiativeRoll() : 0;
        }));
    }

    public Entity current() {
        if (queue.isEmpty()) return null;
        return queue.get(currentIndex);
    }

    public Entity next() {
        if (queue.isEmpty()) return null;
        currentIndex++;
        if (currentIndex >= queue.size()) {
            currentIndex = 0;
            round++;
        }
        return current();
    }

    public boolean hasNext() {
        return !queue.isEmpty();
    }

    public int round() { return round; }

    public boolean isNewRound() { return currentIndex == 0 && round > 0; }

    public void remove(Entity entity) {
        int idx = queue.indexOf(entity);
        if (idx < 0) return;
        queue.remove(idx);
        if (idx < currentIndex) {
            currentIndex--;
        }
        if (currentIndex >= queue.size()) {
            currentIndex = 0;
        }
    }

    public List<Entity> queue() { return List.copyOf(queue); }
    public int size() { return queue.size(); }
}
