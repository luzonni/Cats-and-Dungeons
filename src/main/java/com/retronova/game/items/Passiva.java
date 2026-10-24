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
    CRESCIMENTO("Growth", "XP gain", new Color(0x7FE0C8), "+%d%% XP gained", 10),
    /**
     * VOLEIO: exclusiva do Finn. Cada nivel e uma flecha a mais por disparo.
     *
     * Tem teto proprio de TRES niveis, e nao cinco: com cinco, e somada a pressa,
     * o arco virava uma metralhadora de seis flechas. O dano tambem nao soma
     * inteiro — ver {@code Bonus.danoPorFlecha}.
     */
    VOLEIO("Volley", "Arrows per shot", new Color(0xE8B04A), "+%d arrow per shot", 1, 3, "Finn"),
    /**
     * RETALIACAO: exclusiva do Muffin. Ao levar dano, ele solta uma onda que
     * machuca e EMPURRA quem esta perto.
     *
     * O Muffin e o gato que aguenta, mas aguentar sozinho nao e jogo: e esperar.
     * A onda faz o golpe recebido virar espaco — o cerco que ia esmaga-lo e jogado
     * para tras. Tem recarga, entao nao vira imunidade: ver {@code Bonus.retaliacao}.
     */
    RETALIACAO("Retaliate", "Shockwave when hurt", new Color(0x5FC9A8), "", 0, 3, "Muffin",
            "Shockwave when you get hurt",
            "Wider, stronger shockwave",
            "Huge shockwave, faster recharge"),
    /**
     * EXPLOSAO ARCANA: exclusiva do Azrael. As bolas da varinha explodem ao
     * acertar e ferem quem esta em volta do alvo.
     *
     * O Azrael ja e o gato da area (+20%), mas a arma da mao dele acertava um bicho
     * por vez. A explosao usa o multiplicador de area dele, entao as duas coisas
     * passam a conversar. O dano em volta e uma fracao do golpe, e o alvo direto
     * nao leva duas vezes: ver {@code Bonus.explosaoArcana}.
     */
    EXPLOSAO_ARCANA("Arcane Burst", "Bolts explode on impact", new Color(0xC08CFF), "", 0, 3, "Azrael",
            "Bolts burst on impact",
            "Bigger, stronger bursts",
            "Huge bursts");

    public static final int NIVEL_MAXIMO = 5;

    private final String nome;
    private final String assunto;
    private final Color cor;
    private final String formato;
    /** Quanto cada nivel vale, nas unidades do formato (por cento, ou decimos de HP/s). */
    private final int porNivel;

    /** Ate onde ela sobe. A maioria vai ate {@link #NIVEL_MAXIMO}. */
    private final int nivelMaximo;
    /** O unico gato que a recebe, ou null se e de todos. */
    private final String dono;

    Passiva(String nome, String assunto, Color cor, String formato, int porNivel) {
        this(nome, assunto, cor, formato, porNivel, NIVEL_MAXIMO, null);
    }

    /** Texto da carta por nivel (o primeiro e o do nivel 1), ou null para o formato. */
    private final String[] textos;

    Passiva(String nome, String assunto, Color cor, String formato, int porNivel,
            int nivelMaximo, String dono, String... textos) {
        this.nome = nome;
        this.assunto = assunto;
        this.cor = cor;
        this.formato = formato;
        this.porNivel = porNivel;
        this.nivelMaximo = nivelMaximo;
        this.dono = dono;
        this.textos = textos.length == 0 ? null : textos;
    }

    /**
     * O que a carta diz ao oferecer o nivel {@code nivel} (1 = a primeira vez).
     *
     * As habilidades dos gatos nao mudam um numero so por nivel — o raio, a forca
     * e a recarga mudam juntos — e "+1 shockwave level" nao diz nada. Elas trazem
     * um texto por nivel; as passivas comuns continuam no formato.
     */
    public String efeitoNoNivel(int nivel) {
        if (textos != null) {
            return textos[Math.max(0, Math.min(textos.length - 1, nivel - 1))];
        }
        return efeitoDeUmNivel();
    }

    public int nivelMaximo() {
        return nivelMaximo;
    }

    /** Esta passiva pode sair para este gato? As exclusivas so para o dono. */
    public boolean disponivelPara(String nomeDoGato) {
        if (this == PRESSA && !ArmaExtra.ATIVAS) {
            return false; // Só altera armas extras: não oferecer uma carta sem efeito.
        }
        return dono == null || dono.equals(nomeDoGato);
    }

    /** E exclusiva de algum gato? A carta mostra isso no rotulo. */
    public boolean exclusiva() {
        return dono != null;
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

    /**
     * O efeito ACUMULADO com {@code nivel} niveis, como a tela de Status mostra:
     * "+18% critical chance" com tres niveis de 6%. As habilidades dos gatos, que
     * tem texto por nivel, mostram o texto do nivel atual.
     */
    public String efeitoTotal(int nivel) {
        if (textos != null) {
            return efeitoNoNivel(nivel);
        }
        int total = porNivel * Math.max(0, nivel);
        if (this == REGENERACAO) {
            return String.format(formato, total / 10, total % 10);
        }
        return String.format(formato, total);
    }

    /** Valor de um nivel, como fracao (0.4 = 40%), ou HP/s para a regeneracao. */
    public double valorPorNivel() {
        return this == REGENERACAO ? porNivel / 10.0 : porNivel / 100.0;
    }
}
