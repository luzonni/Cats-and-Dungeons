package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.EffectApplicator;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import java.awt.*;
import java.util.List;

public class Arrow extends Utility {

    private final double angle;
    private final EffectApplicator action;

    public Arrow(double x, double y, double angle, EffectApplicator action) {
        super(x, y, 0);
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
        List<Entity> entities = Game.getMap().getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if(entity instanceof Player || entity == this)
                continue;
            if(this.colliding(entity)) {
                action.effect(entity);
                this.disappear();
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.draw(getSprite(), (int)getX(), (int)getY(), angle + Math.PI/4, null, g);
    }

}
