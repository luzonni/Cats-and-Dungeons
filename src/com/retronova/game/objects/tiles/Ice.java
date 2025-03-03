package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Ice extends Tile {

    private static BufferedImage[] sprite;
    private int indexSprite;

    Ice(int ID, int x, int y) {
        super(ID, x, y, false);
        if(sprite == null) {
            sprite = getSprite("ice");
            indexSprite = Engine.RAND.nextInt(sprite.length);
        }
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setFriction(0.2d);
    }

    @Override
    public BufferedImage getSprite() {
        return sprite[indexSprite];
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    @Override
    public void dispose() {
        sprite = null;
    }
}
