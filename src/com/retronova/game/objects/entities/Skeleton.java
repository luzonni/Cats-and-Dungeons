package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Skeleton extends Entity {

    private final BufferedImage[][] sprite;
    private int countAnim;
    private int indexState;
    private int indexAnim;

    public Skeleton(int ID, double x, double y) {
        super(ID, x, y, 0.8);
        sprite = new BufferedImage[][] {getSprite("skeleton", 0), getSprite("skeleton", 1)};
    }

    @Override
    public BufferedImage getSprite() {
        return sprite[indexState][indexAnim];
    }

    public void tick() {
        super.tick();
        moveIA();
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            indexAnim++;
            if(indexAnim >= sprite[indexState].length) {
                indexAnim = 0;
            }
        }
    }

    private void moveIA() {
        Player player = Game.getPlayer();
        if(Math.sqrt(Math.pow((player.getX() - getX()), 2) + Math.pow(player.getY() - getY(), 2)) < GameObject.SIZE()*4) {
            double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
            getPhysical().addForce(Engine.SCALE, radians);
        }
        this.indexState = getPhysical().isMoving() ? 1 : 0;
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    @Override
    public void dispose() {
    }

}
