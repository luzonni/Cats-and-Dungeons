package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Face da parede da cisterna, virada para dentro.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternStoneFace extends Tile {
    CisternStoneFace(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternStoneFace");
    }

    @Override
    public void effect(Entity e) {
    }
}
