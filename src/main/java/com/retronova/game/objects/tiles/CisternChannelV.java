package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Canaleta reta, no sentido norte-sul.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternChannelV extends Tile {
    CisternChannelV(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternChannelV");
    }

    @Override
    public void effect(Entity e) {
    }
}
