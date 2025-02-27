package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.graphics.SpriteSheet;

import java.awt.image.BufferedImage;

public abstract class Item {

    private String name;
    private BufferedImage[] sprite;
    private int indexSprite;
    private int amount;

    Item(String name, String sprite) {
        this.name = name;
        SpriteSheet sheet = new SpriteSheet("items", sprite, Configs.SCALE);
        int length = sheet.getWidth()/16;
        this.sprite = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            this.sprite[i] = sheet.getSprite(i, 0);
        }
    }

    public BufferedImage getSprite() {
        return this.sprite[indexSprite];
    }

    public void tick() {

    }


}
