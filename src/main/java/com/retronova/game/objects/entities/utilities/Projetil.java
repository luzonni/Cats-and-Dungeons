package com.retronova.game.objects.entities.utilities;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.List;

/**
 * Base de tudo que voa: flecha, kunai, tridente, bola de magia.
 *
 * POR QUE ELA EXISTE — o que estava errado antes.
 *
 * Cada projetil era uma Utility solta, e herdava do GameObject uma caixa de
 * colisao de UM TILE INTEIRO: 16 por 16 pixels de arte, 64 na tela. Um projetil
 * do tamanho de um tile quebra tres coisas de uma vez, e as tres foram sentidas
 * jogando:
 *
 *  1. NASCIA JA COLIDINDO. A caixa aparecia centrada no gato e encostava em tudo
 *     num raio de meio tile. Inimigo ao lado levava dano no primeiro quadro, sem
 *     o tiro sair — "se aparece algum do meu lado, ela taga instantaneamente".
 *
 *  2. MORRIA NA GEOMETRIA. Num corredor de um tile de largura, uma caixa de um
 *     tile raspa a parede em qualquer trajetoria que nao seja perfeitamente
 *     alinhada. O Physical marcava crashing e o projetil sumia no primeiro tick —
 *     "sai a animacao mas nem chega no inimigo".
 *
 *  3. SO FUNCIONAVA DE PERTO. Como quase todo tiro morria cedo, o unico caso que
 *     dava certo era o alvo a poucos pixels.
 *
 * O QUE E FEITO AGORA, e por que.
 *
 * CAIXA PEQUENA. Seis pixels de arte, contra os dezesseis do quadro. E a regra
 * corrente em jogo 2D — a caixa e menor que o desenho — e aqui ela e o que
 * permite ao projetil atravessar um vao de um tile sem raspar.
 *
 * COLISAO POR VARREDURA. Caixa pequena e velocidade alta trazem o problema
 * oposto, o tunelamento: entre um quadro e outro o projetil pula por cima do
 * inimigo e nao encosta em nenhum dos dois instantes. A solucao padrao e nao
 * testar a POSICAO e sim o CAMINHO — o segmento entre onde estava e onde esta.
 * E o mesmo raciocinio de raycast que as engines usam para bala rapida.
 *
 * DONO IGNORADO E NASCIMENTO ADIANTADO. O tiro sai da ponta da arma, ja fora do
 * corpo de quem atirou, e quem atirou nunca e alvo — senao o projetil se mata no
 * proprio dono antes de andar um pixel.
 */
public abstract class Projetil extends Utility {

    /** Lado da caixa de colisao, em pixels de arte. O quadro tem dezesseis. */
    static final double LADO = 6;

    /** O mesmo lado, em pixels de tela — a largura do corredor que ele precisa. */
    public static double larguraEmPixels() {
        return GameObject.SIZE() * (LADO / 16d);
    }

    private double antesX, antesY;
    private final Entity dono;

    /**
     * @param centroX,centroY onde o projetil NASCE, pelo centro dele — e nao pelo
     *                        canto. Canto e a origem natural do GameObject, mas
     *                        quem atira pensa em "sai daqui", e fazer cada chamador
     *                        descontar meia caixa era onde os erros apareciam.
     */
    protected Projetil(double centroX, double centroY, int peso, Entity dono) {
        super(centroX, centroY, peso);
        this.dono = dono;
        setWidth(LADO / 16d);
        setHeight(LADO / 16d);
        setX(centroX - getWidth() / 2d);
        setY(centroY - getHeight() / 2d);
        this.antesX = centroX;
        this.antesY = centroY;
    }

    protected double meioX() {
        return getX() + getWidth() / 2d;
    }

    protected double meioY() {
        return getY() + getHeight() / 2d;
    }

    /**
     * O primeiro alvo no CAMINHO percorrido neste tick, ou null.
     *
     * Testa o segmento de onde o projetil estava ate onde chegou, e nao so a
     * posicao de agora. Sem isso, projetil rapido com caixa pequena atravessa o
     * inimigo entre dois quadros sem nunca encostar nele.
     *
     * Deve ser chamado UMA VEZ por tick, depois de mover: ele consome o registro
     * da posicao anterior.
     */
    protected <T extends Entity> T atingido(Class<T> tipo) {
        Line2D percurso = new Line2D.Double(antesX, antesY, meioX(), meioY());
        T achado = null;
        double maisPerto = Double.MAX_VALUE;
        List<Entity> todas = Game.getMap().getEntities();
        for (int i = 0; i < todas.size(); i++) {
            Entity e = todas.get(i);
            if (e == this || e == dono || !tipo.isInstance(e)) {
                continue;
            }
            // Nenhum projetil para em outra Utility. Moeda, XP, item caido e os
            // proprios tiros sao todos Utility: pedindo Entity.class — que e o que
            // a flecha faz, para valer contra qualquer coisa viva — uma moeda no
            // chao engoliria o tiro no meio do caminho.
            if (e instanceof Utility) {
                continue;
            }
            if (!noCaminho(comFolga(e.getBounds(), larguraEmPixels()), percurso)) {
                continue;
            }
            // O MAIS PROXIMO DA ORIGEM vence. Num tick que cruza dois inimigos, o
            // acerto tem de ser no primeiro; pegar qualquer um faz o tiro parecer
            // que atravessou alguem.
            double d = Math.hypot(e.getX() + e.getWidth() / 2d - antesX,
                    e.getY() + e.getHeight() / 2d - antesY);
            if (d < maisPerto) {
                maisPerto = d;
                achado = tipo.cast(e);
            }
        }
        return achado;
    }

    /**
     * O caminho cruza esta caixa?
     *
     * Isolado num metodo estatico de proposito: e a regra que separa o projetil
     * que acerta do que atravessa o inimigo sem tocar nele, e assim ela pode ser
     * verificada em teste sem jogo carregado.
     */
    /**
     * A caixa do alvo CRESCIDA de meia largura do projetil.
     *
     * O percurso e uma linha sem espessura, mas o projetil tem vinte e quatro
     * pixels de lado. Sem a folga, um tiro cuja borda cobre o inimigo — na tela,
     * um acerto limpo — passa como erro porque o fio do meio passou de raspao.
     * Crescer o alvo em vez de engrossar a linha e a soma de Minkowski, que e como
     * se testa caixa contra caixa em movimento.
     *
     * Como efeito colateral util, a folga tambem absorve o pouco que o inimigo
     * andou durante o tick, que e a queixa de "erra se o bicho se mexe".
     */
    static Rectangle comFolga(Rectangle caixa, double larguraDoProjetil) {
        int folga = (int) Math.round(larguraDoProjetil / 2d);
        return new Rectangle(caixa.x - folga, caixa.y - folga,
                caixa.width + folga * 2, caixa.height + folga * 2);
    }

    static boolean noCaminho(Rectangle alvo, Line2D percurso) {
        // Um percurso de comprimento zero — o projetil parado, ou o primeiro tick
        // antes de andar — nao e segmento nenhum para o intersectsLine. Ai vale o
        // teste de sobreposicao simples.
        if (percurso.getP1().equals(percurso.getP2())) {
            return alvo.contains(percurso.getP1());
        }
        return alvo.intersectsLine(percurso);
    }

    /** Guarda onde o projetil esta, para a varredura do proximo tick. */
    protected void marcarPosicao() {
        this.antesX = meioX();
        this.antesY = meioY();
    }

    private int vida;
    private boolean acabou;

    /**
     * Um tick de voo: devolve quem foi atingido no caminho, ou null.
     *
     * A ORDEM AQUI E O CONSERTO. Antes cada projetil testava a parede primeiro e
     * saia do metodo; um inimigo ENCOSTADO no muro entao nunca levava dano, porque
     * no mesmo tick em que o tiro alcancava o bicho ele tambem raspava o bloco — e
     * a parede ganhava. Era exatamente o "chega perto e nao da em nada".
     *
     * Testar o acerto primeiro e deixar o fim para depois resolve, e por isso a
     * ordem mora AQUI, na base, e nao repetida em cada arma: era um detalhe facil
     * demais de inverter sem querer.
     */
    protected <T extends Entity> T avancar(Class<T> tipo, int limiteDeVida) {
        T alvo = atingido(tipo);
        marcarPosicao();
        this.acabou = ++vida > limiteDeVida || bateuNaParede();
        return alvo;
    }

    /** O voo terminou — bateu na parede ou passou do tempo. Cheque DEPOIS do acerto. */
    protected boolean acabou() {
        return this.acabou;
    }

    /** Bateu em parede? O Physical ja testa a caixa contra os tiles solidos. */
    protected boolean bateuNaParede() {
        return getPhysical().crashing();
    }

    /** Quanto anda por tick, em pixels de tela — para dimensionar limites de vida. */
    protected static double umTile() {
        return GameObject.SIZE();
    }
}
