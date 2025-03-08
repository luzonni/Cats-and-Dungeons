package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;

import java.awt.image.BufferedImage;
import java.util.List;

public class Bomb extends Entity{

    private final double damage;
    private int count;


    public Bomb(double x, double y, double damage) {
        super(-1, x, y, 0.2);
        this.damage = damage;
        setSolid();
        loadSprites("bomb");
    }

    @Override
    public void tick() {
        count++;
        if(count > 60*3) {
            List<Entity> entities = Game.getMap().getEntities();
            for(int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                if(e.getDistance(this) < GameObject.SIZE() * 50.0) {
                    e.strike(AttackTypes.Melee, this.damage);
                }
            }
            this.disappear();
        }
    }

}
