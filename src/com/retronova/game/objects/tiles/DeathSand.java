package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

public class DeathSand extends Tile {

    DeathSand(int id, int x, int y) {
        super(id, x, y, false);
        loadSprites("deathSand");
    }

    @Override
    public void effect(Entity e) {
        e.getPhysical().setFriction(0.9);
    }

}
