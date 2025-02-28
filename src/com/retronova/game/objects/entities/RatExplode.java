package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.engine.Engine;
import com.retronova.game.objects.GameObject;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RatExplode extends Entity {

    private final BufferedImage[][] sprite;
    private int countAnim;
    private int indexState;
    private int indexAnim;

    private boolean chasing;
    private final int raioExplosao;
    private final double danoExplosao;

    public RatExplode(int ID, double x, double y) {
        super(ID,x,y,0.55);
        this.raioExplosao = GameObject.SIZE();
        this.danoExplosao = 5;
        sprite = new BufferedImage[][]{getSprite("ratexplose", 0), getSprite("ratexplose", 1)};
        //this.chasing = false;
    }

    @Override
    public BufferedImage getSprite() {
        return sprite [indexState][indexAnim];
    }

    public void tick() {
        moveIA();
        animar();
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        double distance = this.getDistance(player);

        if(distance < raioExplosao) {
            explodir(player);

        }else {
            double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
            getPhysical().addForce(1, radians);
        }


        this.indexState = getPhysical().isMoving() ? 1 : 0;

    }

    private void explodir(Player player) {
        System.out.println("Mouse explodiu causando " + danoExplosao + " de dano");
        this.dispose();
    }

    private void animar() {
        countAnim ++;
        if (countAnim > 10) {
            countAnim = 0;
            indexAnim ++;
            if (indexAnim >= sprite[indexState].length) {
                indexAnim = 0;
            }
        }
    }


    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }

    public void dispose() {}


}
