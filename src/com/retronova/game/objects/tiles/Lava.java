package com.retronova.game.objects.tiles;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Lava extends Tile {

    private static BufferedImage[] sprite;
    private int indexSprite;
    private int count;
    private int strickCount;

    Lava(int ID, int x, int y) {
        super(ID, x, y, false);
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
            e.strike(AttackTypes.Fire, 10);
            strickCount = 0;
        }
    }

}
