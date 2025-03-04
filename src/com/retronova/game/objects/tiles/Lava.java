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
        if(sprite == null) {
            sprite = loadSprite("lava");
            indexSprite = Engine.RAND.nextInt(sprite.length);
        }
    }

    @Override
    public void tick() {
        count++;
        if(count > 20) {
            count = 0;
            indexSprite++;
            if(indexSprite > sprite.length-1) {
                indexSprite = 0;
            }
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

    @Override
    public BufferedImage getSprite() {
        return sprite[indexSprite];
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    @Override
    public void dispose() {
        sprite = null;
    }
}
