package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.util.List;

public class ThrownFurball extends Utility{
    private Enemy lastEnemy;
    private double angle;
    public ThrownFurball(double x, double y, double angle) {
        super(x, y, 0);
        loadSprites("furball");
        getPhysical().addForce("movement", 3, angle);
    }

    @Override
    public void tick() {
        angle += Math.PI / 12;
        if(getPhysical().crashing()){
            disappear();
        }
        List<Enemy> entities = Game.getMap().getEntities(Enemy.class);
        for(int i = 0; i < entities.size(); i++){
            Enemy enemy = entities.get(i);
            if(!enemy.equals(lastEnemy) && enemy.colliding(this)){
                enemy.strike(AttackTypes.Piercing, 1);
                this.lastEnemy = enemy;
            }

        }
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.draw(getSprite(), (int)getX(), (int)getY(), angle, null, g);
    }
}
