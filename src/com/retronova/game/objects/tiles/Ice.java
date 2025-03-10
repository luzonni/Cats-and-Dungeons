package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Ice extends Tile {

    Ice(int id, int x, int y, boolean solid) {
        super(id, x, y, solid);
        loadSprites("ice");
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setFriction(0.2d);
    }

}
