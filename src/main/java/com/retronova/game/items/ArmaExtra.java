package com.retronova.game.items;

import java.awt.Color;

/**
 * Armas que o gato carrega ALEM da arma da mao, ganhas na roleta do bau.
 *
 * A arma da mao e uma decisao de classe e nao muda; estas se acumulam por cima
 * dela, como em Vampire Survivors: o arco continua atirando enquanto o novelo
 * gira e a aura queima. Cada uma sobe ate o nivel cinco — a mesma carta saindo
 * de novo na roleta e o nivel seguinte, e nao uma copia.
 *
 * A logica de cada uma mora em Arsenal; aqui fica so o que a roleta precisa
 * para escrever a carta.
 */
public enum ArmaExtra {

    NOVELO("Yarn Ball", "Balls of yarn orbit you", new Color(0xFF6FA8)),
    GRIMORIO("Grimoire", "Spell books circle and knock back", new Color(0xC9A227)),
    AURA("Catnip Aura", "Burns everything close to you", new Color(0x5BE07A));

    public static final int NIVEL_MAXIMO = 5;

    private final String nome;
    private final String descricao;
    private final Color cor;

    ArmaExtra(String nome, String descricao, Color cor) {
        this.nome = nome;
        this.descricao = descricao;
        this.cor = cor;
    }

    public String nome() {
        return nome;
    }

    public Color cor() {
        return cor;
    }

    /** O que a carta promete para o nivel que vai ser ganho. */
    public String efeito(int novoNivel) {
        if (novoNivel <= 1) {
            return descricao;
        }
        return switch (this) {
            case NOVELO -> novoNivel % 2 == 0 ? "+1 yarn ball" : "More damage";
            case GRIMORIO -> novoNivel == 4 ? "More damage and knockback" : "+1 book";
            case AURA -> "Bigger and stronger aura";
        };
    }
}
