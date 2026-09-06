package com.retronova.game.objects.particles;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Alpha;
import com.retronova.game.Game;

import java.awt.*;

public class Smoke extends Particle {

    private int count;

    public Smoke(double x, double y, double seconds, double dir) {
        super(x, y, seconds);
        loadSprites("smoke");
        if(Engine.RAND.nextBoolean())
            getSheet().plusIndex();
    }

    @Override
    public void tick() {
        count++;
        if(count >= getSeconds()) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        float t = 1f - count/(float)getSeconds();
        int x = (int)getX();
        int y = (int)getY();
        Alpha.draw(getSprite(), x, y, t, g);
    }
}
