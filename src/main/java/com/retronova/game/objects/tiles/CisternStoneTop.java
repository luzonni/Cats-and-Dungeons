package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Topo da parede da cisterna: a face que recebe a luz.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternStoneTop extends Tile {
    CisternStoneTop(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternStoneTop");
    }

    @Override
    public void effect(Entity e) {
    }
}
