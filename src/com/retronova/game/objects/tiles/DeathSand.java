package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DeathSand extends Tile {

    private static BufferedImage[] sprites;
    private int index = 0;

    DeathSand(int id, int x, int y) {
        super(id, x, y, false);
        if(sprites == null)
            sprites = this.loadSprite("deathSand", 0);
        index = Engine.RAND.nextInt(sprites.length);
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setFriction(0.9);
    }

    @Override
    public BufferedImage getSprite() {
        return sprites[index];
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    @Override
    public void dispose() {

    }
}
