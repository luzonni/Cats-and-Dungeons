package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Laje rachada. Existe para quebrar a repeticao do piso.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternFloorCracked extends Tile {
    CisternFloorCracked(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternFloorCracked");
    }

    @Override
    public void effect(Entity e) {
    }
}
