package com.retronova.game.objects.entities;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Arrow extends Entity{
    private final double damage;
    private final double angle;
    private BufferedImage[] sprite;

    public Arrow(double x, double y, double damage, double angle) {
        super(-1, x, y, 0);
        this.damage = damage;
        this.angle = angle;
        sprite = getSprite("arrow");
    }

    @Override
    public BufferedImage getSprite() {
        return this.sprite[0];

    }

    @Override
    public void tick() {
        getPhysical().addForce(4, this.angle);
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(),g );
    }

    @Override
    public void dispose() {

    }
}
