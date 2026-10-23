package com.retronova.game.items;

import com.retronova.game.objects.entities.Player;

/**
 * O MOVIMENTO DA ARMA NA MAO: respiro, passo, virada, flutuacao e o gesto do
 * ataque. Uma por arma, andada pelo tick do gato e lida pelo desenho.
 *
 * POR QUE UMA CAMADA SO. Antes cada arma era um adesivo preso a pata: parada,
 * andando ou atacando, ela ficava no mesmo pixel ate o golpe comecar, e ai pulava
 * para a pose de ataque. O que faz uma arma parecer segurada e o mesmo para
 * todas — ela respira com o gato, pesa no passo, se recolhe quando ele vira —, e
 * isso e escrito uma vez aqui e somado pela {@link Item#naMao} a pose de porte.
 * Cada arma so diz QUANDO ataca ({@link #preparar}, {@link #disparou}) e QUANTO o
 * gesto dela mexe ({@link Gesto}).
 *
 * O GESTO SEGUE A REGRA DE TODO GOLPE (antecipacao, acao, recuperacao), e o ponto
 * da acao e o tick REAL do disparo: a arma avisa no tick em que o projetil nasce,
 * e o pico do gesto cai nesse quadro. A antecipacao e o tempo que ja falta para o
 * proximo tiro, contado pela propria arma — nada aqui adianta ou atrasa o ataque.
 *
 * TEMPO FIXO MAIS INTERPOLACAO. Tudo anda em ticks de 60 Hz, como o resto do
 * jogo, e o desenho mistura o tick anterior com o atual pela fracao
 * {@code alpha} (ver Engine.alpha): a 144 ou 240 Hz o balanco e continuo, e a
 * velocidade nao depende da taxa de quadros. Nenhum objeto e criado por quadro:
 * {@link #calcular} escreve em campos.
 */
public final class AnimacaoDaArma {

    // ------------------------------------------------------------ numeros
    //
    // Em graus e em PIXELS DE ARTE (multiplicados pela escala do mundo no
    // desenho). Ver a tabela de animacao em docs/CONTEXTO.md, secao 8.

    /** Balanco do respiro parado, em graus para cada lado. */
    static final double RESPIRO_GRAUS = 2.5;
    /** Balanco da caminhada, em graus: a ponta vai para a frente num passo e volta no outro. */
    static final double PASSO_GRAUS = 6;
    /** Quanto a arma pesa no contato do passo, em pixels de arte. */
    static final double PASSO_PX = 1;
    /**
     * Quanto do caminho ate o peso do passo a arma anda por tick.
     *
     * MENOS QUE UM, DE PROPOSITO: a arma chega um tick depois do corpo. E a
     * acao sobreposta dos doze principios — a ponta da coisa segurada e a ultima
     * a assentar —, e e o que faz ela parecer ter peso em vez de estar colada.
     */
    static final double PASSO_ATRASO = 0.45;
    /** Quanto o peso da caminhada muda por tick ao comecar ou parar de andar. */
    static final double TROCA_DE_ANDAR = 0.15;
    /** Ticks que a arma leva para se recolher e voltar quando o gato vira. */
    static final int VIRADA_TICKS = 7;
    /** O quanto ela se recolhe no meio da virada, em graus. */
    static final double VIRADA_GRAUS = 35;
    /** Flutuacao das armas de magia: altura e periodo, em pixels de arte e ticks. */
    static final double FLUTUA_PX = 1.5;
    static final int FLUTUA_PERIODO = 100;
    /** Quanto a antecipacao alcanca do alvo por tick. Tira o salto quando o alvo some. */
    static final double PREPARO_SUAVE = 0.3;
    /** Sem disparo recente. */
    private static final int PARADA = Integer.MAX_VALUE / 2;

    /**
     * Quanto o gesto de ataque de uma arma mexe.
     *
     * @param acao        ticks da antecipacao ate o pico; curtos, e o golpe
     * @param recuperacao ticks do pico de volta ao porte
     * @param grausAtras  giro no auge da antecipacao (positivo = recolhe)
     * @param grausFrente giro no pico da acao (positivo = estende)
     * @param pxAtras     recuo no auge da antecipacao, em pixels de arte
     * @param pxFrente    avanco no pico, em pixels de arte
     * @param subirPx     quanto a arma sobe na antecipacao — o cajado erguido
     */
    public record Gesto(int acao, int recuperacao, double grausAtras, double grausFrente,
                        double pxAtras, double pxFrente, double subirPx) {
        /** Arma que nao avisa os disparos: o gesto fica parado. */
        public static final Gesto NENHUM = new Gesto(1, 1, 0, 0, 0, 0, 0);
    }

    private Gesto gesto = Gesto.NENHUM;
    private boolean flutua;

    // ------------------------------------------------------------ estado
    private long t;
    private double andar, andarAntes;
    private double peso, pesoAntes;
    private double preparo, preparoAntes, preparoAlvo;
    private int desde = PARADA;
    private double preparoNoDisparo;
    private int virada;
    private int lado;

    // ------------------------------------------------------------ saida
    /** Giro a somar a pose, em graus. */
    public double graus;
    /** Avanco ao longo da pose (para a frente do gato), em pixels de arte. */
    public double avanco;
    /** Deslocamento vertical, em pixels de arte (positivo = desce). */
    public double desce;
    /** O gesto agora, de -1 (auge da antecipacao) a +1 (pico da acao). */
    public double valor;
    /** Antecipacao, de 0 a 1: carga para brilhos que crescem antes do tiro. */
    public double carga;
    /** Clarao do disparo, de 1 no tick do tiro ate 0 no fim da acao e meia recuperacao. */
    public double clarao;

    AnimacaoDaArma() {
    }

    /** Configura a arma: o gesto dela e se ela flutua (as de magia). */
    void configurar(Gesto gesto, boolean flutua) {
        this.gesto = gesto == null ? Gesto.NENHUM : gesto;
        this.flutua = flutua;
    }

    /** Ticks de relogio da animacao: o respiro e a flutuacao contam daqui. */
    public long tempo() {
        return t;
    }

    // ------------------------------------------------------------ eventos

    /**
     * Quanto falta para o proximo ataque, de 0 (longe) a 1 (agora). A arma chama
     * no tick dela; sem chamada, a antecipacao cai sozinha.
     */
    public void preparar(double fracao) {
        preparoAlvo = Math.max(0, Math.min(1, fracao));
    }

    /** O ataque saiu NESTE tick. O pico do gesto cai aqui. */
    public void disparou() {
        preparoNoDisparo = preparo;
        desde = 0;
        preparo = preparoAntes = preparoAlvo = 0;
    }

    // ------------------------------------------------------------ tick

    public void tick(Player p) {
        t++;
        if (desde < PARADA) {
            desde++;
        }
        andarAntes = andar;
        double alvo = p.andandoNaFolha() ? 1 : 0;
        andar += Math.max(-TROCA_DE_ANDAR, Math.min(TROCA_DE_ANDAR, alvo - andar));

        pesoAntes = peso;
        peso += (p.andandoNaFolha() ? p.corpoAfundado() * PASSO_PX : 0) * PASSO_ATRASO
                - peso * PASSO_ATRASO;

        preparoAntes = preparo;
        preparo += (preparoAlvo - preparo) * PREPARO_SUAVE;
        // Quem nao avisa de novo deixa a antecipacao cair: arma que perdeu o alvo
        // nao fica com o cajado erguido para sempre.
        preparoAlvo = 0;

        int agora = p.getLadoDoPasso();
        if (lado != 0 && agora != lado) {
            virada = VIRADA_TICKS;
        } else if (virada > 0) {
            virada--;
        }
        lado = agora;
    }

    // ------------------------------------------------------------ desenho

    /** Calcula a saida para este quadro. Uma chamada por desenho. */
    public void calcular(Player p, float alpha) {
        double a = Math.max(0, Math.min(1, alpha));
        double w = lerp(andarAntes, andar, a);
        double fase = p.faseDaFolha(alpha);
        double respiro = Math.sin(2 * Math.PI * (t + a) / 72d) * RESPIRO_GRAUS;
        double passo = Math.sin(2 * Math.PI * fase) * PASSO_GRAUS;
        graus = respiro * (1 - w) + passo * w;
        desce = lerp(pesoAntes, peso, a);
        if (flutua) {
            // A flutuacao nao depende do passo: e magia, nao peso. Andando ela
            // continua, mais curta, para nao brigar com o balanco.
            desce -= FLUTUA_PX * (0.5 + 0.5 * Math.sin(2 * Math.PI * (t + a) / FLUTUA_PERIODO))
                    * (1 - 0.5 * w);
        }

        double prep = lerp(preparoAntes, preparo, a);
        valor = curvaDoGesto(prep, desde + a, gesto.acao(), gesto.recuperacao(), preparoNoDisparo);
        carga = valor < 0 ? -valor : 0;
        if (valor < 0) {
            graus -= gesto.grausAtras() * -valor;
            avanco = -gesto.pxAtras() * -valor;
            desce -= gesto.subirPx() * -valor;
        } else {
            graus += gesto.grausFrente() * valor;
            avanco = gesto.pxFrente() * valor;
        }
        clarao = claraoDoDisparo(desde + a, gesto.acao(), gesto.recuperacao());

        if (virada > 0) {
            graus -= VIRADA_GRAUS * curvaDaVirada((virada - a) / VIRADA_TICKS);
        }
    }

    // ------------------------------------------------------------ curvas puras

    /**
     * O gesto no tempo: -1 no auge da antecipacao, +1 no pico, 0 em repouso.
     *
     * A ACAO PARTE DE ONDE A ANTECIPACAO ESTAVA, e nao de -1: se o tiro saiu com
     * o cajado meio erguido, o golpe comeca dali. Comecar de um ponto fixo e o
     * "pulo" no primeiro quadro do ataque que esta camada existe para tirar.
     * A acao tem saida rapida e chegada suave (ease-out cubica); a volta e um
     * smoothstep, e durante ela a proxima antecipacao ja pode estar crescendo.
     *
     * @param preparo  antecipacao atual, 0 a 1
     * @param desde    ticks desde o disparo, com fracao
     */
    static double curvaDoGesto(double preparo, double desde, int acao, int recuperacao,
                               double preparoNoDisparo) {
        if (desde >= acao + recuperacao) {
            return -preparo;
        }
        if (desde < acao) {
            double f = Math.max(0, desde) / acao;
            double e = 1 - Math.pow(1 - f, 3);
            return -preparoNoDisparo + (1 + preparoNoDisparo) * e;
        }
        double f = (desde - acao) / recuperacao;
        double e = f * f * (3 - 2 * f);
        return (1 - e) - preparo * e;
    }

    /** O clarao do disparo: cheio no tick do tiro, apagado na metade da volta. */
    static double claraoDoDisparo(double desde, int acao, int recuperacao) {
        double fim = acao + recuperacao / 2d;
        if (desde >= fim || desde < 0) {
            return 0;
        }
        return 1 - desde / fim;
    }

    /**
     * A virada: recolhe rapido e volta devagar. Recebe o que falta, de 1 a 0.
     * Em zero vale zero, entao o fim da virada encosta na pose sem degrau.
     */
    static double curvaDaVirada(double falta) {
        double f = Math.max(0, Math.min(1, falta));
        // Sobe ate 1 no primeiro terco e desce no resto: um sino assimetrico.
        return f > 0.66 ? (1 - f) / 0.34 : f / 0.66;
    }

    private static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }
}
