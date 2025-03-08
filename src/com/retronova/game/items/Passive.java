package com.retronova.game.items;

import java.awt.*;

public abstract class Passive extends Item {

    Passive(int id, String name, String sprite){
        super(id, name, sprite);
    }

    public abstract void apply();


    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {

    }
}
