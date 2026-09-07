package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;

import java.awt.Graphics2D;

/**
 * Poeira levantada pelo passo.
 *
 * Substitui o uso de {@link Volatile} para a caminhada. O Volatile serve bem a
 * efeitos que não têm direção — dano, veneno, coração —, porque sorteia a deriva
 * na circunferência inteira. Aplicado ao passo, isso mandava metade da poeira
 * PARA A FRENTE do gato, como se ele estivesse correndo dentro dela. É o que
 * fazia a caminhada parecer errada sem ser óbvio o porquê.
 *
 * Aqui a nuvem sai no sentido CONTRÁRIO ao passo, num leque estreito, e vai
 * freando até parar — a poeira recebe o empurrão do pé e é abandonada no ar.
 *
 * A vida é curta de propósito: um terço de segundo, dentro da faixa que a
 * referência de animação recomenda para poeira de passo. A anterior durava mais
 * de oitocentos milissegundos e ficava boiando atrás do gato.
 */
public class Poeira extends Particle {

    /** Ticks de vida. Vinte a 60 Hz dão pouco mais de 0,3 s. */
    private static final int VIDA = 20;

    private final double dx;
    private final double dy;
    private int idade;

    public Poeira(double x, double y, double anguloDoPasso) {
        super(x, y, VIDA / 60d);
        loadSprites("walking");
        setX(getX() - getWidth() / 2d);
        setY(getY() - getHeight() / 2d);

        double contrario = anguloDoPasso + Math.PI;
        double leque = (Engine.RAND.nextDouble() - 0.5) * 0.6;
        // A força acompanha a escala do jogo: em pixels de mundo, um valor fixo
        // ficaria imperceptível nas escalas grandes.
        double forca = (0.18 + Engine.RAND.nextDouble() * 0.18) * Configs.GameScale();
        this.dx = Math.cos(contrario + leque) * forca;
        this.dy = Math.sin(contrario + leque) * forca;
    }

    @Override
    public void tick() {
        idade++;
        double freio = 1 - (idade / (double) VIDA);
        setX(getX() + dx * freio);
        setY(getY() + dy * freio);

        int quadro = idade * getSheet().size() / VIDA;
        getSheet().setIndex(Math.min(quadro, getSheet().size() - 1));

        if (idade >= VIDA) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }
}
