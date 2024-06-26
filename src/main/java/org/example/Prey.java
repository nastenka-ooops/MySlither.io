package org.example;

public class Prey {
    double x, y;
    int dir;
    double wang, ang;
    double speed;
    private final double size;
    private final long spawnTime;

    public Prey(double x, double y, int dir, double wang, double ang, double speed, double size) {
        this.x = x;
        this.y = y;
        this.dir = dir;
        this.wang = wang;
        this.ang = ang;
        this.speed = speed;
        this.size = size;
        this.spawnTime = System.currentTimeMillis();
    }


    public double getRadius() {
        double fillRate = (System.currentTimeMillis() - spawnTime) / 1200.0;
        if (fillRate >= 1) {
            return size;
        } else {
            return (1 - Math.cos(Math.PI * fillRate)) / 2 * size;
        }
    }
}
