package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.random;

public class MySlitherCanvas extends JPanel {
    private final MySlitherJFrame view;
    private static final Color BACKGROUND_COLOR = new Color(0x2B2B2B);
    private static final Color FOREGROUND_COLOR = new Color(0xA9B7C6);
    private static final Color SECTOR_COLOR = new Color(0x803C3F41);
    private static final Color FOOD_COLOR = new Color(0xAADFD0);
    private static final Color PREY_COLOR = new Color(0x8021);
    private static final float[] PREY_HALO_FRACTIONS = new float[]{0.5f, 1f};
    private static final Color[] PREY_HALO_COLORS = new Color[]{new Color(0x66FFFF10, true), new Color(0x00FFFF00, true)};
    private static final Color SNAKE_COLOR = new Color(0xFFAFFAF);
    private static final Color OWN_SNAKE_COLOR = new Color(0x39AFFF);
    private static final float[] SNAKE_HALO_FRACTIONS = new float[]{0.5f, 1f};
    private static final Color[] SNAKE_HALO_COLORS = new Color[]{new Color(0x60287BDE, true), new Color(0x00287BDE, true)};
    private static final Color[] OWN_SNAKE_HALO_COLORS = new Color[]{new Color(0x6039AFFF, true), new Color(0x0039AFFF, true)};
    private static final Color SNAKE_BODY_COLOR = new Color(0x6A8759);
    private static final Color NAME_SHADOW_COLOR = new Color(0xC02B2B2B, true);
    private static final Font NAME_FONT = Font.decode("SansSerif-BOLD");
    private static Color OWN_SNAKE_BODY_COLOR = new Color(0xA5C261);


    private final int MAX_ZOOM = 18;
    private final int MIN_ZOOM = -2;

    Random random = new Random();

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

        OWN_SNAKE_BODY_COLOR = view.SNAKES.get(view.snake.getSelectedIndex());

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
                                model.sectorSize-2, model.sectorSize-2);
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

            //Paint prey
            model.preys.values().forEach(prey -> {
                double preyRadius = prey.getRadius();
                if (preyRadius <= 0) {
                    return;
                }
                g.setPaint(new RadialGradientPaint((float) (prey.x - 0.5/scale),(float) (prey.y - 0.5 / scale),
                        (float) (preyRadius*2), PREY_HALO_FRACTIONS , PREY_HALO_COLORS));
                g.fillRect((int) Math.floor(prey.x - preyRadius * 2 - 1), (int) Math.floor(prey.y - preyRadius * 2 - 1),
                        (int) (preyRadius * 4 + 3), (int) (preyRadius * 4 + 3));
                g.setColor(PREY_COLOR);
                g.fill(new Ellipse2D.Double(prey.x - preyRadius, prey.y - preyRadius,
                        preyRadius * 2, preyRadius * 2));
            });


            //Paint snakes
            oldStroke = g.getStroke();
            g.setFont(NAME_FONT.deriveFont((float) (18 / Math.pow(scale, 0.75))));
            model.snakes.values().forEach(snake -> {
                double thickness = 16 + snake.body.size() / 4.0;
                if (snake.body.size() >= 2) {
                    g.setColor(snake == model.snake ? OWN_SNAKE_BODY_COLOR : snake.skin/*SNAKE_BODY_COLOR*/);
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

                // Проверка на ускорение змеи
                if (snake.isBoosting()) {
                    g.setPaint(new RadialGradientPaint(
                            (float) (snake.x - 0.5 / scale),
                            (float) (snake.y - 0.5 / scale),
                            (float) (thickness * 4 / 3),
                            SNAKE_HALO_FRACTIONS,
                            snake == model.snake ? OWN_SNAKE_HALO_COLORS : SNAKE_HALO_COLORS
                    ));
                    g.fillRect(
                            (int) Math.round(snake.x - thickness * 3 / 2 - 1),
                            (int) Math.round(snake.y - thickness * 3 / 2 - 1),
                            (int) (thickness * 3 + 2),
                            (int) (thickness * 3 + 2)
                    );
                }

                g.setColor(snake == model.snake ? OWN_SNAKE_COLOR : SNAKE_COLOR);
                g.fill(new Ellipse2D.Double(
                        snake.x - thickness * 2 / 3,
                        snake.y - thickness * 2 / 3,
                        thickness * 4 / 3,
                        thickness * 4 / 3
                ));
                // Получаем текст длины змеи
                String lengthText = "" + model.getSnakeLength(snake.body.size(), snake.getFam());

                // Рисуем тень для текста имени и длины змеи
                g.setColor(NAME_SHADOW_COLOR);
                g.drawString(snake.name,
                        (float) (snake.x - g.getFontMetrics().stringWidth(snake.name) / 2.0 + g.getFontMetrics().getHeight() / 12.0),
                        (float) (snake.y - thickness * 2 / 3 - g.getFontMetrics().getHeight() + g.getFontMetrics().getHeight() / 12.0)
                );
                g.drawString(lengthText,
                        (float) (snake.x - g.getFontMetrics().stringWidth(lengthText) / 2.0 + g.getFontMetrics().getHeight() / 12.0),
                        (float) (snake.y - thickness * 2 / 3 + g.getFontMetrics().getHeight() / 12.0)
                );

                // Рисуем текст имени и длины змеи
                g.setColor(FOREGROUND_COLOR);
                g.drawString(snake.name,
                        (float) (snake.x - g.getFontMetrics().stringWidth(snake.name) / 2.0),
                        (float) (snake.y - thickness * 2 / 3 - g.getFontMetrics().getHeight())
                );
                g.drawString(lengthText,
                        (float) (snake.x - g.getFontMetrics().stringWidth(lengthText) / 2.0),
                        (float) (snake.y - thickness * 2 / 3)
                );
            });

            g.setStroke(oldStroke);
            g.setTransform(oldTransform);
            //TODO
            //Paint minimap
        }
    }
}
