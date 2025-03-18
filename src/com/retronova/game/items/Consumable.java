package com.retronova.game.items;

import java.awt.*;

public abstract class Consumable extends Item {


    private int stack;

    Consumable(int id, String name, String sprite){
        super(id, name, sprite);
        this.stack = 1;
    }

    public abstract void consume();

    public void setStack(int stack) {
        this.stack = stack;
    }

    public int getStack() {
        return this.stack;
    }

    public void tick(){

    }

    @Override
    public void render(Graphics2D g) {

    }
}
