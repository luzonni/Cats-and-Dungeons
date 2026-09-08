package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ThrownFurball extends Projetil {

    private double angle;

    /**
     * A bola ATRAVESSA e continua, mas cada inimigo so leva dano uma vez.
     *
     * Antes o controle era uma unica referencia ao ultimo atingido, o que deixava
     * o mesmo inimigo levar dano de novo assim que outro entrasse no meio.
     */
    private final Set<Enemy> atingidos = new HashSet<>();

    private static final int LIMITE = 60 * 3;

    public ThrownFurball(double centroX, double centroY, double angle) {
        this(centroX, centroY, angle, null);
    }

    public ThrownFurball(double centroX, double centroY, double angle, Entity dono) {
        super(centroX, centroY, 0, dono);
        loadSprites("furball");
        getPhysical().addForce("movement", 3, angle);
    }

    @Override
    public void tick() {
        angle += Math.PI / 12;
        // ATRAVESSA: acerta e continua, entao o fim so e checado depois.
        Enemy alvo = avancar(Enemy.class, LIMITE);
        if (alvo != null && atingidos.add(alvo)) {
            alvo.strike(AttackTypes.Piercing, 1);
        }
        if (acabou()) {
            disappear();
        }
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.draw(getSprite(), (int) (meioX() - getSprite().getWidth() / 2d),
                (int) (meioY() - getSprite().getHeight() / 2d), angle, null, g);
    }
}
