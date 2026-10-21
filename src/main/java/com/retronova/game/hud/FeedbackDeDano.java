package com.retronova.game.hud;

import com.retronova.engine.Engine;
import com.retronova.game.map.Camera;
import com.retronova.game.objects.entities.Player;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * O que a tela diz quando o gato apanha: um clarão vermelho nas BORDAS, um pulso
 * fraco quando a vida está baixa, e um tremor mínimo de câmera.
 *
 * Antes o golpe recebido só aparecia na barra de vida, num canto, e no meio de
 * uma horda ninguém olha para o canto. O gemido e a vibração do controle já
 * existiam; faltava a tela.
 *
 * DISCRETO DE PROPÓSITO. Tudo fica nas bordas e nunca cobre o centro, onde estão
 * o gato e os bichos. O clarão é proporcional ao quanto doeu (um arranhão quase
 * não aparece) e some em um terço de segundo. O pulso de vida baixa é mais fraco
 * ainda. E o tremor fica bem abaixo do de um terremoto ou de uma elite caindo:
 * levar dano é o que mais acontece numa corrida, e um efeito que grita a cada
 * golpe vira ruído e atrapalha justamente quando o jogador mais precisa ver.
 *
 * Lê a vida do gato a cada tick e compara com a anterior: não precisa de gancho
 * no Player, e cura (vida subindo) não dispara nada.
 */
final class FeedbackDeDano {

    /** Opacidade máxima do clarão, sobre a borda já semitransparente da imagem. */
    private static final float CLARAO_MAXIMO = 0.8f;
    /** Opacidade máxima do pulso de vida baixa: bem menos que o clarão. */
    private static final float PULSO_MAXIMO = 0.32f;
    /** Abaixo desta fração da vida, o pulso começa. */
    private static final double VIDA_BAIXA = 0.25;

    private final Player gato;
    private double vidaAnterior = -1;
    /** Força do clarão agora, de 0 a 1; decai a cada tick. */
    private float clarao;
    private int tempo;

    private BufferedImage borda;

    FeedbackDeDano(Player gato) {
        this.gato = gato;
    }

    void tick() {
        tempo++;
        clarao *= 0.88f;
        if (clarao < 0.01f) {
            clarao = 0;
        }
        double vida = gato.getLife();
        double maxima = Math.max(1, gato.getLifeSize());
        if (vidaAnterior >= 0 && vida < vidaAnterior && !gato.morrendo()) {
            double fracao = (vidaAnterior - vida) / maxima;
            clarao = Math.min(1f, clarao + intensidadeDoClarao(fracao));
            Camera.tremer(Math.min(2.0, 0.8 + fracao * 6));
        }
        vidaAnterior = vida;
    }

    /**
     * Quanto o clarão acende para um golpe que tirou {@code fracao} da vida máxima.
     *
     * Um golpe de 5% da vida acende um quarto; de 20% em diante, tudo. Abaixo de
     * um arranhão (1%) não acende nada: veneno e espinhos tiram vida aos pouquinhos,
     * e piscar a borda a cada tique deles seria insuportável.
     */
    static float intensidadeDoClarao(double fracao) {
        if (fracao < 0.01) {
            return 0;
        }
        return (float) Math.min(1, 0.15 + fracao * 4.25);
    }

    /** O pulso da vida baixa, de 0 a 1: um "tum-tum" por segundo, e nada acima de 25%. */
    static float intensidadeDoPulso(double fracaoDaVida, int tempo) {
        if (fracaoDaVida <= 0 || fracaoDaVida >= VIDA_BAIXA) {
            return 0;
        }
        // Quanto menos vida, mais forte; e duas batidas curtas a cada 60 ticks.
        double urgencia = 1 - fracaoDaVida / VIDA_BAIXA;
        int t = tempo % 60;
        double batida = t < 8 ? Math.sin(t / 8.0 * Math.PI) : (t >= 12 && t < 20 ? 0.6 * Math.sin((t - 12) / 8.0 * Math.PI) : 0);
        return (float) (0.35 + 0.65 * urgencia) * (float) (0.4 + 0.6 * batida);
    }

    void render(Graphics2D g) {
        double fracaoDaVida = gato.getLife() / Math.max(1, gato.getLifeSize());
        float pulso = gato.morrendo() ? 0 : intensidadeDoPulso(fracaoDaVida, tempo) * PULSO_MAXIMO;
        float alfa = Math.max(clarao * CLARAO_MAXIMO, pulso);
        if (alfa <= 0.01f) {
            return;
        }
        prepararBorda();
        Composite antes = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alfa)));
        g.drawImage(borda, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        g.setComposite(antes);
    }

    /**
     * A borda vermelha, pintada uma vez numa imagem pequena e esticada na tela.
     * O miolo é transparente até 55% do raio: o centro da tela nunca fica tingido.
     */
    private void prepararBorda() {
        if (borda != null) {
            return;
        }
        int lado = 128;
        borda = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gb = borda.createGraphics();
        Point2D centro = new Point2D.Float(lado / 2f, lado / 2f);
        gb.setPaint(new RadialGradientPaint(centro, lado * 0.72f, new float[]{0.55f, 1f},
                new Color[]{new Color(200, 20, 30, 0), new Color(200, 20, 30, 150)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        gb.fillRect(0, 0, lado, lado);
        gb.dispose();
    }
}
