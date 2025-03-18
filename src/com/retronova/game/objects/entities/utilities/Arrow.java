package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Effect;
import com.retronova.game.objects.entities.EffectApplicator;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.util.List;

public class Arrow<T extends Entity> extends Utility {

    private final double damage;
    private final double angle;
    private final Class<T> type;
    private EffectApplicator effect;

    public Arrow(double x, double y, double damage, double angle, Class<T> type) {
        super(-1, x, y, 0);
        this.damage = damage;
        this.angle = angle;
        this.type = type;
        loadSprites("arrow");
        setSpeed(7d);
        this.getPhysical().addForce("shot", getSpeed(), angle);
    }

    public Arrow(double x, double y, double damage, double angle, Class<T> type, EffectApplicator effect) {
        super(-1, x, y, 0);
        this.damage = damage;
        this.angle = angle;
        this.type = type;
        this.effect = effect;
        loadSprites("arrow");
        setSpeed(7d);
        this.getPhysical().addForce("shot", getSpeed(), angle);
    }

    @Override
    public void tick() {
        if(getPhysical().crashing()) {
            this.disappear();
        }
        List<T> entities = Game.getMap().getEntities(type);
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if(this.colliding(entity)) {
                entity.strike(AttackTypes.Piercing, damage);
                entity.getPhysical().addForce("knockback", 2.2, this.angle);
                if(this.effect != null)
                    entity.addEffect("arrowEffect", effect, 1);
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
