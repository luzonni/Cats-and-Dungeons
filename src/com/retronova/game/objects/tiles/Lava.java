package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

import java.awt.image.BufferedImage;

public class Lava extends Tile {

    private static BufferedImage[] sprite;
    private int indexSprite;
    private int count;
    private int strickCount;

    Lava(int id, int x, int y, boolean solid) {
        super(id, x, y, solid);
        loadSprites("lava");
    }

    @Override
    public void tick() {
        count++;
        if(count > 20) {
            count = 0;
            getSheet().plusIndex();
        }
    }

    @Override
    public void effect(Entity e) {
        strickCount++;
        if(strickCount > 30) {
            strickCount = 0;
            e.EFFECT_FIRE(3, 6);
        }
        e.getPhysical().setRoughness(0.85d);
    }

}
