package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Laje seca da plataforma central.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternFloor extends Tile {
    CisternFloor(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternFloor");
    }

    @Override
    public void effect(Entity e) {
    }
}
