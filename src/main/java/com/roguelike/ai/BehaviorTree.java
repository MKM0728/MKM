package com.roguelike.ai;

import java.util.Random;

public class BehaviorTree {

    private static final double FLEE_HP_RATIO = 0.3;
    private static final int ATTACK_RANGE = 1;
    private static final int FLEE_SAFE_DISTANCE = 8;

    private AiState state = AiState.IDLE;
    private int wanderTargetX, wanderTargetY;
    private final Random rng = new Random();

    public AiState update(boolean canSeePlayer, double hpRatio,
                          int enemyX, int enemyY, int playerX, int playerY) {

        if (hpRatio < FLEE_HP_RATIO && canSeePlayer) {
            state = AiState.FLEE;
            return state;
        }

        int dist = Math.max(Math.abs(enemyX - playerX), Math.abs(enemyY - playerY));

        if (canSeePlayer) {
            if (dist <= ATTACK_RANGE) {
                state = AiState.ATTACK;
            } else {
                state = AiState.CHASE;
            }
            return state;
        }

        if (state == AiState.CHASE || state == AiState.FLEE) {
            if (!canSeePlayer && dist > FLEE_SAFE_DISTANCE) {
                state = AiState.WANDER;
            }
        }

        if (state == AiState.IDLE || state == AiState.WANDER) {
            state = AiState.WANDER;
        }

        return state;
    }

    public int[] getWanderTarget(int currentX, int currentY, int roomMinX, int roomMinY,
                                  int roomMaxX, int roomMaxY) {
        if (state != AiState.WANDER) return new int[]{currentX, currentY};

        if (wanderTargetX == 0 && wanderTargetY == 0
            || (currentX == wanderTargetX && currentY == wanderTargetY)) {
            int rangeX = roomMaxX - roomMinX;
            int rangeY = roomMaxY - roomMinY;
            wanderTargetX = roomMinX + rng.nextInt(Math.max(1, rangeX));
            wanderTargetY = roomMinY + rng.nextInt(Math.max(1, rangeY));
        }
        return new int[]{wanderTargetX, wanderTargetY};
    }

    public int[] getFleeTarget(int enemyX, int enemyY, int playerX, int playerY) {
        int dx = enemyX - playerX;
        int dy = enemyY - playerY;
        int fleeX = enemyX + Integer.signum(dx) * FLEE_SAFE_DISTANCE;
        int fleeY = enemyY + Integer.signum(dy) * FLEE_SAFE_DISTANCE;
        return new int[]{fleeX, fleeY};
    }

    public AiState state() { return state; }

    public void reset() {
        state = AiState.IDLE;
        wanderTargetX = 0;
        wanderTargetY = 0;
    }
}
