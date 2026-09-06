package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class Ice extends Tile {

    Ice(int id, int x, int y, boolean solid) {
        super(id, x, y, solid);
        loadSprites("ice");
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setRoughness(0.2d);
    }

}
