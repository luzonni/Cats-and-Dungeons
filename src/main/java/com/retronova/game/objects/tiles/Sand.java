package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class Sand extends Tile {
    Sand(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("sand");
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setDrag(0.18d);
    }
}
