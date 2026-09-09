package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Frasco deixado para tras.
 *
 * Recortado do DungeonTileset II e repintado por tools/GenCisterna.java.
 */
public class CisternFlask extends Tile {
    CisternFlask(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternFlask");
    }

    @Override
    public void effect(Entity e) {
    }
}
