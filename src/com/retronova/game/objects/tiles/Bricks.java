package com.retronova.game.objects.tiles;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Bricks extends Tile {

    private static BufferedImage[] sprites;

    Bricks(int id, int x, int y) {
        super(id, x, y, true);
        if(Bricks.sprites == null)
            Bricks.sprites = this.getSprite("brick");
    }

    @Override
    public BufferedImage getSprite() {
        return sprites[0];
    }

    @Override
    public void tick() {
        //Logic tile
        //codes here usually is deprecated to optimization.
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(sprites[0], g);
    }

    @Override
    public void dispose() {
        Bricks.sprites = null;
    }
}
