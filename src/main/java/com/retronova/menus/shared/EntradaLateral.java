package com.retronova.menus.shared;

import com.retronova.engine.Engine;

import java.awt.Graphics2D;

/**
 * Uma tela entrando de lado, como a troca de tela de um celular: curta, rápida
 * no começo e assentando sem tranco (ease-out). Reaproveitável: quem desenha a
 * tela chama {@link #aplicar} antes e anda {@link #tick} no tick.
 *
 * Nasceu para a loja do vendedor surgir depois da conversa; a loja do espelho
 * vai usar a mesma, e qualquer painel que precise "chegar" em vez de aparecer.
 *
 * Enquanto desliza, {@link #ativa} é verdadeiro: quem usa ignora aperto nesse
 * tempo, senão o mesmo Enter que encerrou o diálogo compraria um item.
 */
public final class EntradaLateral {

    /** Duração, em ticks: 0,23 s. Abaixo de 0,15 s a entrada não se vê; acima de 0,3 s atrasa. */
    public static final int DURACAO = 14;
    /** De que lado ela vem: 1 da direita, -1 da esquerda. */
    private final int lado;
    private int t;

    public EntradaLateral(int lado) {
        this.lado = lado >= 0 ? 1 : -1;
    }

    public static EntradaLateral daDireita() {
        return new EntradaLateral(1);
    }

    public void tick() {
        if (t < DURACAO) {
            t++;
        }
    }

    public boolean ativa() {
        return t < DURACAO;
    }

    /** Quanto já entrou, de 0 a 1, com a fração entre ticks. */
    public double progresso(float alpha) {
        double f = Math.min(1, (t + (ativa() ? alpha : 0)) / DURACAO);
        return 1 - Math.pow(1 - f, 3);
    }

    /** Desloca o desenho que vem a seguir para onde a tela está agora. */
    public void aplicar(Graphics2D g, float alpha) {
        double falta = 1 - progresso(alpha);
        g.translate((int) Math.round(falta * Engine.window.largura() * lado), 0);
    }
}
