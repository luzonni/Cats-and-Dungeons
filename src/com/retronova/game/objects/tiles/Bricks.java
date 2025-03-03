package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Bricks extends Tile {

    private static BufferedImage[] sprites;
    private int index = 0;

    Bricks(int id, int x, int y) {
        super(id, x, y, true);
        if(Bricks.sprites == null)
            Bricks.sprites = this.getSprite("brick", 1);
        index = Engine.RAND.nextInt(Bricks.sprites.length);
    }

    @Override
    public BufferedImage getSprite() {
        return sprites[0];
    }

    @Override
    public void effect(Entity e) {

    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(sprites[index], g);
    }

    @Override
    public void dispose() {
        Bricks.sprites = null;
    }

}
