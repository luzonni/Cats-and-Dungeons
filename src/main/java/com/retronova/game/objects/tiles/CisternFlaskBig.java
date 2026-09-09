package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Frasco grande, do mesmo despojo.
 *
 * Recortado do DungeonTileset II e repintado por tools/GenCisterna.java.
 */
public class CisternFlaskBig extends Tile {
    CisternFlaskBig(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternFlaskBig");
    }

    @Override
    public void effect(Entity e) {
    }
}
