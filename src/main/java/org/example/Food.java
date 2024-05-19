package org.example;

public class Food {
    final int x, y;
    private final double size;
    private final double rsp;
    private final long spawnTime;

    public Food(int x, int y, double size, boolean fastSpawn) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.rsp = fastSpawn ? 4 : 1;
        spawnTime = System.currentTimeMillis();
    }

    public double getRadius() {
        double fillRate = rsp * (System.currentTimeMillis() - spawnTime) / 1200.0;
        if (fillRate >= 1) {
            return size;
        } else {
            return (1 - Math.cos(Math.PI * fillRate)) / 2 * size;
        }
    }
}
