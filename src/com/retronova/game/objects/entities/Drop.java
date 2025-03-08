package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Drop extends Entity {

    private final Item item;

    public Drop(double x, double y, Item item) {
        super(-1, x, y, 0.2);
        this.item = item;
    }

    @Override
    public BufferedImage getSprite() {
        return item.getSprite();
    }

    @Override
    public void tick() {
        if(Mouse.clickOnMap(Mouse_Button.LEFT, getBounds(), Game.C)) {
            if(Game.getPlayer().getInventory().give(this.item))
                die();
        }
    }
}
