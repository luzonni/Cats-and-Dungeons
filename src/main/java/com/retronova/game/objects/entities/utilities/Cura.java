package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

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
            com.retronova.game.objects.particles.NumeroDeDano.cura((int) Math.ceil(cura),
                    gato.getX() + gato.getWidth() / 2.0, gato.getY());
            Sound.play(Sounds.Coin);
            Game.getMap().put(new com.retronova.game.objects.particles.Coleta(com.retronova.game.items.Icone.VIGOR,
                    getX() + getWidth() / 2.0, getY() + getHeight() / 2.0, Math.max(1, Math.round(Configs.GameScale() * 0.67f))));
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

    /**
     * O CORAÇÃO É O ÍCONE DA VIDA (Icone.VIGOR), o mesmo do card: arte em jogo =
     * arte do ícone. Era uma matriz pintada aqui com sombra preta, brilho branco
     * puro e um halo suavizado por quadro. O pulso, que diz "pega", virou um
     * sobe-e-desce em pixel inteiro, e o halo é imagem pronta na família da vida.
     */
    @Override
    public void render(Graphics2D g) {
        int s = Configs.GameScale();
        int escala = Math.max(1, Math.round(s * 0.67f));
        int cx = (int) getX() + getWidth() / 2;
        int cy = (int) (getY() + getHeight() / 2 - Math.round(Math.abs(Math.sin((idade + Engine.alpha()) * 0.1)) * 2) * s);
        com.retronova.engine.graphics.Brilho.disco(g, cx, cy, 9 * s,
                com.retronova.engine.graphics.Familia.VIDA.tom(com.retronova.engine.graphics.Familia.LUZ, 60));
        com.retronova.game.items.Icone.VIGOR.desenharCentrado(g, cx, cy, escala);
    }
}
