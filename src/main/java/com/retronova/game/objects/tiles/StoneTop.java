package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Face superior da parede de pedra. Sólida.
 *
 * É a superfície que recebe a luz de cima, por isso é o tile claro do par. Vem
 * sempre acima de {@link StoneFace}: as duas juntas formam uma parede com
 * volume, em vez de uma faixa chapada.
 */
public class StoneTop extends Tile {
    StoneTop(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("stoneTop");
    }

    @Override
    public void effect(Entity e) {
    }
}
