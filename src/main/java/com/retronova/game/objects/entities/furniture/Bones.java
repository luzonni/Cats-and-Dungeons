package com.retronova.game.objects.entities.furniture;

/**
 * Ossada no chão. Conta, sem uma linha de texto, que alguém não voltou.
 */
public class Bones extends Furniture {

    public Bones(int ID, double x, double y) {
        super(ID, x, y, 1000, false);
        loadSprites("bones");
        setGroundObject();
    }

    @Override
    public void tick() {
    }
}
