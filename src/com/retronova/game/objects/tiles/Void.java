package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Entity;

public class Void extends Tile {

    private int count;

    Void(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("void");
    }

    @Override
    public void tick() {
        count++;
        if(count > 5 + Engine.RAND.nextInt(10)) {
            count = 0;
            getSheet().plusIndex();
        }
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setFriction(0d);
    }
}
