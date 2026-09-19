package com.retronova.engine.sound;

/**
 * Converte a posição de um slider no ganho que o mixer recebe.
 *
 * ---------------------------------------------------------------------------
 * POR QUE NÃO BASTA DIVIDIR POR CEM
 *
 * Era o que os controles faziam: slider em 50 virava ganho 0,50. Parece a coisa
 * mais natural do mundo e é um erro conhecido, porque a audição não é linear —
 * ela é logarítmica. Um sinal precisa de DEZ VEZES a potência para ser ouvido
 * como o dobro de alto.
 *
 * A consequência prática: com ganho linear, metade do curso do slider cobre
 * apenas os últimos 6 dB. Sair de 100 para 50 quase não se ouve; sair de 10 para
 * 5 é uma queda enorme. Toda a faixa útil fica espremida nos primeiros passos, e
 * o resto do curso é um trecho onde arrastar não faz quase nada. É por isso que
 * o controle nunca parecia responder direito.
 *
 * Aqui a curva é exponencial, que é o inverso disso: cada passo do slider vale
 * o MESMO tanto de mudança percebida, do começo ao fim do curso.
 *
 * ---------------------------------------------------------------------------
 * OS NÚMEROS
 *
 * A forma é a recomendada para controles de volume: {@code y = a·e^(b·x)}, com
 * {@code a} e {@code b} escolhidos para que o curso inteiro cubra uma faixa
 * dinâmica dada e que x=1 devolva exatamente 1.
 *
 * QUARENTA DECIBÉIS, e não os sessenta da recomendação genérica. Sessenta é o
 * número de quem faz tocador de música, onde o material chega em qualquer nível
 * e o controle precisa dar conta de tudo. Aqui os arquivos passaram pela régua
 * do tools/GenMixagem.java e já chegam no nível certo, então o slider não
 * precisa consertar loudness — só ajustar gosto e ambiente. Com sessenta, o meio
 * do curso cairia a -30 dB e a metade de cima seria a única parte utilizável;
 * com quarenta, o meio fica em -20 dB e o curso inteiro serve.
 *
 * A RAMPA NO PÉ existe porque exponencial nunca chega a zero. Sem ela, o slider
 * no mínimo ainda deixaria passar -40 dB de som — audível numa sala silenciosa,
 * e quem arrasta até o fim quer SILÊNCIO, não "bem baixinho". Os dez por cento
 * finais do curso descem em reta até zero de verdade.
 */
public final class Ganho {

    /** Do topo ao pé do curso, em decibéis. */
    private static final double ALCANCE_DB = 40;

    /** O ganho no pé da curva exponencial: 10^(-40/20) = 0,01. */
    private static final double A = Math.pow(10, -ALCANCE_DB / 20);

    /** Escolhido para que x=1 devolva exatamente 1. */
    private static final double B = Math.log(1 / A);

    /** Abaixo disto o ganho desce em reta até zero. */
    private static final double RAMPA = 0.1;

    private Ganho() {
    }

    /**
     * O multiplicador para uma posição de slider de 0 a 100.
     *
     * @return de 0 (mudo de verdade) a 1 (o nível de referência da mixagem)
     */
    public static double de(int porcento) {
        double x = Math.max(0, Math.min(100, porcento)) / 100d;
        if (x <= 0) {
            return 0;
        }
        double y = A * Math.exp(B * x);
        if (x < RAMPA) {
            y *= x / RAMPA;
        }
        // O TETO E LITERAL. A * e^B vale 1 na algebra e 1,0000000000000004 em
        // ponto flutuante, e esse resto entrega ao mixer um ganho MAIOR que a
        // unidade — que e a definicao de estourar. Um teste pegou; a olho nunca
        // se veria, e o sintoma seria um estalo raro na faixa mais alta.
        return Math.min(1, y);
    }

    /**
     * A posição de slider que produz um ganho linear dado. É a inversa de {@link
     * #de}.
     *
     * SERVE À MIGRAÇÃO, e é a razão de ela existir. Quem já jogava tem um
     * config.json gravado com números do mundo linear — os padrões de fábrica
     * eram 20 e 20, que ali valiam -14 dB. Lidos pela curva nova, os mesmos 20
     * virariam -32 dB — dezoito decibéis abaixo, ou perto de um quarto do volume
     * percebido —, e a pessoa abriria o jogo achando que a atualização quebrou o
     * áudio. Convertendo, 20 vira 65 e o volume que ela tinha continua sendo o
     * volume que ela tem.
     */
    public static int posicaoDe(double ganhoLinear) {
        if (ganhoLinear <= A) {
            // Abaixo do pé da curva não há posição equivalente; o mais perto é o
            // trecho da rampa, que é linear em cima de A.
            return (int) Math.round(Math.max(0, ganhoLinear / A * RAMPA) * 100);
        }
        double x = Math.log(ganhoLinear / A) / B;
        return (int) Math.round(Math.max(0, Math.min(1, x)) * 100);
    }
}
