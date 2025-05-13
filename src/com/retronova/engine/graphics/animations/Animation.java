package com.retronova.engine.graphics.animations;


import java.awt.*;

public abstract class Animation {

    public abstract void start();

    public abstract void tick();

    public abstract void render(Graphics2D graphics);

}
