package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Canaleta reta, no sentido leste-oeste.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternChannelH extends Tile {
    CisternChannelH(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternChannelH");
    }

    @Override
    public void effect(Entity e) {
    }
}
