package com.retronova.game.objects.entities.furniture;

/**
 * Corrente presa à parede. Sugere que algo foi contido aqui.
 */
public class Chain extends Furniture {

    public Chain(int ID, double x, double y) {
        super(ID, x, y, 1000, false);
        loadSprites("chain");
        setGroundObject();
    }

    @Override
    public void tick() {
    }
}
