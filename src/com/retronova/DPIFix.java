package com.retronova;

public class DPIFix {
    static {
        System.loadLibrary("dpifix");
    }
    public static native void setDPI();
}
