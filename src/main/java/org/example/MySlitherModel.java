package org.example;

import java.awt.*;
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
    private final double[] fpsls, fmlts;

    final boolean[][] sectors;
    private long lastUpdateTime;
    private final MySlitherJFrame view;

    Snake snake;

    Map<Integer, Snake> snakes = new LinkedHashMap<>();
    Map<Integer, Prey> preys = new LinkedHashMap<>();
    Map<Integer, Food> foods = new LinkedHashMap<>();

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

        sectors = new boolean[gameRadius * 2 / sectorSize][gameRadius * 2 / sectorSize];

        fmlts = new double[mscps + 1];
        fpsls = new double[mscps + 1];
        for (int i = 0; i < mscps; i++) {
            double base = (double) (mscps - i) / mscps;
            fmlts[i] = 1 / (base * base * Math.sqrt(Math.sqrt(base)));
            fpsls[i + 1] = fpsls[i] + fmlts[i];
        }

        lastUpdateTime = System.currentTimeMillis();

    }

    public void removeSnake(int id) {
        synchronized (view.modelLock) {
            snakes.remove(id);
        }
    }

    public void addSnake(int snakeId, String name, Color skin, double x, double y, double ang, double wang, double speed, double fam,
                         Deque<SnakeBodyPart> body) {
        synchronized (view.modelLock) {
            Snake newSnake = new Snake(snakeId, name, skin, x, y, wang, ang, speed, fam, body, this);
            if (snake == null) {
                snake = newSnake;
            }
            snakes.put(snakeId, newSnake);
        }
    }

    Snake getSnake(int snakeID) {
        return snakes.get(snakeID);
    }

    int getSnakeLength(int bodyLength, double fillAmount) {
        bodyLength = Math.min(bodyLength, mscps);
        return (int) (15 * (fpsls[bodyLength] + fillAmount * fmlts[bodyLength]) - 20);
    }
    Prey getPrey(int id) {
        return preys.get(id);
    }

    void removePrey(int id){
        synchronized (view.modelLock){
            preys.remove(id);
        }
    }

    void addPrey(int id, Prey prey){
        synchronized (view.modelLock){
            preys.put(id, prey);
        }
    }

    void addSector(int x, int y) {
        synchronized (view.modelLock) {
            sectors[y][x] = true;
        }
    }

    public void removeSector(int x, int y) {
        synchronized (view.modelLock) {
            sectors[y][x] = false;
            foods.values().removeIf(food -> food.x / sectorSize == x && food.y / sectorSize == y);
        }
    }

    public void addFood(int x, int y, double size, boolean isFastSpawn) {
        synchronized (view.modelLock) {
            foods.put((y * gameRadius * 3) + x, new Food(x, y, size, isFastSpawn));
        }
    }

    public void removeFood(int x, int y){
        synchronized (view.modelLock) {
            foods.remove(y * sectorSize *3 + x);
        }
    }
}
