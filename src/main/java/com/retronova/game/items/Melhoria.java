package com.retronova.game.items;

import com.retronova.game.objects.entities.Modifiers;

/**
 * O QUE UMA CARTA DE FIM DE TURNO ENTREGA.
 *
 * ---------------------------------------------------------------------------
 * POR QUE AS CARTAS DEIXARAM DE SER ARMAS
 *
 * Até aqui a recompensa era uma arma inteira, e isso tinha dois defeitos. O
 * primeiro é que trocar de arma é uma decisão que o jogador toma UMA vez e nunca
 * mais: depois que a espada de fogo entra na mão, as próximas quinze cartas de
 * arma são ruído. O segundo é que raridade não significava nada — uma "espada
 * épica" era só uma espada de outra cor, porque a arma não tinha grau.
 *
 * Melhoria resolve os dois. A mesma melhoria existe em quatro intensidades, então
 * a raridade passa a ser o que ela sempre deveria ter sido: QUANTO, e não O QUÊ.
 * É assim que o Hades faz — a mesma bênção sai comum, rara ou épica, com o mesmo
 * efeito e números diferentes —, e é o que multiplica o conteúdo por quatro sem
 * escrever uma carta nova.
 *
 * ---------------------------------------------------------------------------
 * A CURVA DE RETORNO DECRESCENTE, e por que ela existe
 *
 * Uma corrida tem vinte salas. Sem freio, pegar "dano" vinte vezes seria a
 * jogada certa sempre, e a escolha de fim de turno viraria uma formalidade.
 *
 * A curva é a do Pom of Power do Hades: a primeira cópia vale inteira, a segunda
 * oitenta por cento, depois sessenta, quarenta, trinta, vinte, quinze e dez, com
 * piso em dez. O efeito é que espalhar rende mais do que insistir — e é o
 * jogador que descobre isso jogando, sem o jogo precisar proibir nada.
 */
public enum Melhoria {

    /** Vida máxima. O degrau comum vale um décimo da vida do Muffin. */
    VIGOR("Vigor", "Maximum health", Modifiers.Life, Formato.INTEIRO,
            8, 16, 28, 50),

    /** Dano somado a todo golpe, seja de arma que for. */
    FURIA("Fury", "Attack damage", Modifiers.Damage, Formato.INTEIRO,
            3, 6, 11, 20),

    /** Desconto em todo dano recebido. Ver Entity.strike para o teto. */
    COURO("Hide", "Damage resistance", Modifiers.Resistance, Formato.PORCENTO,
            0.04, 0.08, 0.13, 0.20),

    /** Velocidade de corrida. A base do Muffin é 3,5. */
    PATAS("Swift Paws", "Move speed", Modifiers.Speed, Formato.DECIMAL,
            0.25, 0.45, 0.75, 1.20),

    /**
     * Cadência.
     *
     * OS VALORES SÃO NEGATIVOS DE PROPÓSITO: o atributo é o INTERVALO entre
     * golpes, em ticks, então melhorar é diminuir. O texto da carta esconde isso
     * do jogador — ele lê "ataca mais rápido", que é o que de fato acontece — mas
     * o sinal tem de estar certo aqui, ou a carta deixaria a arma mais lenta.
     */
    FRENESI("Frenzy", "Attack speed", Modifiers.AttackSpeed, Formato.CADENCIA,
            -1.5, -3, -5, -8),

    /** Sorte: alimenta a esquiva e o peso da experiência que cai. */
    INSTINTO("Instinct", "Luck", Modifiers.Luck, Formato.DECIMAL,
            0.10, 0.20, 0.35, 0.60),

    /**
     * A carta que melhora as PRÓXIMAS cartas.
     *
     * É a única do baralho que não faz nada no combate, e por isso ela precisa de
     * três cuidados que as outras não precisam.
     *
     * O PRIMEIRO É A FORMA DO EFEITO. O Hades resolve isto levantando o PISO da
     * raridade: o Ovo Cósmico e o santuário de Caronte fazem a bênção não poder
     * mais sair comum. É limpo, mas é grosso demais para uma carta que se repete —
     * três cópias apagariam o sistema de raridade inteiro. Aqui o efeito é uma
     * CHANCE DE PROMOÇÃO: a carta sorteia o grau normalmente e depois pode subir
     * um degrau. Fina o bastante para empilhar, e diz a mesma coisa em uma frase.
     *
     * O SEGUNDO É O TETO, em {@link Raridade#PROMOCAO_MAXIMA}. Sem ele, cópias
     * suficientes transformariam a promoção em certeza e a raridade deixaria de
     * existir — exatamente o defeito do piso, só que chegando devagar.
     *
     * O TERCEIRO É QUANDO ELA APARECE. Uma carta que melhora o futuro vale mais
     * quanto mais cedo vem, e na última sala ela não vale NADA. Oferecer uma opção
     * objetivamente morta é o pior que um sistema de cartas pode fazer, porque
     * quem não percebe é punido por não conhecer o jogo. Por isso ela sai de cena
     * no fim da corrida — ver {@code Melhorias.oferecer}.
     */
    PRESAGIO("Omen", "card upgrade chance", Modifiers.Fortune, Formato.PORCENTO,
            0.05, 0.09, 0.14, 0.20);

    /** Como o número é escrito na carta. */
    private enum Formato { INTEIRO, DECIMAL, PORCENTO, CADENCIA }

    private final String nome;
    private final String assunto;
    private final Modifiers atributo;
    private final Formato formato;
    private final double[] porRaridade;

    Melhoria(String nome, String assunto, Modifiers atributo, Formato formato,
             double comum, double raro, double epico, double lendario) {
        this.nome = nome;
        this.assunto = assunto;
        this.atributo = atributo;
        this.formato = formato;
        this.porRaridade = new double[]{comum, raro, epico, lendario};
    }

    public String nome() {
        return this.nome;
    }

    /** Em que atributo esta melhoria mexe. */
    public Modifiers atributo() {
        return this.atributo;
    }

    /** Quanto esta melhoria vale neste grau, antes do desgaste por repetição. */
    public double valor(Raridade raridade) {
        return porRaridade[raridade.ordinal()];
    }

    /**
     * O quanto a N-ésima cópia ainda rende, de 0 a 1.
     *
     * A curva do Pom of Power do Hades. O piso em dez por cento é deliberado: uma
     * cópia que valesse zero transformaria a carta num logro — o jogador escolheria
     * algo que não faz nada, e o jogo não teria dito.
     *
     * @param jaTinha quantas cópias o gato já tinha antes desta
     */
    public static double desgaste(int jaTinha) {
        double[] curva = {1.0, 0.8, 0.6, 0.4, 0.3, 0.2, 0.15, 0.1};
        return jaTinha < curva.length ? curva[jaTinha] : 0.1;
    }

    /** O que a carta promete, já com o desgaste desta cópia embutido. */
    public String efeito(Raridade raridade, int jaTinha) {
        double v = valor(raridade) * desgaste(jaTinha);
        return switch (formato) {
            case INTEIRO -> String.format("+%d %s", Math.round(Math.abs(v)),
                    assunto.toLowerCase());
            case DECIMAL -> String.format(java.util.Locale.ROOT, "+%.2f %s",
                    Math.abs(v), assunto.toLowerCase());
            case PORCENTO -> String.format("+%d%% %s", Math.round(Math.abs(v) * 100),
                    assunto.toLowerCase());
            // A CADENCIA E DITA EM QUADROS GANHOS, e nao no sinal cru do atributo.
            // "-3 intervalo de ataque" e verdade e nao comunica nada; o jogador
            // pensa em "ataca mais rapido", que e o mesmo fato do outro lado.
            case CADENCIA -> String.format("%d frames faster attacks",
                    Math.max(1, Math.round(Math.abs(v))));
        };
    }

    /** O assunto em uma palavra, para a linha de classificação da carta. */
    public String assunto() {
        return this.assunto;
    }
}
