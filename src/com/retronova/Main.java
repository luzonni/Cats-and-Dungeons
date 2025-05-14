package com.retronova;

import com.retronova.engine.Engine;

public class Main {
    public static void main(String[] args) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            DPIFix.setDPI();
        }
        new Engine();
    }
}
