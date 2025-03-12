package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.exceptions.NotFound;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Item {

    public static Item build(ItemIDs id) {
        int i = id.ordinal();
        switch (id) {
            case Silk -> {
                return new ItemSilk(i);
            }
            case Sword -> {
                return new Sword(i);
            }
            case Bow -> {
                return new Bow(i);
            }
            case Bomb -> {
                return new ItemBomb(i);
            }
            case Feed -> {
                return new Feed(i);
            }

        }
        throw new NotFound("Item not found");
    }

    private final int id;
    private final String name;
    private final BufferedImage[] sprite;
    private int indexSprite;

    private String[] specifications;

     Item(int id, String name, String sprite) {
        this.id = id;
        this.name = name;
        SpriteSheet sheet = new SpriteSheet("items", sprite, Configs.SCALE);
        int length = sheet.getWidth()/16;
        this.sprite = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            this.sprite[i] = sheet.getSpriteWithIndex(i, 0);
        }
        addSpecifications("Teste1", "Teste2", "Teste3", "Teste4", "Teste5");
    }

    protected void addSpecifications(String... specifications) {
         this.specifications = specifications;
    }

    public String[] getSpecifications() {
         return this.specifications;
    }

    public int getID() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public BufferedImage getSprite() {
        return this.sprite[indexSprite];
    }

    protected void plusIndexSprite() {
        this.indexSprite++;
        if(indexSprite > sprite.length-1) {
            indexSprite = 0;
        }
    }

    protected void resetIndexSprite() {
        this.indexSprite = 0;
    }

    public abstract void tick();

    public abstract void render(Graphics2D g);

}
