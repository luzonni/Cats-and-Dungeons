package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Grade redonda na parede: por onde a agua entrou.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternGrate extends Tile {
    CisternGrate(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternGrate");
    }

    @Override
    public void effect(Entity e) {
    }
}
