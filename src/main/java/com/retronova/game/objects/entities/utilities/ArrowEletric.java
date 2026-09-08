package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.objects.entities.EffectApplicator;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class ArrowEletric extends Projetil {

    private final double angle;
    private final EffectApplicator action;

    private static final int LIMITE = 60 * 4;

    public ArrowEletric(double centroX, double centroY, double angle, EffectApplicator action) {
        this(centroX, centroY, angle, null, action);
    }

    public ArrowEletric(double centroX, double centroY, double angle, Entity dono,
                        EffectApplicator action) {
        super(centroX, centroY, 0, dono);
        this.angle = angle;
        this.action = action;
        loadSprites("arroweletric");
        setSpeed(7d);
        this.getPhysical().addForce("shot", getSpeed(), angle);
    }

    @Override
    public void tick() {
        Entity alvo = avancar(Entity.class, LIMITE);
        if (alvo != null && !(alvo instanceof Player)) {
            action.effect(alvo);
            disappear();
            return;
        }
        if (acabou()) {
            disappear();
        }
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.apontar(getSprite(), meioX(), meioY(), angle, Rotate.DIAGONAL, g);
    }
}
