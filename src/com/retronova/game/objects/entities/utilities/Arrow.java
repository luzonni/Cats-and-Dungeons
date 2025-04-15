package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.ActionBack;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Effect;
import com.retronova.game.objects.entities.EffectApplicator;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.util.List;

public class Arrow extends Utility {

    private final double angle;
    private final EffectApplicator action;

    public Arrow(double x, double y, double angle, EffectApplicator action) {
        super(-1, x, y, 0);
        this.angle = angle;
        this.action = action;
        loadSprites("arrow");
        setSpeed(7d);
        this.getPhysical().addForce("shot", getSpeed(), angle);
    }

    @Override
    public void tick() {
        if(getPhysical().crashing()) {
            this.disappear();
        }
        List<Enemy> entities = Game.getMap().getEntities(Enemy.class);
        for(int i = 0; i < entities.size(); i++) {
            Enemy entity = entities.get(i);
            if(this.colliding(entity)) {
                action.effect(entity);
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
