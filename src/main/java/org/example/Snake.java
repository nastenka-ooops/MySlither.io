package org.example;

import java.awt.*;
import java.util.Deque;

public class Snake {
    final int id;
    final String name;
    double x, y;
    Color skin;
    int dir; // direction
    double wang, ang; // snake movement
    double speed, tsp; // speed and temporary speed
    private double fam; // acceleration factor
    final Deque<SnakeBodyPart> body;
    private final MySlitherModel model;

    public Snake(int id, String name, Color skin,  double x, double y, double wang, double ang, double speed,
                 double fam, Deque<SnakeBodyPart> body, MySlitherModel model) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.wang = wang;
        this.ang = ang;
        this.speed = speed;
        this.fam = fam;
        this.body = body;
        this.model = model;
        this.skin = skin;
    }

    // Get size of a snake
    private double getSc() {
        return Math.min(6, 1 + (body.size() - 2) / 106.0);
    }

    // get the factor of snake speed
    private double getFsp() {
        return model.nsp1 + model.nsp2 * getSc();
    }

    boolean isBoosting() {
        return tsp > getFsp();
    }
    double getFam() {
        return fam;
    }

    void setFam(double fam) {
        this.fam = fam;
    }
}
