package com.retronova.game.items;

import java.awt.Color;

/**
 * Passivas: melhorias que nao sao atributo bruto nem arma, e sim REGRAS — o
 * alcance do ima de XP, quanto o dano recebido cai, quanto a area das armas
 * cresce. Vem das cartas de nivel (SubirDeNivel), sobem ate o nivel 5, e cada
 * uma e metade de uma evolucao de arma (ver Evolucao).
 *
 * O EFEITO em si e lido por Bonus, que soma passiva + gato + progressao entre
 * corridas; aqui fica so o que a carta precisa para ser escrita.
 *
 * Novas sempre no FIM do enum: o save grava o ordinal.
 */
public enum Passiva {

    IMA("Magnet", "XP pickup range", new Color(0x5FB0FF), "+%d%% pickup range", 40),
    ARMADURA("Armor", "Damage taken", new Color(0x9AA4B8), "-%d%% damage taken", 6),
    REGENERACAO("Regeneration", "Health regen", new Color(0xE0475F), "+%d.%d HP per second", 4),
    CRITICO("Critical", "Critical chance", new Color(0xFFD23F), "+%d%% critical chance", 6),
    AREA("Area", "Weapon area", new Color(0x5BE07A), "+%d%% weapon area", 12),
    PRESSA("Haste", "Weapon cooldown", new Color(0xB57CF0), "-%d%% weapon cooldown", 8),
    CRESCIMENTO("Growth", "XP gain", new Color(0x7FE0C8), "+%d%% XP gained", 10);

    public static final int NIVEL_MAXIMO = 5;

    private final String nome;
    private final String assunto;
    private final Color cor;
    private final String formato;
    /** Quanto cada nivel vale, nas unidades do formato (por cento, ou decimos de HP/s). */
    private final int porNivel;

    Passiva(String nome, String assunto, Color cor, String formato, int porNivel) {
        this.nome = nome;
        this.assunto = assunto;
        this.cor = cor;
        this.formato = formato;
        this.porNivel = porNivel;
    }

    public String nome() {
        return nome;
    }

    public String assunto() {
        return assunto;
    }

    public Color cor() {
        return cor;
    }

    /** O efeito de UM nivel, como a carta escreve. */
    public String efeitoDeUmNivel() {
        if (this == REGENERACAO) {
            return String.format(formato, porNivel / 10, porNivel % 10);
        }
        return String.format(formato, porNivel);
    }

    /** Valor de um nivel, como fracao (0.4 = 40%), ou HP/s para a regeneracao. */
    public double valorPorNivel() {
        return this == REGENERACAO ? porNivel / 10.0 : porNivel / 100.0;
    }
}
