package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * O ralo no piso: para onde as canaletas correm.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternSink extends Tile {
    CisternSink(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternSink");
    }

    @Override
    public void effect(Entity e) {
    }
}
