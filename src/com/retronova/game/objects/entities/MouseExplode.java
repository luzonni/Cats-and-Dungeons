package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseExplode extends Entity {

    private int countAnim;


    //private final int raioExplosao;
    private final double danoExplosao;

    // TODO: Entender pq a sprite ta nervosa virando de um lado pro outro sem motivo
    MouseExplode(int ID, double x, double y) {
        super(ID,x,y,0.5);
        this.danoExplosao = 5;
        loadSprites("ratexplode");
        setSolid();
        setAlive();
    }

    public void tick() {
        moveIA();
        animar();
    }

    public void moveIA() {
        Player player = Game.getPlayer();

        if(this.getDistance(player) < GameObject.SIZE()) {
            explodir(player);
            getPhysical().addForce(0,0);
        }else {
            double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
            getPhysical().addForce(getSpeed(), radians);
        }

    }

    private void explodir(Player player) {
        System.out.println("Mouse explodiu causando " + danoExplosao + " de dano");
        player.strike(AttackTypes.Explosion, danoExplosao);
        disappear();
    }

    private void animar() {
        countAnim ++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }


    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }

}
