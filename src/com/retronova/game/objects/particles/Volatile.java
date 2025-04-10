package com.retronova.game.objects.particles;

import com.retronova.engine.Engine;
import com.retronova.game.Game;

import java.awt.*;

public class Volatile extends Particle {

    private final double dir;
    private int count;
    private int indexAnim;

    public Volatile(String sprite, double x, double y) {
        super(x, y, 0.5);
        loadSprites(sprite);
        this.dir = Engine.RAND.nextDouble()*Math.PI*2;
        setX(getX() - getWidth()/2d);
        setY(getY() - getHeight()/2d);
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
        if(indexAnim >= getSheet().size()) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }
}
