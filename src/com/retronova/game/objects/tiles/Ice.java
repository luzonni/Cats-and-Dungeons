package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Ice extends Tile {

    Ice(int ID, int x, int y) {
        super(ID, x, y, false);
        loadSprites("ice");
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setFriction(0.2d);
    }

}
