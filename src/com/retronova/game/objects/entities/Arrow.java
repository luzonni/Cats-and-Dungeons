package com.retronova.game.objects.entities;

import com.retronova.game.Game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Arrow extends Entity {

    private final double damage;
    private final double angle;

    private static BufferedImage[] sprite;

    public Arrow(double x, double y, double damage, double angle) {
        super(-1, x, y, 0);
        this.damage = damage;
        this.angle = angle;
        if(sprite == null) {
            sprite = loadSprite("arrow");
        }
    }

    @Override
    public BufferedImage getSprite() {
        return sprite[0];
    }

    @Override
    public void tick() {
        getPhysical().addForce(4, this.angle);
        if(getPhysical().crashing()) {
            this.disappear();
        }
        List<Entity> entities = Game.getMap().getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if(entity instanceof Player)
                continue;
            if(this.colliding(entity) && entity.isAlive()) {
                entity.strike(AttackTypes.Piercing, damage);
                entity.getPhysical().addForce(2.2, this.angle);
                this.disappear();
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        int middleX = (int)getX() + getWidth()/2;
        int middleY = (int)getY() + getHeight()/2;
        g.rotate((angle + Math.PI/4), middleX - Game.C.getX(), middleY - Game.C.getY());
        renderSprite(getSprite(),g );
        g.rotate(-(angle + Math.PI/4), middleX - Game.C.getX(), middleY - Game.C.getY());
    }

    @Override
    public void dispose() {
        sprite = null;
    }
}
