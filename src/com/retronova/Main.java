package com.retronova;

import com.retronova.engine.Engine;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")) {
            System.setProperty("sun.java2d.uiScale", "1.0");
        }
        SwingUtilities.invokeLater(Engine::new);
    }
}
