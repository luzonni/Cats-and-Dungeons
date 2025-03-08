package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class Stone extends Tile {

    Stone(int id, int x, int y) {
        super(id, x, y, false);
        loadSprites("stone");
    }

    @Override
    public void effect(Entity e) {

    }

}
