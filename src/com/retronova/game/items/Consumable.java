package com.retronova.game.items;

import com.retronova.engine.Engine;

import java.awt.*;

public abstract class Consumable extends Item {

    Consumable(int id, String name, String sprite){
        super(id, name, sprite);
    }

    public abstract void consume();

    public void tick(){

    }

    @Override
    public void render(Graphics2D g) {

    }
}
