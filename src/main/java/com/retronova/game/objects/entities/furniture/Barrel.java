package com.retronova.game.objects.entities.furniture;

/**
 * Barril de madeira. Sugere provisões — alguém se abasteceu aqui.
 */
public class Barrel extends Furniture {

    public Barrel(int ID, double x, double y) {
        super(ID, x, y, 1000, true);
        loadSprites("barrel");
    }

    @Override
    public void tick() {
    }
}
