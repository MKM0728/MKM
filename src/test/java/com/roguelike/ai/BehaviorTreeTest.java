package com.roguelike.ai;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BehaviorTreeTest {

    private BehaviorTree bt;

    @BeforeEach
    void setUp() {
        bt = new BehaviorTree();
    }

    @Test
    void idleTransitionsToWander() {
        var state = bt.update(false, 1.0, 5, 5, 10, 10);
        assertEquals(AiState.WANDER, state);
    }

    @Test
    void seesPlayerAndChases() {
        var state = bt.update(true, 1.0, 5, 5, 10, 10);
        assertEquals(AiState.CHASE, state);
    }

    @Test
    void adjacentToPlayerAttacks() {
        var state = bt.update(true, 1.0, 5, 5, 6, 5);
        assertEquals(AiState.ATTACK, state);
    }

    @Test
    void lowHpFleesWhenVisible() {
        var state = bt.update(true, 0.2, 5, 5, 10, 10);
        assertEquals(AiState.FLEE, state);
    }

    @Test
    void lowHpDoesNotFleeWhenHidden() {
        var state = bt.update(false, 0.2, 5, 5, 10, 10);
        assertEquals(AiState.WANDER, state);
    }

    @Test
    void wanderTargetChanges() {
        bt.update(false, 1.0, 5, 5, 10, 10);
        int[] t1 = bt.getWanderTarget(5, 5, 0, 0, 10, 10);
        assertTrue(t1[0] >= 0 && t1[0] <= 10);
        assertTrue(t1[1] >= 0 && t1[1] <= 10);
    }

    @Test
    void fleeTargetMovesAwayFromPlayer() {
        bt.update(true, 0.2, 5, 5, 10, 10);
        int[] flee = bt.getFleeTarget(5, 5, 3, 3);
        assertTrue(flee[0] > 5);
        assertTrue(flee[1] > 5);
    }

    @Test
    void resetReturnsToIdle() {
        bt.update(true, 0.2, 5, 5, 10, 10);
        bt.reset();
        assertEquals(AiState.IDLE, bt.state());
    }
}
