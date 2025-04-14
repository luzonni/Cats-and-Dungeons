package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

public class Ceramics extends Tile {

    Ceramics(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("ceramics");
        getSheet().setIndex(Engine.RAND.nextInt(getSheet().size()));
    }

    @Override
    public void effect(Entity e) {

    }
}
