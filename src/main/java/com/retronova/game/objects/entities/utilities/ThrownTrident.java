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
 * e tridente funcionam em Minecraft, Terraria e afins.
 *
 * A VOLTA E PARA A MAO, E NAO PARA O PONTO DE LANCAMENTO.
 *
 * A primeira versao voltava em linha reta, refazendo a propria ida ao contrario.
 * O raciocinio era que curva le como bumerangue e o tridente nao e bumerangue —
 * mas o efeito na pratica era outro: o gato joga, ANDA (que e o que se faz o
 * tempo todo neste jogo), e a arma volta para onde ele estava, nao para onde ele
 * esta. Era isso que parecia "guardar a posicao de onde foi jogado". Um par de
 * condicoes de encerramento tapava o buraco fazendo a arma sumir perto da origem,
 * o que so trocava um defeito visivel por um invisivel.
 *
 * Agora a volta MIRA no gato a cada quadro. E o que o tridente da lealdade do
 * Minecraft faz, e e a unica versao em que a arma sempre chega — a reta so
 * funcionava enquanto ninguem se mexesse. A curva que aparece e pequena, porque o
 * gato raramente andou muito em meio segundo, e ela LE bem: a arma parece
 * procurar o dono.
 *
 * DA DANO NA IDA E NA VOLTA, uma pancada por bicho em cada perna do trajeto. Sao
 * duas passagens de verdade — a arma cruza o campo duas vezes — e cobrar so a
 * primeira desperdicava metade do voo. Sem a divisao em pernas, porem, um bicho
 * parado na linha tomaria dano a cada quadro em que fosse atravessado, e a arma
 * viraria uma serra: por isso a lista de atingidos e ZERADA na virada, e nao
 * simplesmente abandonada.
 *
 * E o que isso faz com a arma vale a pena dizer: acertar na ida e um problema de
 * mira, acertar na volta e um problema de POSICIONAMENTO — depende de onde o gato
 * esta quando ela retorna. Sao duas decisoes diferentes no mesmo arremesso.
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
                // A LISTA E ZERADA, e nao esquecida: quem levou pancada na ida pode
                // levar outra na volta, mas so uma por perna. Zerar aqui e o que
                // separa "duas passagens" de "uma serra".
                atingidos.clear();
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
            // MIRA NO GATO A CADA QUADRO. E a linha que faz a arma voltar para onde
            // o dono ESTA, e nao para onde ele estava quando arremessou.
            double aoDonoX = dono.getX() + dono.getWidth() / 2d - meioX();
            double aoDonoY = dono.getY() + dono.getHeight() / 2d - meioY();
            angulo = Math.atan2(aoDonoY, aoDonoX);
            // UMA CONDICAO DE FIM, e nao mais duas. A segunda — "chegou perto de
            // onde foi jogada" — existia so para o caso de a reta nao passar mais
            // pelo gato, que era consequencia de a volta ser reta. Com a arma
            // mirando nele, chegar na mao e a unica maneira de o voo terminar bem,
            // e e a unica que o jogador consegue prever.
            if (Math.hypot(aoDonoX, aoDonoY) <= GameObject.SIZE() * 0.75) {
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
     *
     * A lista de atingidos vale por PERNA do trajeto — ela e zerada na virada —,
     * entao um bicho parado na linha leva uma pancada quando a arma passa e outra
     * quando ela volta.
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
