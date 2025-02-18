package com.retronova.game.objects.entities;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Zombie extends Entity {

    private BufferedImage[] sprite;

    public Zombie(int ID, double x, double y) {
        super(ID, x, y, 1);
        sprite = getSprite("zombie");
    }

    @Override
    public BufferedImage getSprite() {
        return sprite[0];
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(sprite[0], g);
    }

    @Override
    public void dispose() {
    }

}
