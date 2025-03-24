package com.retronova.game.objects.particles;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Alpha;
import com.retronova.game.Game;

import java.awt.*;

public class Poison extends Particle {

    private final double dir;
    private int count;
    private int indexAnim;

    public Poison(double x, double y, double seconds, double dir) {
        super(x, y, seconds);
        loadSprites("poison");
        this.dir = dir;
        if (Engine.RAND.nextBoolean())
            getSheet().plusIndex();
    }

    @Override
    public void tick() {
        setX(getX() + Math.cos(dir) * 0.5d);
        setY(getY() + Math.sin(dir) * 0.5d);
        count++;
        if (count >= 7) {
            count = 0;
            indexAnim++;
            getSheet().setIndex(indexAnim);
        }
        if(indexAnim >= 8) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        float t = 1f - count / (float) getSeconds();
        int x = (int) getX() - getWidth()/2 - Game.C.getX();
        int y = (int) getY() - getHeight()/2 - Game.C.getY();
        Alpha.draw(getSprite(), x, y, 1, g);
    }
}