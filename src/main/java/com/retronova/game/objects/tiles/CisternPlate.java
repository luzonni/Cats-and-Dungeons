package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Placa de pressao emperrada.
 *
 * Recortado do DungeonTileset II e repintado por tools/GenCisterna.java.
 */
public class CisternPlate extends Tile {
    CisternPlate(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternPlate");
    }

    @Override
    public void effect(Entity e) {
    }
}
