package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.EffectApplicator;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import java.awt.*;

public class Arrow extends Projetil {

    private final double angle;
    private final EffectApplicator action;

    /** Trava de seguranca: nenhuma flecha vive mais que isto. */
    private static final int LIMITE = 60 * 4;

    public Arrow(double centroX, double centroY, double angle, EffectApplicator action) {
        this(centroX, centroY, angle, "arrow", 7d, null, action);
    }

    public Arrow(double centroX, double centroY, double angle, String sprite,
                 EffectApplicator action) {
        this(centroX, centroY, angle, sprite, 7d, null, action);
    }

    public Arrow(double centroX, double centroY, double angle, String sprite,
                 double velocidade, EffectApplicator action) {
        this(centroX, centroY, angle, sprite, velocidade, null, action);
    }

    /**
     * @param centroX,centroY o CENTRO de onde o tiro sai — a ponta da arma
     * @param dono            quem atirou; nunca e alvo do proprio tiro
     * @param velocidade      pixels de arte por tick
     */
    public Arrow(double centroX, double centroY, double angle, String sprite,
                 double velocidade, Entity dono, EffectApplicator action) {
        super(centroX, centroY, 0, dono);
        this.angle = angle;
        this.action = action;
        loadSprites(sprite);
        setSpeed(velocidade);
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

    /**
     * Em voo, a flecha e desenhada pelo MIOLO DO DESENHO no meio da hitbox.
     *
     * A hitbox agora tem seis pixels de arte, e nao os dezesseis de antes; o
     * desenho continua do tamanho que era e so passa a ser centrado nela.
     */
    @Override
    public void render(Graphics2D g) {
        Rotate.apontar(getSprite(), meioX(), meioY(), angle, Rotate.PARA_CIMA, g);
    }
}
