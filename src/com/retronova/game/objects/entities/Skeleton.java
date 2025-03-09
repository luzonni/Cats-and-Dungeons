package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Skeleton extends Entity {

    private int countAnim;

    Skeleton(int ID, double x, double y) {
        super(ID, x, y, 0.5);
        loadSprites("mouseskeleton");

        addResistances(AttackTypes.Fire, 0.5);
        addResistances(AttackTypes.Poison, 0.8);
        addResistances(AttackTypes.Piercing, 1);
        setSolid();
        setAlive();
    }

    public void tick() {
        moveIA();
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    private void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce(1, radians);
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
    }

}
