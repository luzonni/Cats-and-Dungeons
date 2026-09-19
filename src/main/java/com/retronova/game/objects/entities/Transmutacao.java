package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Alpha;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A arma velha vira a nova, na mão do gato.
 *
 * POR QUE UMA ANIMAÇÃO, E NÃO SÓ TROCAR O SPRITE.
 *
 * Escolher o elemento é a decisão mais importante da corrida, e sem cerimônia ela
 * acontecia num quadro: o jogador clicava numa carta e a espada simplesmente era
 * outra. Trocar sem mostrar a troca desperdiça o único momento em que o jogo pode
 * dizer "isto que você acabou de fazer importa" — e é justamente o que jogos como
 * o Stardew Valley entenderam ao levantar o objeto acima da cabeça em vez de
 * empurrá-lo direto para a mochila. A pausa É a recompensa.
 *
 * NADA NOVO FOI DESENHADO. As duas armas já existem — a comum e a elemental, que
 * são silhuetas diferentes — e o clarão branco é o mesmo que o gato usa ao levar
 * pancada. A animação só junta o que estava solto: tirar da mão, subir, apagar na
 * luz, e descer sendo outra coisa.
 *
 * AS QUATRO FASES, e por que cada uma dura o que dura:
 *
 *   SUBIR (34)    a arma deixa a mão e vai até acima da cabeça, desacelerando no
 *                 fim. É a antecipação: sem ela a luz chegaria antes de o olho
 *                 saber onde olhar.
 *   ACENDER (26)  branqueia até a silhueta sumir na luz. A troca acontece no pico,
 *                 escondida — o que o jogador vê é uma coisa virando outra, e não
 *                 um sprite sendo substituído por outro.
 *   SEGURAR (46)  a arma nova fica parada, ainda clara, descendo o branco. É aqui
 *                 que ela é LIDA. É a fase mais longa das quatro, de propósito:
     *                 é a única em que não acontece nada além de a arma nova ser
     *                 olhada, e recompensa que passa rápido não é sentida como
     *                 recompensa.
 *   DESCER (30)   volta para a mão, e o jogo continua.
 */
public final class Transmutacao {

    // OS TEMPOS DOBRARAM. A primeira versao fechava em 72 quadros — pouco mais de
    // um segundo — e o relato foi que nao dava tempo de SENTIR que se ganhou algo.
    // Estava certo: um segundo e o suficiente para ver a troca acontecer, e nao
    // para ela virar um momento. O que faz uma recompensa parecer recompensa e o
    // jogo PARAR nela, e a parte que mais cresceu aqui e justamente a de segurar a
    // arma nova parada no alto.
    private static final int SUBIR = 34;
    private static final int ACENDER = 26;
    private static final int SEGURAR = 46;
    private static final int DESCER = 30;
    private static final int TOTAL = SUBIR + ACENDER + SEGURAR + DESCER;

    /** Quanto a arma sobe acima do centro do gato, em pixels de arte. */
    private static final int ALTURA = 22;

    private final BufferedImage antes;
    private final BufferedImage depois;
    private int t;

    private final Sounds doElemento;

    public Transmutacao(BufferedImage antes, BufferedImage depois, Sounds doElemento) {
        this.antes = antes;
        this.depois = depois;
        this.doElemento = doElemento;
        // O ARPEJO DE CONQUISTA, e nao o estalo generico de portal. O portal diz
        // "alguma coisa apareceu"; este diz "voce ganhou alguma coisa", que e o que
        // de fato esta acontecendo. Ver GenSonsDeCena.
        Sound.play(Sounds.Conquista);
    }

    public boolean acabou() {
        return t >= TOTAL;
    }

    public void tick() {
        if (acabou()) {
            return;
        }
        // O PICO DA LUZ NAO LEVA MAIS O ESTALO.
        //
        // Ele estava aqui para marcar o instante da troca, e marcava — mas o
        // arquivo e um estalo seco de casca quebrando, o mesmo que o portal usa, e
        // em cima do arpejo de conquista ele soava como pisar em mato. Dois sons
        // disputando o mesmo quadro nao dobram a enfase; um atrapalha o outro. O
        // arpejo ja diz "voce ganhou algo", que e a frase inteira.
        //
        // O som do ELEMENTO fica, porque nao e reforco: e a primeira vez que se
        // ouve o timbre que vai acompanhar o resto da corrida.
        if (t == SUBIR + ACENDER) {
            if (doElemento != null) {
                // O SOM DO ELEMENTO NO QUADRO DA TROCA. E o instante em que a arma
                // deixa de ser a de fabrica e passa a ser a daquela corrida, e e a
                // primeira vez que o jogador ouve o som que vai acompanhar o resto
                // da partida.
                Sound.play(doElemento);
            }
        }
        t++;
    }

    /** De 0 (na mão) a 1 (no alto), com desaceleração no fim da subida. */
    private double altura() {
        if (t < SUBIR) {
            double f = t / (double) SUBIR;
            return 1 - Math.pow(1 - f, 3);
        }
        if (t < SUBIR + ACENDER + SEGURAR) {
            return 1;
        }
        double f = (t - SUBIR - ACENDER - SEGURAR) / (double) DESCER;
        return 1 - f;
    }

    /**
     * O quanto a arma está branca, de 0 a 1.
     *
     * Sobe durante o ACENDER e desce durante o SEGURAR. O pico dura um quadro só —
     * é nele que a arma é trocada, e é por isso que a substituição não se vê.
     */
    private float luz() {
        if (t < SUBIR) {
            return 0f;
        }
        if (t < SUBIR + ACENDER) {
            return (t - SUBIR) / (float) ACENDER;
        }
        if (t < SUBIR + ACENDER + SEGURAR) {
            return 1f - (t - SUBIR - ACENDER) / (float) SEGURAR;
        }
        return 0f;
    }

    /** Antes do pico é a arma velha; do pico em diante, a nova. */
    private BufferedImage arma() {
        return t < SUBIR + ACENDER ? antes : depois;
    }

    public void render(Graphics2D g, int centroX, int centroY) {
        BufferedImage arma = arma();
        if (arma == null) {
            return;
        }
        int px = Configs.GameScale();
        int y = centroY - (int) (ALTURA * px * altura());
        int x = centroX - arma.getWidth() / 2;

        float luz = luz();
        BufferedImage desenho = luz > 0.01f ? Expressao.clarao(arma, luz) : arma;
        // A ARMA NOVA ENTRA APARECENDO. No quadro da troca ela nasce invisível e
        // ganha corpo junto com a queda do branco; sem isso, o pico da luz seria
        // seguido por um sprite surgindo pronto, que lê como corte de edição.
        if (t >= SUBIR + ACENDER && t < SUBIR + ACENDER + 4) {
            Alpha.draw(desenho, x, y - arma.getHeight() / 2,
                    (t - SUBIR - ACENDER) / 4f, g);
            return;
        }
        g.drawImage(desenho, x, y - arma.getHeight() / 2, null);
    }
}
