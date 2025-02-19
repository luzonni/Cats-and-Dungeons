package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Zombie extends Entity {

    private final BufferedImage[][] sprite;
    private int countAnim;
    private int indexState;
    private int indexAnim;

    public Zombie(int ID, double x, double y) {
        super(ID, x, y, 0.6);
        sprite = new BufferedImage[][] {getSprite("zombie", 0), getSprite("zombie", 1)};

        // Zumbi não deve ter resistencia a nada não eu acho, que eles são lascado já né.
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

    public void takeDamage(AttackTypes attackType, double baseDamage) {
        strike(attackType, baseDamage); // Aplica o dano com a strike do entity
        System.out.println("Zombier tomou " + baseDamage + " de de dano de " + attackType + ". Vida restante: " + getLife());
        // TODO fazer tanto no skeleton como no zombie, o impacto do item que vai dar dano, e tirar essa bosta que aparece no terminal quando toma dano.

    }

    @Override
    public void render(Graphics2D g) {
        //TODO ajeitar esse sistema... está meio quebrado, estou espirrando pra krl e sem paciencia...
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
    }

    @Override
    public void dispose() {
    }

}
