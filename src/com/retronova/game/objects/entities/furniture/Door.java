package com.retronova.game.objects.entities.furniture;

public class Door extends Furniture {

    private boolean opened;

    public Door(int ID, double x, double y) {
        super(ID, x, y, 1000);
        loadSprites("door");
    }

    public void open() {
        this.opened = true;
    }

    public boolean isOpened() {
        return this.opened;
    }

    @Override
    public void tick() {

    }
}
