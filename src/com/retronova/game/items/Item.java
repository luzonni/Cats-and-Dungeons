package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.exceptions.NotFound;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Item {

    public static Item build(ItemIDs id) {
        int i = id.ordinal();
        switch (id) {
            case Silk -> {
                return new Silk(i);
            }
            case Sword -> {
                return new Sword(i);
            }
            case Bow -> {
                return new Bow(i);
            }
        }
        throw new NotFound("Item not found");
    }

    private String name;
    private BufferedImage[] sprite;
    private int indexSprite;

    Item(int id, String name, String sprite) {
        this.name = name;
        SpriteSheet sheet = new SpriteSheet("items", sprite, Configs.SCALE);
        int length = sheet.getWidth()/16;
        this.sprite = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            this.sprite[i] = sheet.getSpriteWithIndex(i, 0);
        }
    }

    public String getName() {
        return name;
    }

    public BufferedImage getSprite() {
        return this.sprite[indexSprite];
    }

    public abstract void tick();

    public abstract void render(Graphics2D g);

}
