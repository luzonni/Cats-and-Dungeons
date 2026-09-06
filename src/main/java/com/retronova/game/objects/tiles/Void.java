package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class Void extends Tile {

    Void(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("void");
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setRoughness(0d);
    }
}
