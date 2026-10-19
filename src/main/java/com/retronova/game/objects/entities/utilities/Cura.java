package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Word;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Um coracao que cai de objeto quebrado e cura uma fatia da vida.
 *
 * Vem andando ate o gato como o XP — com a horda em volta, parar para catar e
 * morrer. A diferenca e o que ele diz: o XP e o futuro, a cura e o agora, e
 * por isso ela e vermelha, pulsa e e maior.
 */
public class Cura extends Utility {

    /** Fracao da vida maxima que ele devolve. */
    private static final double FRACAO = 0.15;
    private static final int ESPALHANDO = 20;

    private int idade;
    private double velocidade;

    public Cura(double x, double y) {
        super(x, y, 1);
        loadSprites("xp");   // so para o tamanho da caixa; o desenho e proprio
    }

    @Override
    public void tick() {
        idade++;
        Player gato = Game.getPlayer();
        if (gato.colliding(this)) {
            double cura = gato.getLifeSize() * FRACAO;
            gato.setLife(Math.min(gato.getLifeSize(), gato.getLife() + cura));
            Game.getMap().put(new Word("+" + (int) Math.ceil(cura),
                    gato.getX() + gato.getWidth() / 2.0, gato.getY(), 1));
            Sound.play(Sounds.Coin);
            disappear();
            return;
        }
        if (idade < ESPALHANDO) {
            return;
        }
        double limite = gato.getDistance(this) < gato.getRangeOfColect() ? 15 : 8;
        velocidade = Math.min(limite, velocidade + 0.25);
        getPhysical().addForce("follow", velocidade, gato.getAngle(this));
    }

    @Override
    public void render(Graphics2D g) {
        int s = Configs.GameScale();
        double pulso = 1 + 0.12 * Math.sin((idade + Engine.alpha()) * 0.2);
        int cel = Math.max(1, (int) Math.round(s * pulso));
        String[] coracao = {
                ".##.##.",
                "#+#####",
                "#######",
                ".#####.",
                "..###..",
                "...#..."};
        int lado = 7 * cel;
        int x0 = (int) getX() + getWidth() / 2 - lado / 2;
        int y0 = (int) getY() + getHeight() / 2 - 3 * cel;
        g.setColor(new Color(255, 70, 90, 60));
        g.fillOval(x0 - lado / 2, y0 - lado / 2, lado * 2, lado * 2);
        for (int ly = 0; ly < coracao.length; ly++) {
            for (int lx = 0; lx < coracao[ly].length(); lx++) {
                char c = coracao[ly].charAt(lx);
                if (c == '.') {
                    continue;
                }
                g.setColor(Color.BLACK);
                g.fillRect(x0 + lx * cel + cel / 2, y0 + ly * cel + cel / 2, cel, cel);
                g.setColor(c == '+' ? Color.WHITE : new Color(0xE0334F));
                g.fillRect(x0 + lx * cel, y0 + ly * cel, cel, cel);
            }
        }
    }
}
