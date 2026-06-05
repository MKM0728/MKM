package com.roguelike.ai;

import com.roguelike.map.Tile;

import java.util.*;

public class PathFinder {

    private static final double DIAGONAL_COST = 1.4;
    private static final double CARDINAL_COST = 1.0;
    private static final int[][] DIRECTIONS = {
        {0, -1}, {0, 1}, {-1, 0}, {1, 0},        // cardinal
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1}        // diagonal
    };

    private final int width;
    private final int height;
    private final Tile[][] grid;
    private List<Point> cachedPath;
    private Point cachedTarget;

    public PathFinder(Tile[][] grid) {
        this.grid = grid;
        this.height = grid.length;
        this.width = height > 0 ? grid[0].length : 0;
    }

    public List<Point> findPath(int startX, int startY, int goalX, int goalY) {
        if (startX == goalX && startY == goalY) return List.of();

        if (cachedTarget != null && cachedTarget.x == goalX && cachedTarget.y == goalY) {
            return cachedPath;
        }

        var open = new PriorityQueue<Node>();
        var closed = new boolean[height][width];
        var gScore = new double[height][width];
        var cameFrom = new Node[height][width];

        for (int y = 0; y < height; y++) {
            Arrays.fill(gScore[y], Double.POSITIVE_INFINITY);
        }

        gScore[startY][startX] = 0;
        open.add(new Node(startX, startY, 0, heuristic(startX, startY, goalX, goalY)));

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (current.x == goalX && current.y == goalY) {
                cachedPath = reconstructPath(cameFrom, current);
                cachedTarget = new Point(goalX, goalY);
                return cachedPath;
            }

            if (closed[current.y][current.x]) continue;
            closed[current.y][current.x] = true;

            for (int i = 0; i < DIRECTIONS.length; i++) {
                int nx = current.x + DIRECTIONS[i][0];
                int ny = current.y + DIRECTIONS[i][1];

                if (!inBounds(nx, ny) || closed[ny][nx] || !grid[ny][nx].isWalkable()) {
                    continue;
                }

                // block diagonal movement if both cardinal neighbors aren't walkable
                boolean isDiagonal = i >= 4;
                if (isDiagonal) {
                    int cx = current.x + DIRECTIONS[i][0];
                    int cy = current.y;
                    int rx = current.x;
                    int ry = current.y + DIRECTIONS[i][1];
                    if ((inBounds(cx, cy) && !grid[cy][cx].isWalkable())
                        || (inBounds(rx, ry) && !grid[ry][rx].isWalkable())) {
                        continue;
                    }
                }

                double moveCost = isDiagonal ? DIAGONAL_COST : CARDINAL_COST;
                double tentativeG = gScore[current.y][current.x] + moveCost;

                if (tentativeG < gScore[ny][nx]) {
                    gScore[ny][nx] = tentativeG;
                    double f = tentativeG + heuristic(nx, ny, goalX, goalY);
                    Node neighbor = new Node(nx, ny, tentativeG, f);
                    open.add(neighbor);
                    cameFrom[ny][nx] = current;
                }
            }
        }

        return List.of(); // no path
    }

    public void clearCache() {
        cachedPath = null;
        cachedTarget = null;
    }

    private List<Point> reconstructPath(Node[][] cameFrom, Node current) {
        var path = new ArrayList<Point>();
        Node node = current;
        while (node != null) {
            path.add(new Point(node.x, node.y));
            node = cameFrom[node.y][node.x];
        }
        Collections.reverse(path);
        path.remove(0); // remove start position
        return path;
    }

    private double heuristic(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        return CARDINAL_COST * Math.max(dx, dy) + (DIAGONAL_COST - CARDINAL_COST) * Math.min(dx, dy);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public record Point(int x, int y) {}

    private record Node(int x, int y, double g, double f) implements Comparable<Node> {
        @Override
        public int compareTo(Node o) { return Double.compare(this.f, o.f); }
    }
}
