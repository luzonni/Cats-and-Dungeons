package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Caveira caida no piso.
 *
 * Recortado do DungeonTileset II e repintado por tools/GenCisterna.java.
 */
public class CisternSkull extends Tile {
    CisternSkull(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternSkull");
    }

    @Override
    public void effect(Entity e) {
    }
}
