package com.retronova.menus;

import java.awt.Rectangle;

/** O relatório ocupa a janela; texto mantém a escala escolhida nos menus. */
record LayoutDoFim(Rectangle abas, Rectangle personagem, Rectangle atributos,
                   Rectangle danos, Rectangle loja, int botoesY) {
    static final int MARGEM = 12, VAO = 10, ALTURA_ABAS = 13, ESPACO_ABAS = 10;
    static final double PERSONAGEM = 0.24, ATRIBUTOS = 0.38;

    static LayoutDoFim calcular(int largura, int escala, int topo, int base, int botaoAltura, int margemConfigurada) {
        int margem = Math.max(margemConfigurada, MARGEM * escala);
        int vao = VAO * escala;
        int util = largura - 2 * margem;
        int y = topo + (ALTURA_ABAS + ESPACO_ABAS) * escala;
        int by = base - botaoAltura;
        int h = Math.max(1, by - vao - y);
        int colunas = util - 2 * vao;
        int esquerda = (int) (colunas * PERSONAGEM);
        int centro = (int) (colunas * ATRIBUTOS);
        int direita = colunas - esquerda - centro;
        int x2 = margem + esquerda + vao, x3 = x2 + centro + vao;
        return new LayoutDoFim(new Rectangle(margem, topo, util, ALTURA_ABAS * escala),
                new Rectangle(margem, y, esquerda, h), new Rectangle(x2, y, centro, h),
                new Rectangle(x3, y, direita, h), new Rectangle(x2, y, centro + vao + direita, h), by);
    }
}
