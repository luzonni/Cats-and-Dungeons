package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class KingCursedCatBoss extends Entity {

    private int countAnim;

    KingCursedCatBoss(int ID, double x, double y) {
        super(ID,x,y, 0.3);
        loadSprites("kingcursedcatboss");
    }

    @Override
    public void tick() {
        moveIA();
        animation();

    }

    public void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce(0.50, radians);
    }

    public void animation() {
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    @Override
    public void render(Graphics2D k) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, k);
    }

}
