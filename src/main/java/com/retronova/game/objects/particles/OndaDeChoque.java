package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;

import java.awt.*;

/**
 * Um anel que abre a partir de um ponto e some: a leitura de "algo explodiu aqui".
 *
 * Serve às habilidades de área dos gatos (a onda do Muffin, a explosão do Azrael).
 * É discreto de propósito — traço fino, pouca opacidade, um terço de segundo —
 * porque numa horda essas ondas saem o tempo todo, e um efeito que chama atenção
 * a cada vez vira ruído por cima do que importa: os bichos e o gato.
 *
 * O raio cresce rápido e desacelera (ease-out), e a opacidade cai junto. O anel
 * chega ao raio do efeito no fim, para o jogador ver até onde ele alcançou.
 */
public class OndaDeChoque extends Particle {

    private final double raio;
    private final Color cor;
    private final float forca;
    private int count;

    /**
     * @param cx    centro, em pixels de mundo
     * @param cy    centro, em pixels de mundo
     * @param raio  até onde o anel abre, em pixels
     * @param cor   cor do anel
     * @param forca opacidade máxima, de 0 a 1 (as ondas pequenas usam menos)
     */
    public OndaDeChoque(double cx, double cy, double raio, Color cor, float forca) {
        super(cx, cy, 0.33);
        this.raio = raio;
        this.cor = cor;
        this.forca = Math.max(0f, Math.min(1f, forca));
    }

    @Override
    public void tick() {
        if (++count >= getSeconds()) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        double f = Math.min(1, (count + Engine.alpha()) / getSeconds());
        double r = raio * (1 - Math.pow(1 - f, 3));
        int alfa = (int) (255 * forca * (1 - f));
        if (alfa <= 0 || r <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double cx = getX(), cy = getY();
        // Um véu bem fraco dentro do anel, e o anel por cima.
        g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alfa / 5));
        g2.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
        g2.setStroke(new BasicStroke(Math.max(1f, Configs.GameScale() * 0.75f)));
        g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alfa));
        g2.drawOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
        g2.dispose();
    }
}
