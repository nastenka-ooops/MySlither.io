package org.example;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class MySlitherModel {
    static final double PI2 = Math.PI * 2;

    final double spangdv;
    final double nsp1, nsp2, nsp3;
    private final double mamu1, mamu2;
    final int gameRadius;
    final int sectorSize;
    private final double cst;
    private final int mscps;

    private final MySlitherJFrame view;

    Snake snake;

    Map<Integer, Snake> snakes = new LinkedHashMap<>();

    public MySlitherModel(double spangdv, double nsp1, double nsp2, double nsp3, double mamu1, double mamu2,
                          int gameRadius, int sectorSize, double cst, int mscps, MySlitherJFrame view) {
        this.gameRadius = gameRadius;
        this.sectorSize = sectorSize;

        this.spangdv = spangdv;
        this.nsp1 = nsp1;
        this.nsp2 = nsp2;
        this.nsp3 = nsp3;
        this.mamu1 = mamu1;
        this.mamu2 = mamu2;
        this.cst = cst;
        this.mscps = mscps;
        this.view = view;

    }

    public void removeSnake(int id) {
        synchronized (view.modelLock) {
            snakes.remove(id);
        }
    }
    public void addSnake(int snakeId, String name, double x, double y, double ang, double wang, double speed, double fam,
                          Deque<SnakeBodyPart> body){
        synchronized (view.modelLock){
            Snake newSnake = new Snake(snakeId, name, x, y, wang, ang, speed, fam, body, this);
            if (snake==null){
                snake=newSnake;
            }
            snakes.put(snakeId, newSnake);
        }
    }
    Snake getSnake(int snakeID) {
        return snakes.get(snakeID);
    }
}
