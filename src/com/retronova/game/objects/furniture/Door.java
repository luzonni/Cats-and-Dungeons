package com.retronova.game.objects.furniture;

public class Door extends Furniture {

    private boolean opened;

    Door(int ID, double x, double y) {
        super(ID, x, y);
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
