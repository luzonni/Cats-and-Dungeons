package com.retronova.game.items;

public abstract class Consumable extends Item {

    Consumable(int id, String name, String sprite){
        super(id, name, sprite);
    }

    public abstract void consume();

    public void tick(){};

}
