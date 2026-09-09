package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Laje gasta, a mais escura das tres.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternFloorWorn extends Tile {
    CisternFloorWorn(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternFloorWorn");
    }

    @Override
    public void effect(Entity e) {
    }
}
