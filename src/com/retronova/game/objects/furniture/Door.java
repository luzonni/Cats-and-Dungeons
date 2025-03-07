package com.retronova.game.objects.furniture;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Door extends Furniture {

    private static BufferedImage[] sprites;
    private boolean opened;

    Door(int ID, double x, double y) {
        super(ID, x, y);
        if(sprites == null) {
            sprites = loadSprite("door");
        }
    }

    @Override
    public BufferedImage getSprite() {
        return sprites[opened ? 1 : 0];
    }

    public void open() {
        this.opened = true;
    }

    public boolean isOpened() {
        return this.opened;
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    @Override
    public void dispose() {
        sprites = null;
    }
}
