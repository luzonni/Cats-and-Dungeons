package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Drop extends Utility {

    private final Item item;
    private double backR;
    private double itemR;
    private int count;

    public Drop(double x, double y, Item item) {
        super(-1, x, y, 0.2);
        this.item = item;
        loadSprites("drop");
    }

    @Override
    public void tick() {
        if(Mouse.clickOnMap(Mouse_Button.LEFT, getBounds(), Game.C)) {
            if(Game.getPlayer().getInventory().give(this.item))
                disappear();
        }
        count++;
        if(count > 7) {
            backR += 0.025d;
            itemR += 0.05d;
            if(backR > Math.PI*2) {
                backR = 0;
            }
            if(itemR > Math.PI*2) {
                itemR = 0;
            }
        }
    }

    private double getItemR() {
        double x = (getX() + getWidth()/2d) - (getX() + getWidth()/2d + Math.cos(itemR) * getWidth()/2);
        double y = (getY() + getHeight()/2d) - getY();
        return Math.atan2(y, x) - Math.PI/2;
    }

    @Override
    public void render(Graphics2D g) {
        int x = (int) getX() - Game.C.getX();
        int y = (int)getY() - Game.C.getY();
        BufferedImage sprite = getSprite();
        BufferedImage itemSprite = item.getSprite();
        Rotate.draw(sprite, x, y, backR, null, g);
        Rotate.draw(itemSprite, x, y, getItemR(), null, g);
    }

}
