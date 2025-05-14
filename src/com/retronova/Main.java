package com.retronova;

import com.retronova.engine.Engine;

public class Main {
    public static void main(String[] args) {
        DPIFix.fixDPI();
        new Engine();
    }
}
