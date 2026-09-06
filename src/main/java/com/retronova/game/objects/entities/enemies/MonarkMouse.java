package com.retronova.game.objects.entities.enemies;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MonarkMouse extends Enemy {

    private int countAnim;

    public MonarkMouse(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("monarkmouse");
        //adicionar resistência
        setSolid();
    }

    public void tick() {
        moveIA();
        animation();
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce("move", 0.50, radians);
    }

    public void animation() {
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }

    }

    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }

    @Override
    public void dispose() {}
}






