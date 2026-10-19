package com.retronova.game.objects.particles;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Expressao;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * O ultimo instante de um bicho: pisca branco, salta, gira e some.
 *
 * Antes o inimigo morto simplesmente deixava de existir entre um quadro e o
 * outro. Numa horda isso tira o peso de cada morte — o rato nao "morre", ele
 * para de ser desenhado. Um terco de segundo de corpo voando e o que faz o
 * golpe parecer ter derrubado alguem.
 */
public class Cadaver extends Particle {

    private static final int DURACAO = 20;

    private final BufferedImage sprite;
    private final double vx;
    private final double giro;
    private int idade;

    public Cadaver(BufferedImage sprite, double x, double y) {
        super(x, y, DURACAO / 60.0);
        this.sprite = sprite;
        this.vx = (Engine.RAND.nextDouble() - 0.5) * 3;
        this.giro = (Engine.RAND.nextBoolean() ? 1 : -1) * (0.15 + Engine.RAND.nextDouble() * 0.1);
        setWidth(sprite.getWidth() / (double) getWidth());
        setHeight(sprite.getHeight() / (double) getHeight());
    }

    @Override
    public void tick() {
        idade++;
        setX(getX() + vx);
        if (idade >= DURACAO) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        float t = Math.min(1f, (idade + Engine.alpha()) / DURACAO);
        // Salto: sobe e cai num arco.
        double salto = Math.sin(t * Math.PI) * sprite.getHeight() * 0.6;
        // Escala: estufa no comeco e encolhe ate sumir.
        double escala = t < 0.2 ? 1 + t * 1.5 : 1.3 * (1 - (t - 0.2) / 0.8);
        float opacidade = 1f - t * t;
        BufferedImage img = idade < 4 ? Expressao.clarao(sprite, 1f) : sprite;

        double cx = getX() + sprite.getWidth() / 2.0;
        double cy = getY() + sprite.getHeight() / 2.0 - salto;
        AffineTransform antes = g.getTransform();
        java.awt.Composite compAntes = g.getComposite();
        g.translate(cx, cy);
        g.rotate(giro * idade);
        g.scale(escala, escala);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, opacidade)));
        g.drawImage(img, -sprite.getWidth() / 2, -sprite.getHeight() / 2, null);
        g.setComposite(compAntes);
        g.setTransform(antes);
    }
}
