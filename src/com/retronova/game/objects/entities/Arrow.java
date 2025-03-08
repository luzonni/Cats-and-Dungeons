package com.retronova.game.objects.entities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;

import java.awt.*;
import java.util.List;

public class Arrow extends Entity {

    private final double damage;
    private final double angle;

    public Arrow(double x, double y, double damage, double angle) {
        super(-1, x, y, 0);
        this.damage = damage;
        this.angle = angle;
        loadSprites("arrow");
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
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() - Game.C.getY();
        Rotate.draw(getSprite(), x, y, angle + Math.PI/4, null, g);
    }

}
