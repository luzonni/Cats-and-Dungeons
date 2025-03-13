package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;

import java.awt.*;

public class Spark extends Particle {

    private int count;
    private double r;

    public Spark(double x, double y, double seconds) {
        super(x, y, seconds);
    }

    @Override
    public void tick() {
        count++;
        r += Math.toRadians(Engine.RAND.nextInt(48));
        setX(getX() + Math.cos(r) * 2.1d);
        setY(getY() - 3d);
        if(count > getSeconds()) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() - Game.C.getY();
        int size = Configs.SCALE;
        g.setColor(new Color(255, 0,0, 255 - (int)(255 * (count/getSeconds()))));
        g.fillRect(x, y, size, size);
    }
}
