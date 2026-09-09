package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.KunaiThrown;

import java.awt.*;

public class Kunai extends Item {

    private double angle;
    private final double damage = 2;
    private int countAttack;

    /**
     * A kunai que esta no ar. Enquanto houver uma, a mao fica VAZIA.
     *
     * Antes ela era arremessada e continuava desenhada na mao ao mesmo tempo, o
     * que le como duplicacao: uma arma que se joga tem de sair de onde estava. Com
     * a mao vazia durante o voo e uma nova aparecendo depois, a leitura passa a ser
     * a de um punhado delas no cinto — que e o que uma kunai e.
     */
    private KunaiThrown emVoo;

    /** Ticks de mao vazia depois que a kunai some, antes de a proxima aparecer. */
    private static final int ATE_SACAR = 12;
    private int sacando;
    private final Point kunaiPosition;
    private final Point spriteRotatePosition;

    public Kunai(int id) {
        super(id, "Kunai", "kunai");
        this.kunaiPosition = new Point();
        // O punho fica onde o gerador assenta toda arma: meio de baixo do quadro.
        this.spriteRotatePosition = empunhadura(getSprite());
        addSpecifications("Throwable", "Player damage + " + this.damage, "medium speed");
    }

    @Override
    protected Porte porte() {
        return Porte.UMA_MAO;
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        // A kunai fica NA PATA, e nao orbitando o corpo. O ponto de rotacao dela
        // e a argola do cabo, no canto de baixo do sprite; encostar essa argola na
        // pata e o que faz a arma parecer segura em vez de flutuando ao lado. O
        // arremesso tambem sai dai, que e de onde a mao a soltaria.
        Point mao = player.getMao();
        this.kunaiPosition.setLocation(mao.x - spriteRotatePosition.x,
                mao.y - spriteRotatePosition.y);
        if (emVoo != null) {
            // Sumiu do mapa: acertou, bateu na parede ou expirou. Comeca a espera
            // para a proxima ser sacada.
            if (!Game.getMap().getEntities().contains(emVoo)) {
                emVoo = null;
                sacando = ATE_SACAR;
            }
            return;
        }
        if (sacando > 0) {
            sacando--;
            return;
        }
        // MIRA SEMPRE, ARREMESSA SO COM CAMINHO LIVRE.
        //
        // Enquanto as duas coisas eram a mesma, um inimigo atras do bloco fazia o
        // alvo virar nulo e o angulo parava de ser atualizado — a kunai ficava
        // apontada para o proprio gato, no ultimo valor que tinha.
        // Aponta para o mais proximo, mas ATIRA no mais proximo ALCANCAVEL: com um
        // bicho colado na parede ao lado, mirar nele e travar era a mesma coisa.
        Enemy mira = alvoParaMirar(player, player.getRange());
        Enemy target = alvoAlcancavel(player, player.getRange());
        Enemy paraApontar = target != null ? target : mira;
        if (paraApontar != null) {
            this.angle = paraApontar.getAngle(player);
        }
        if(target != null) {
            countAttack++;
            if(countAttack >= player.getAttackSpeed()*5) {
                countAttack = 0;
                double currentDamage = player.getDamage() + this.damage;
                // Sai da PONTA da lamina, e nao do canto do quadro: nascendo no
                // canto, a kunai comecava dentro do proprio gato.
                java.awt.geom.Point2D.Double ponta = boca(getSprite(),
                        kunaiPosition.x + spriteRotatePosition.x,
                        kunaiPosition.y + spriteRotatePosition.y,
                        this.angle, Rotate.DIAGONAL);
                // Mira DA PONTA DA LAMINA, que e de onde ela sai — nao do gato.
                double tiro = miraDe(ponta.x, ponta.y, target);
                this.emVoo = new KunaiThrown(ponta.x, ponta.y, currentDamage, tiro, player);
                Game.getMap().put(emVoo);
            }
        }else {
            countAttack = 0;
        }
    }

    @Override
    public void render(Graphics2D g) {
        // Mao vazia enquanto uma esta no ar, e no instante em que a proxima esta
        // sendo sacada. E o vazio que faz o arremesso parecer arremesso.
        if (emVoo != null || sacando > 0) {
            return;
        }
        // A ARTE APONTA PARA CIMA-ESQUERDA, como as espadas: medido no arquivo, a
        // ponta esta no canto de cima e a argola do cabo no canto de baixo. Ela
        // estava sendo tratada como deitada para a direita, e por isso saia virada
        // ao contrario na mao. Ancorada pelo
        // MIOLO do desenho: o +PI/4 e o pivo de canto eram da arte antiga, em pe.
        Rotate.apontar(getSprite(), kunaiPosition.x + spriteRotatePosition.x,
                kunaiPosition.y + spriteRotatePosition.y, this.angle, Rotate.DIAGONAL, g);
    }
}