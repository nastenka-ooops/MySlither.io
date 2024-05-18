package org.example;

import javax.swing.*;
import java.awt.*;

public class MySlitherCanvas extends JPanel {
    private final MySlitherJFrame view;
    private static final Color BACKGROUND_COLOR = new Color(0x2B2B2B);
    private static final Color FOREGROUND_COLOR = new Color(0xA9B7C6);
    private final int MAX_ZOOM = 18;
    private final int MIN_ZOOM = 0;


    private int zoom = 12;


    public MySlitherCanvas(MySlitherJFrame view) {
        super();
        this.view = view;

        setBackground(BACKGROUND_COLOR);
        setForeground(FOREGROUND_COLOR);

        addMouseWheelListener(e -> {
            zoom -=e.getWheelRotation();
            zoom = Math.max(zoom, MIN_ZOOM);
            zoom = Math.min(zoom, MAX_ZOOM);
        });
    }
}
