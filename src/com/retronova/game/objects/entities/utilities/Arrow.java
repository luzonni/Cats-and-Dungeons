package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.util.List;

public class Arrow extends Utility {

    private final double damage;
    private double angle;
    private final Entity shooter;

    public Arrow(double x, double y, double damage, double angle, Entity shooter) {
        super(-1, x, y, 0);
        this.damage = damage;
        this.angle = angle;
        this.shooter = shooter;
        loadSprites("arrow");
        setSpeed(7d);
        this.getPhysical().addForce("show", getSpeed(), angle);
    }

    @Override
    public void tick() {
        //getPhysical().addForce("flying", 4, this.angle);
        if(getPhysical().crashing()) {
            this.disappear();
        }
        List<Entity> entities = Game.getMap().getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if(entity.equals(shooter))
                continue;
            if(this.colliding(entity)) {
                entity.strike(AttackTypes.Piercing, damage);
                entity.getPhysical().addForce("knockback", 2.2, this.angle);
                entity.addEffect("poison", (Entity e) -> {
                    //Quando a flecha colide com uma entidade, ela fica com efeito de dano de veneno. APENAS TESTE!
                    e.strike(AttackTypes.Poison, 0.1d);
                }, 3);
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
