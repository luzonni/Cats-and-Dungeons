package com.retronova;

import com.retronova.engine.Engine;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DPIFix.fixDPI();
        SwingUtilities.invokeLater(Engine::new);
    }
}
