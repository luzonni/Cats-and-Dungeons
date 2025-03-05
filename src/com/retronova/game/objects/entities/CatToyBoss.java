package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CatToyBoss extends Entity {
    private final BufferedImage[][] sprite;
    private int countAnim;
    private int indexState;
    private int indexAnim;

    CatToyBoss(int ID, double x, double y) {
        super(ID, x, y, 0.4);
        sprite = new BufferedImage[][] {loadSprite("cattoyboss", 0), loadSprite("cattoyboss", 1)};
        //adicionar resistência

    }

    @Override
    public BufferedImage getSprite() {
        return sprite[indexState][indexState];
    }

    @Override
    public void tick() {
        moveIA();
        animation();
        //adicionar dano
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce(0.30, radians);

    }

    public void animation() {
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            indexAnim++;
            if(indexAnim >= sprite[indexState].length) {
                indexAnim = 0;
            }
        }
    }

    @Override
    public void render(Graphics2D c) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, c);
    }

    @Override
    public void dispose() {

    }
}
