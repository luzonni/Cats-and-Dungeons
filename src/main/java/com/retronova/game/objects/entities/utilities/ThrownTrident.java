package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * O tridente no ar: vai, bate em quem encontra, e volta.
 *
 * POR QUE VIROU ENTIDADE. Antes o arremesso era simulado dentro do proprio item,
 * com a distancia saindo de {@code Investida.avanco()} — que vai de MENOS UM a
 * mais um, porque foi feita para golpe corpo a corpo, onde o menos um e o braco
 * recuando antes do talho. Multiplicado por seis tiles de alcance, esse menos um
 * punha o tridente seis tiles ATRAS do gato: era isso que parecia "nascer do
 * outro lado". E o dano so era testado num unico quadro, o do impacto teorico, na
 * extensao maxima — o inimigo teria de estar exatamente na ponta do alcance
 * naquele frame, entao na pratica a arma nao dava dano nenhum.
 *
 * Nada disso se conserta ajustando numero. Arma que sai da mao e volta e um
 * PROJETIL, e projetil e uma entidade com posicao propria: e assim que bumerangue
 * e tridente funcionam em Minecraft, Terraria e afins. Duas fases, as duas em linha
 * RETA: ida ate o alcance ou ate bater na parede, e volta refazendo a mesma reta
 * de tras para a frente. Voltar perseguindo o gato descrevia uma curva, e curva
 * le como bumerangue — a referencia aqui e o martelo do Thor, que volta direto.
 *
 * Cada inimigo leva UMA pancada por arremesso. Sem isso, um bicho parado no meio
 * do caminho tomaria dano a cada quadro em que o tridente o atravessa, na ida e
 * na volta, e a arma viraria uma serra.
 */
public class ThrownTrident extends Projetil {

    private final Player dono;
    private final double dano;
    private final double alcance;
    private final double origemX, origemY;

    private double angulo;
    private boolean voltando;
    private boolean acabou;

    /** Quem ja levou pancada NESTE arremesso. */
    private final Set<Entity> atingidos = new HashSet<>();

    /**
     * Trava de seguranca, em ticks.
     *
     * A volta e uma reta, e uma reta pode nao encontrar ninguem: se o gato andou
     * muito e a arma passou longe da origem sem chegar perto dele, nada a
     * encerraria. O limite existe para isso, e para o caso de o dono morrer.
     */
    private static final int LIMITE_DE_VIDA = 60 * 3;
    private int vida;

    /**
     * Velocidade, em pixels de arte por tick.
     *
     * Mais lento que a flecha (7) de proposito: o tridente e pesado e o jogador
     * precisa VER a ida e a volta para entender que a arma vai retornar. A esta
     * velocidade os seis tiles de ida levam pouco mais de meio segundo.
     */
    private static final double VELOCIDADE = 4.0;

    public ThrownTrident(Player dono, double x, double y, double angulo,
                         double dano, double alcance) {
        super(x, y, 0, dono);
        this.dono = dono;
        this.angulo = angulo;
        this.dano = dano;
        this.alcance = alcance;
        this.origemX = x;
        this.origemY = y;
        loadSprites("trident");
        setSpeed(VELOCIDADE);
    }

    /** Ja voltou (ou desistiu)? O item so relanca depois disso. */
    public boolean acabou() {
        return acabou;
    }

    @Override
    public void tick() {
        if (++vida > LIMITE_DE_VIDA) {
            encerrar();
            return;
        }

        // BATE PRIMEIRO, decide o fim depois.
        //
        // Com o teste de parede na frente, um inimigo ENCOSTADO no muro nunca
        // levava dano na volta: no mesmo tick em que o tridente o alcancava ele
        // tambem raspava o bloco, e o encerrar saia do metodo antes do golpe.
        bater();

        if (!voltando) {
            double andou = Math.hypot(meioX() - origemX, meioY() - origemY);
            // Bateu na parede tambem faz voltar: e mais legivel que sumir, e
            // ainda devolve a arma ao jogador em vez de puni-lo pela mira.
            if (andou >= alcance || bateuNaParede()) {
                voltando = true;
                // VOLTA RETO, refazendo a propria linha — nao perseguindo o gato.
                // Perseguir descrevia uma curva, e curva le como bumerangue. A
                // referencia e o tridente do Minecraft e o martelo do Thor: a
                // arma volta na mesma reta em que foi, direto para a mao.
                angulo += Math.PI;
            }
        } else {
            // BATEU NA PAREDE NA VOLTA: acaba ali.
            //
            // A volta e uma reta, e reta nao desvia. Se o gato sai da frente, a
            // reta termina numa parede — e ate agora o tridente ficava empurrando
            // contra ela ate o limite de vida estourar ou o jogador ir busca-lo.
            // Encerrar devolve a arma a mao na hora, que e o que o jogador espera
            // de uma arma que volta.
            if (bateuNaParede()) {
                encerrar();
                return;
            }
            // Acaba ao chegar na mao OU ao completar a volta ate onde foi jogada.
            // Sao duas condicoes porque o gato pode ter andado: sem a segunda, um
            // tridente arremessado por alguem que saiu do lugar voaria reto para
            // sempre, ja que a reta de volta nao passa mais por ele.
            double aoDono = Math.hypot(dono.getX() + dono.getWidth() / 2d - meioX(),
                    dono.getY() + dono.getHeight() / 2d - meioY());
            double aOrigem = Math.hypot(meioX() - origemX, meioY() - origemY);
            if (aoDono <= GameObject.SIZE() * 0.75 || aOrigem <= GameObject.SIZE() * 0.5) {
                encerrar();
                return;
            }
        }

        // A forca e reaplicada todo tick com o mesmo nome: o Physical troca o
        // vetor existente em vez de somar um novo. Sem isso a forca decairia com
        // o atrito e o tridente iria morrendo no meio do caminho.
        getPhysical().addForce("arremesso", getSpeed(), angulo);
    }

    /**
     * Bate em quem cruza o CAMINHO do tick, e nao em quem encosta na posicao.
     *
     * Com a caixa reduzida a seis pixels, testar so a posicao deixaria o tridente
     * passar por dentro de um inimigo entre dois quadros sem tocar nele. A
     * varredura resolve isso; um inimigo por tick e o bastante, porque o proximo
     * tick varre o trecho seguinte.
     */
    private void bater() {
        Enemy alvo = atingido(Enemy.class);
        marcarPosicao();
        if (alvo == null || atingidos.contains(alvo)) {
            return;
        }
        atingidos.add(alvo);
        alvo.strike(AttackTypes.Piercing, dano);
        alvo.getPhysical().addForce("knockback", 3, alvo.getAngle(this));
    }

    private void encerrar() {
        this.acabou = true;
        disappear();
    }

    @Override
    public void render(Graphics2D g) {
        // Aponta para onde esta indo. Um tridente girando no ar ficaria bonito,
        // mas a 16 pixels o giro come a leitura dos tres dentes, que e a unica
        // coisa que diferencia esta arma de um cabo de vassoura.
        // O tridente novo vem do pacote, desenhado na diagonal — o desenho a mao
        // anterior era em pe, e por isso usava PARA_CIMA.
        Rotate.apontar(getSprite(), meioX(), meioY(), angulo, Rotate.DIAGONAL, g);
    }
}
