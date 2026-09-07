package com.retronova.game.objects.entities.furniture;

/**
 * Entulho de pedra. Set dressing barato, espalhado para quebrar a repetição do
 * piso e sugerir ruína.
 *
 * Atravessável de propósito: são cacos no chão, não uma barreira. Sólido, cada
 * peça virava uma parede invisível e a sala virava labirinto.
 */
public class Rubble extends Furniture {

    public Rubble(int ID, double x, double y) {
        super(ID, x, y, 1000, false);
        loadSprites("rubble");
        setGroundObject();
    }

    @Override
    public void tick() {
    }
}
