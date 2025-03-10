package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class Ceramics extends Tile {

    Ceramics(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("ceramics");
    }

    @Override
    public void effect(Entity e) {

    }
}
