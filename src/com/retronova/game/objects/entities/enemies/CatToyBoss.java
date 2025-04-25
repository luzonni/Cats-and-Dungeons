package com.retronova.game.objects.entities.enemies;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CatToyBoss extends Enemy {

    private int countAnim;

    public CatToyBoss(int ID, double x, double y) {
        super(ID, x, y, 150);
        loadSprites("cattoyboss");
        //adicionar resistência

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
        getPhysical().addForce("move", 0.30, radians);

    }

    public void animation() {
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
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
