package com.retronova.game.objects.furniture;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Door extends Furniture {

    public Door(int ID, double x, double y) {
        super(ID, x, y);
    }

    @Override
    public BufferedImage getSprite() {
        return null;
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {

    }

    @Override
    public void dispose() {

    }
}
