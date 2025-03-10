package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class Stone extends Tile {

    Stone(int id, int x, int y, boolean solid) {
        super(id, x, y, solid);
        loadSprites("stone");
    }

    @Override
    public void effect(Entity e) {

    }

}
