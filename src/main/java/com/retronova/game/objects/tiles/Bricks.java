package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class Bricks extends Tile {

    Bricks(int id, int x, int y, boolean solid) {
        super(id, x, y, solid);
        loadSprites("brick");
    }

    @Override
    public void effect(Entity e) {

    }

}
