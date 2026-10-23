package com.retronova.game.objects.particles;

import com.retronova.game.Game;
import com.retronova.game.items.Icone;

import java.awt.Graphics2D;

/**
 * O "peguei" de um coletável: o ícone dele sobe um pouco e some piscando, em
 * pouco mais de um décimo de segundo.
 *
 * CURTA DE PROPÓSITO: coletar acontece dezenas de vezes por minuto, e a animação
 * só precisa confirmar que o item entrou — sumir no mesmo quadro em que encosta
 * deixava a dúvida se pegou ou se passou por cima. Piscar em vez de desbotar:
 * mudar a transparência por partícula esvazia a fila do pipeline de vídeo (ver
 * Icones da Roleta), e o pisca lê igual nessa duração.
 */
public final class Coleta extends Particle {

    /** Duração, em ticks, e quanto sobe por tick, em pixels de tela. */
    public static final int TICKS = 8;
    private static final double SOBE = 1.5;

    private final Icone icone;
    private final int escala;
    private int idade;

    public Coleta(Icone icone, double cx, double cy, int escala) {
        super(cx, cy, TICKS / 60.0);
        this.icone = icone;
        this.escala = escala;
    }

    @Override
    public void tick() {
        idade++;
        if (idade >= TICKS) {
            Game.getMap().remove(this);
            return;
        }
        setY(getY() - SOBE);
    }

    @Override
    public void render(Graphics2D g) {
        if (idade % 2 == 0) {
            icone.desenharCentrado(g, (int) getX(), (int) getY(), escala);
        }
    }
}
