package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MySlitherCanvas extends JPanel {
    private final MySlitherJFrame view;
    private static final Color BACKGROUND_COLOR = new Color(0x2B2B2B);
    private static final Color FOREGROUND_COLOR = new Color(0xA9B7C6);
    private static final Color SECTOR_COLOR = new Color(0x803C3F41);
    private static final Color FOOD_COLOR = new Color(0x803664);
    private static final Color SNAKE_COLOR = new Color(0x287BDE);
    private static final Color OWN_SNAKE_COLOR = new Color(0x39AFFF);

    private final int MAX_ZOOM = 18;
    private final int MIN_ZOOM = -2;


    private int zoom = 12;

    public ScheduledExecutorService repaintThread;

    public MySlitherCanvas(MySlitherJFrame view) {
        super();
        this.view = view;

        setBackground(BACKGROUND_COLOR);
        setForeground(FOREGROUND_COLOR);

        addMouseWheelListener(e -> {
            zoom -= e.getWheelRotation();
            zoom = Math.max(zoom, MIN_ZOOM);
            zoom = Math.min(zoom, MAX_ZOOM);
        });

        GraphicsEnvironment localGraphicEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        int refreshRate = localGraphicEnvironment.getDefaultScreenDevice().getDisplayMode().getRefreshRate();
        long repaintDelay = 1000000000 / (refreshRate != DisplayMode.REFRESH_RATE_UNKNOWN ? refreshRate : 60);

        repaintThread = Executors.newSingleThreadScheduledExecutor();
        repaintThread.scheduleAtFixedRate(this::repaint, 1, repaintDelay, TimeUnit.NANOSECONDS);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (!(graphics instanceof Graphics2D)) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int minDimension = Math.min(width, height);

        modelPaintBlock:
        synchronized (view.modelLock) {
            MySlitherModel model = view.model;
            if (model == null) {
                break modelPaintBlock;
            }

            AffineTransform oldTransform = g.getTransform();
            double scale;

            g.translate(width / 2, height / 2);
            scale = Math.pow(1.25, zoom + 1) * minDimension / (model.gameRadius * 2);
            g.scale(scale, scale);
            g.translate(-model.snake.x, -model.snake.y);


            //Paint sectors
            g.setColor(SECTOR_COLOR);
            for (int y = 0; y < model.sectors.length; y++) {
                for (int x = 0; x < model.sectors[y].length; x++) {
                    if (model.sectors[y][x]) {
                        g.fillRect(x * model.sectorSize + 1, y * model.sectorSize + 1,
                                model.sectorSize, model.sectorSize);
                    }
                }
            }

            //Paint borders of playing field
            g.setColor(FOREGROUND_COLOR);
            Stroke oldStroke = g.getStroke();
            g.setStroke(new BasicStroke(128));
            g.drawOval(-64, -64, model.gameRadius * 2 + 128, model.gameRadius * 2 + 128);
            g.setStroke(oldStroke);

            //Paint food
            g.setColor(FOOD_COLOR);
            model.foods.values().forEach(food -> {
                double foodRadius = food.getRadius();
                g.fill(new Ellipse2D.Double(food.x - foodRadius, food.y - foodRadius,
                        foodRadius * 2, foodRadius * 2));
            });


            //TODO add nick
            //Paint snakes
            oldStroke = g.getStroke();
            model.snakes.values().forEach(snake -> {
                if (snake.body.size() >= 2) {
                    double thickness = 16 + snake.body.size() / 4.0;
                    g.setColor(snake == model.snake ? OWN_SNAKE_COLOR : SNAKE_COLOR);
                    g.setStroke(new BasicStroke((float) thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));


                    double totalLength = 0;
                    double lastX = 0;
                    double lastY = 0;

                    // calculate total length
                    for (SnakeBodyPart bodyPart : snake.body) {
                        if (bodyPart != snake.body.getFirst()) {
                            totalLength += Math.sqrt(Math.pow((bodyPart.x - lastX), 2) +
                                    Math.pow((bodyPart.y - lastY), 2));
                        }
                        if (bodyPart != snake.body.getLast()) {
                            lastX = bodyPart.x;
                            lastY = bodyPart.y;
                        }
                    }


                    //Drawing snake path
                    Path2D.Double snakePath = new Path2D.Double();
                    snakePath.moveTo(snake.x, snake.y);

                    lastX = snake.x;
                    lastY = snake.y;

                    for (SnakeBodyPart bodyPart : snake.body) {
                        double partLength = Math.sqrt(Math.pow((bodyPart.x - lastX),2) +
                                Math.pow((bodyPart.y - lastY), 2));
                        if (partLength>totalLength){
                            snakePath.lineTo(lastX + (totalLength/partLength)*(bodyPart.x-lastX),
                                    lastY + (totalLength/partLength)*(bodyPart.y)-lastY);
                            break;
                        }
                        snakePath.lineTo(bodyPart.x, bodyPart.y);
                        totalLength -= partLength;
                        lastX = bodyPart.x;
                        lastY = bodyPart.y;
                    }

                    g.draw(snakePath);
                }
            });
        }
    }
}
