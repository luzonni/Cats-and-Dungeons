package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Agua parada da cisterna. Rasa: molha o passo, nao barra.
 *
 * Recortado do pacote Puny Dungeon por tools/GenCisterna.java.
 */
public class CisternWater extends Tile {
    CisternWater(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("cisternWater");
    }

    @Override
    public void effect(Entity e) {
    }
}
