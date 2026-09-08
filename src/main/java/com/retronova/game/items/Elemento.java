package com.retronova.game.items;

import com.retronova.game.objects.entities.AttackTypes;

import java.awt.Color;

/**
 * O elemento de uma arma: o que ela e alem da cor.
 *
 * O pedido foi explicito — "nao e pra ser so ruido": espada de fogo tem de dar
 * dano de fogo. Entao o elemento nao mora no nome nem no PNG, mora aqui, e
 * carrega as tres coisas que ele muda no jogo: o TIPO DE DANO, que passa pelo
 * mapa de resistencias de cada inimigo em Entity.strike; o MULTIPLICADOR, que e o
 * unico jeito de "lendaria" significar alguma coisa sem inventar um elemento que
 * ninguem resiste; e a COR, usada nos efeitos desenhados em tela, para o feitico
 * na tela combinar com a arma na mao.
 *
 * NENHUM e um valor de verdade, e nao ausencia: a arma comum passa por exatamente
 * o mesmo caminho das outras, so que devolvendo o tipo de dano padrao da arma. Sem
 * isso cada classe precisaria de um "if elemento == null" antes de todo golpe.
 *
 * A LENDARIA nao tem tipo proprio de proposito. Um sexto tipo de dano que nenhum
 * bicho resiste seria, na pratica, dano puro com nome bonito; o que a torna
 * lendaria e bater mais forte no tipo que a arma ja tinha.
 */
public enum Elemento {

    NENHUM("",        "",           null,               1.00, new Color(0xB6CBCF)),
    FOGO("_fire",     "Fire ",      AttackTypes.Fire,   1.10, new Color(0xDF864D)),
    GELO("_ice",      "Ice ",       AttackTypes.Ice,    1.05, new Color(0x85C4D4)),
    AGUA("_water",    "Water ",     AttackTypes.Water,  1.05, new Color(0x79A8D4)),
    TERRA("_earth",   "Earth ",     AttackTypes.Earth,  1.15, new Color(0xB07E41)),
    AR("_air",        "Air ",       AttackTypes.Air,    0.95, new Color(0xD7FFE4)),
    LENDARIA("_legend", "Legendary ", null,             1.50, new Color(0xFFD850));

    private final String sufixo;
    private final String prefixoDoNome;
    private final AttackTypes ataque;
    private final double multiplicador;
    private final Color cor;

    Elemento(String sufixo, String prefixoDoNome, AttackTypes ataque,
             double multiplicador, Color cor) {
        this.sufixo = sufixo;
        this.prefixoDoNome = prefixoDoNome;
        this.ataque = ataque;
        this.multiplicador = multiplicador;
        this.cor = cor;
    }

    /** O sufixo do arquivo: "sword" + "_fire" = sword_fire.png. */
    public String sprite(String base) {
        return base + sufixo;
    }

    public String nome(String base) {
        return prefixoDoNome + base;
    }

    /**
     * O tipo de dano. Cai no padrao da arma quando o elemento nao tem um proprio.
     *
     * @param padrao o que a arma usa sem elemento — corte na espada, perfuracao
     *               no machado e no arco, feitico na varinha.
     */
    public AttackTypes ataque(AttackTypes padrao) {
        return ataque == null ? padrao : ataque;
    }

    public double dano(double base) {
        return base * multiplicador;
    }

    public Color cor() {
        return cor;
    }
}
