package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

public class DarkBriks extends Tile {

    DarkBriks(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("darkBricks");
        getSheet().setIndex(Engine.RAND.nextInt(2));
    }

    @Override
    public void effect(Entity e) {

    }
}
