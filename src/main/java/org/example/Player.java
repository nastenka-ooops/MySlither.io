package org.example;

import static org.example.MySlitherModel.PI2;

public abstract class Player {
    final String name;

    public Player(String name) {
        this.name = name;
    }
    public abstract Wish action(MySlitherModel model);

    static class Wish{
        final double angle;
        final boolean boost;

        public Wish(double angle, boolean boost) {
            if (angle<0 || angle>=PI2){
                throw new IllegalArgumentException("angle not in range 0 to PI2");
            }
            this.angle = angle;
            this.boost = boost;
        }
    }
}
