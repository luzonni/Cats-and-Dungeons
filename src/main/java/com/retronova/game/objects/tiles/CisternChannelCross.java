package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Cruzamento das duas canaletas.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternChannelCross extends Tile {
    CisternChannelCross(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternChannelCross");
    }

    @Override
    public void effect(Entity e) {
    }
}
