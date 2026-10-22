package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.particles.OndaDeChoque;
import com.retronova.game.objects.particles.Word;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

/**
 * A arma velha vira a nova, na mão do gato: um estalo curto, e não uma cerimônia.
 *
 * A VERSÃO ANTERIOR PARAVA A CENA POR 2,3 SEGUNDOS: a arma subia acima da cabeça,
 * branqueava, ficava parada no alto e descia. Bonita uma vez; do lado de quem joga,
 * dois segundos com a arma fora da mão no meio da horda, logo depois de uma tela
 * de escolha que já tinha parado o jogo. A recompensa já foi sentida na carta —
 * o que falta aqui é só CONFIRMAR o que mudou, e confirmação é rápida.
 *
 * O QUE OS ROGUELIKES RÁPIDOS FAZEM. No Vampire Survivors e no Brotato a arma nova
 * aparece num piscar, com um brilho e um texto, e o jogo nunca para; no Hades a
 * bênção nova dá um clarão na cor do deus em volta do Zagreu e ele já está
 * andando. O "Juice it or lose it" (Jonasson e Purho) e o "Art of Screenshake"
 * (Nijman) chegam ao mesmo lugar: o que vende o impacto é um quadro de luz, uma
 * escala que estoura e volta e uma onda — tudo em décimos de segundo. E a Nielsen
 * Norman mede ~0,1 s como "instantâneo" e ~0,4 s como o ponto em que a espera
 * começa a ser notada: a animação inteira fica abaixo disso.
 *
 * AS DUAS FASES:
 *
 *   ESTOURAR  a arma velha dá um pulo curto para cima e branqueia. No pico da
 *             luz ela vira a nova — a troca acontece escondida no branco — e
 *             saem o anel na cor do elemento e o NOME da arma nova, que sobe
 *             sozinho por mais um segundo: é ele que diz qual arma veio, sem
 *             prender o gato esperando.
 *   ASSENTAR  a arma nova volta à mão, um pouco maior do que é, e encolhe até o
 *             tamanho dela com um passo além (ease-out-back), perdendo o branco.
 *
 * NADA DISSO BLOQUEIA: o gato anda, ataca e usa habilidades o tempo todo. A arma
 * só é desenhada pela animação em vez de pela mão, por uns 18 quadros.
 *
 * Todos os números de ajuste fino estão no topo.
 */
public final class Transmutacao {

    // ----------------------------------------------------------- ajuste fino

    /** Ticks do pulo até o pico da luz, onde a arma troca (6 = 0,1 s). */
    public static final int ESTOURAR = 6;
    /** Ticks da volta à mão, perdendo o branco e o tamanho (12 = 0,2 s). */
    public static final int ASSENTAR = 12;
    /** Quanto a arma pula acima do centro do gato, em pixels de arte. */
    public static final int ALTURA = 6;
    /** Escala da arma nova no pico, antes de encolher para 1. */
    public static final double ESTOURO = 1.4;
    /** Segundos que o nome da arma nova fica subindo sobre o gato. */
    public static final double SEGUNDOS_DO_NOME = 1.2;
    /** Raio do anel na cor do elemento, em tiles, e a opacidade dele (0 a 1). */
    public static final double RAIO_DO_ANEL = 1.6;
    public static final float FORCA_DO_ANEL = 0.7f;

    private static final int TOTAL = ESTOURAR + ASSENTAR;

    private final BufferedImage antes;
    private final BufferedImage depois;
    private final Sounds doElemento;
    private final Color cor;
    private final String nome;
    private int t;

    /**
     * @param cor  a cor do elemento: anel e nome
     * @param nome o nome da arma nova, que sobe sobre o gato
     */
    public Transmutacao(BufferedImage antes, BufferedImage depois, Sounds doElemento, Color cor, String nome) {
        this.antes = antes;
        this.depois = depois;
        this.doElemento = doElemento;
        this.cor = cor;
        this.nome = nome;
        // SEM SOM DE RECOMPENSA AQUI. Tocava o arpejo de conquista; agora quem
        // diz "você ganhou" é a confirmação do elemento, na escolha (ver
        // EscolhaDeElemento), meio segundo antes. Dois sons de recompensa seguidos
        // brigam pela mesma atenção. O que fica é o som do elemento no quadro da
        // troca (ver tick), o primeiro golpe com o timbre novo.
    }

    public boolean acabou() {
        return t >= TOTAL;
    }

    /**
     * @param centroX centro do gato, em pixels de mundo, para o anel e o nome
     * @param centroY idem
     */
    public void tick(double centroX, double centroY) {
        if (acabou()) {
            return;
        }
        if (t == ESTOURAR) {
            // O QUADRO DA TROCA: o som do elemento (a primeira vez que se ouve o
            // timbre que acompanha o resto da corrida), o anel e o nome.
            if (doElemento != null) {
                Sound.play(doElemento);
            }
            if (cor != null) {
                Game.getMap().put(new OndaDeChoque(centroX, centroY, RAIO_DO_ANEL * GameObject.SIZE(),
                        cor, FORCA_DO_ANEL));
            }
            if (nome != null) {
                Game.getMap().put(new Word(nome, centroX, centroY - GameObject.SIZE(), SEGUNDOS_DO_NOME,
                        cor == null ? Color.WHITE : clara(cor)));
            }
        }
        t++;
    }

    /** O tempo da animação com a fração do quadro, para ela fluir acima de 60 FPS. */
    private double agora() {
        return Math.min(TOTAL, t + Engine.alpha());
    }

    /** De 0 (na mão) a 1 (no alto): sobe desacelerando, desce desacelerando. */
    private double altura(double f) {
        if (f < ESTOURAR) {
            double p = f / ESTOURAR;
            return 1 - Math.pow(1 - p, 2);
        }
        double p = (f - ESTOURAR) / ASSENTAR;
        return Math.pow(1 - p, 2);
    }

    /** O branco: sobe até o pico da troca e some na primeira metade do assentar. */
    private float luz(double f) {
        if (f < ESTOURAR) {
            return (float) (f / ESTOURAR);
        }
        return (float) Math.max(0, 1 - (f - ESTOURAR) / (ASSENTAR * 0.5));
    }

    /** A escala: 1 antes da troca; depois, de ESTOURO até 1 com um passo além. */
    private double escala(double f) {
        if (f < ESTOURAR) {
            return 1;
        }
        double p = Math.min(1, (f - ESTOURAR) / ASSENTAR);
        double c1 = 1.7, c3 = c1 + 1;
        double volta = 1 + c3 * Math.pow(p - 1, 3) + c1 * Math.pow(p - 1, 2);   // 0 -> 1, ease-out-back
        return ESTOURO + (1 - ESTOURO) * volta;
    }

    /**
     * Desenha a arma NA POSE DA MÃO, deslocada para cima e escalada a partir da
     * mão. Partir da pose de porte, e não de uma arma em pé no centro do gato, é
     * o que deixa a animação começar e terminar sem salto: no primeiro e no
     * último quadro ela é exatamente a arma que estava e a que fica.
     *
     * @param mao   onde fica a mão do gato, em pixels de mundo
     * @param naMao quem desenha um sprite na pose de porte (ver Item.renderNaMao)
     */
    public void render(Graphics2D g, Point mao, BiConsumer<Graphics2D, BufferedImage> naMao) {
        double f = agora();
        BufferedImage arma = f < ESTOURAR ? antes : depois;
        if (arma == null) {
            return;
        }
        int px = Configs.GameScale();
        double esc = escala(f);
        float luz = luz(f);
        BufferedImage desenho = luz > 0.01f ? Expressao.clarao(arma, luz) : arma;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.translate(mao.x, mao.y - ALTURA * px * altura(f));
        g2.scale(esc, esc);
        g2.translate(-mao.x, -mao.y);
        naMao.accept(g2, desenho);
        g2.dispose();
    }

    private static Color clara(Color c) {
        return new Color(Math.min(255, c.getRed() + 60), Math.min(255, c.getGreen() + 60),
                Math.min(255, c.getBlue() + 60));
    }
}
