package org.example;

import java.util.Deque;

public class Snake {
    final int id;
    final String name;
    double x, y;
    int dir; // direction
    double wang, arg; // snake movement
    double sp, tsp; // speed and temporary speed
    private double fam; // acceleration factor
    final Deque<SnakeBodyPart> body;
    private final MySlitherModel model;

    public Snake(int id, String name, double x, double y, double wang, double arg, double sp,
                 double fam, Deque<SnakeBodyPart> body, MySlitherModel model) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.wang = wang;
        this.arg = arg;
        this.sp = sp;
        this.fam = fam;
        this.body = body;
        this.model = model;
    }

    // Get size of a snake
    private double getSc() {
        return Math.min(6, 1 + (body.size() - 2) / 106.0);
    }

    //Get direction change angle
    double getScang() {
        return 0.13 + 0.87 * Math.pow((7 - getSc()) / 6, 2);
    }
    // Get turning speed percentage
    double getSpang() {
        return Math.min(sp / model.spangdv, 1);
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
