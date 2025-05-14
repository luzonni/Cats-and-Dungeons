package com.retronova;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class DPIFix {
    interface User32 extends Library {
        boolean SetProcessDPIAware();
    }

    public static void fixDPI() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                User32 user32 = Native.load("user32", User32.class);
                boolean result = user32.SetProcessDPIAware();
                System.out.println("SetProcessDPIAware: " + result);
            } catch (UnsatisfiedLinkError | Exception e) {
                System.err.println("Não foi possível aplicar o fix de DPI: " + e.getMessage());
            }
        }
    }
}
