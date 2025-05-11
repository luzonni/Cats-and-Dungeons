package com.retronova.engine.graphics.animations;


import java.awt.*;

public abstract class Animation {

    private double rad, rotate;

    private void setRotate(double rotate) {
        this.rotate = rotate;
    }

    public abstract void start();

    public abstract void tick();

    public abstract void render(Graphics2D graphics);

}
