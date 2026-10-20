package com.retronova.game.items;

/**
 * Evolucoes de arma: uma arma extra no nivel maximo + a passiva parceira dela
 * viram uma versao evoluida, entregue pelo proximo bau aberto.
 *
 * E o objetivo de longo prazo da corrida, copiado do Vampire Survivors: o
 * jogador deixa de pegar "a carta mais forte" e passa a montar PARES. A
 * evolucao dobra o dano da arma e muda o jeito dela (Arsenal e ArmasDeArea
 * perguntam a Melhorias se a arma evoluiu).
 *
 * Novas sempre no FIM do enum: o save grava o ordinal.
 */
public enum Evolucao {

    NOVELO_GIGANTE(ArmaExtra.NOVELO, Passiva.IMA, "Giant Yarn", "Huge yarn balls, double damage"),
    TOMO_ANCIAO(ArmaExtra.GRIMORIO, Passiva.AREA, "Ancient Tome", "Wider orbit, double damage and knockback"),
    CATNIP_SAGRADO(ArmaExtra.AURA, Passiva.REGENERACAO, "Holy Catnip", "Huge aura, heals you as it burns"),
    TEMPESTADE(ArmaExtra.RAIO, Passiva.CRITICO, "Thunderstorm", "Twice the chains, double damage"),
    TERREMOTO(ArmaExtra.TREMOR, Passiva.ARMADURA, "Earthshaker", "Double quake radius and damage"),
    ARMAGEDOM(ArmaExtra.METEORO, Passiva.PRESSA, "Armageddon", "Twice the meteors, double damage");

    private final ArmaExtra arma;
    private final Passiva passiva;
    private final String nome;
    private final String descricao;

    Evolucao(ArmaExtra arma, Passiva passiva, String nome, String descricao) {
        this.arma = arma;
        this.passiva = passiva;
        this.nome = nome;
        this.descricao = descricao;
    }

    public ArmaExtra arma() {
        return arma;
    }

    public Passiva passiva() {
        return passiva;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public static Evolucao de(ArmaExtra arma) {
        for (Evolucao e : values()) {
            if (e.arma == arma) {
                return e;
            }
        }
        return null;
    }
}
