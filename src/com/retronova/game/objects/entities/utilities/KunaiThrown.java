package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.util.List;

public class KunaiThrown extends Utility {


    private final double damage;
    private final double direction;
    private final Point spriteRotatePosition;

    public KunaiThrown(double x, double y, double damage, double direction) {
        super(x, y, 2);
        this.spriteRotatePosition = new Point((int)(2.5* Configs.GameScale()), (int)(13.5*Configs.GameScale()));
        this.damage = damage;
        this.direction = direction;
        loadSprites("kunai");
        getPhysical().addForce("moving", 8, this.direction);
    }

    @Override
    public void tick() {
        List<Enemy> enemies = Game.getMap().getEntities(Enemy.class);
        for(Enemy enemy : enemies) {
            if(this.colliding(enemy)) {
                this.disappear();
                enemy.strike(AttackTypes.Piercing, this.damage);
            }
        }
        if(!getPhysical().isMoving() || getPhysical().crashing()) {
            disappear();
        }
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.draw(getSprite(), (int)getX() - Game.C.getX(), (int)getY() - Game.C.getY(), this.direction + Math.PI/4d, spriteRotatePosition, g);
    }
}
