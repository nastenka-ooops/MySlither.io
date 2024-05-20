package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setProperty("sun.java2d.opengl", "true");
        new MySlitherJFrame().setVisible(true);
    }
}
