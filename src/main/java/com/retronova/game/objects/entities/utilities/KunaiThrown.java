package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;

public class KunaiThrown extends Projetil {

    private final double damage;
    private final double direction;

    /** Trava de seguranca, em ticks. */
    private static final int LIMITE = 60 * 4;

    public KunaiThrown(double centroX, double centroY, double damage, double direction,
                       Entity dono) {
        super(centroX, centroY, 0, dono);
        this.damage = damage;
        this.direction = direction;
        loadSprites("kunai");
        // Peso zero e forca aplicada uma vez: sem atrito, a kunai mantem a
        // velocidade ate encostar em algo. Com peso 2, como estava, ela perdia
        // forca no caminho e parava sozinha antes de chegar.
        getPhysical().addForce("moving", 8, this.direction);
    }

    @Override
    public void tick() {
        Enemy alvo = avancar(Enemy.class, LIMITE);
        if (alvo != null) {
            alvo.strike(AttackTypes.Piercing, this.damage);
            disappear();
            return;
        }
        if (acabou()) {
            disappear();
        }
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.apontar(getSprite(), meioX(), meioY(), direction, Rotate.DIAGONAL, g);
    }
}
