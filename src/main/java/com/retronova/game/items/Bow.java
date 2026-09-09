package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.Arrow;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Bow extends Item {

    private double angle = 0;
    private int count;
    private int countShot;
    private BufferedImage arrowSprite;

    /** A que distância da mão o arco fica quando não há pose, em pixels de arte. */
    private static final double ALCANCE_DA_MAO = 7;

    /**
     * Onde o arco está, a partir do meio do gato — agora vindo da pose.
     *
     * Era um sete fixo. Só que arco não é segurado, é apontado: os campos de
     * posição da pose não faziam nada nele, e quem tentasse ajustá-lo no editor
     * mexia sem efeito nenhum. Aqui o dx da pose vira essa distância.
     */
    private double distanciaDoArco() {
        Poses.Pose p = pose();
        return Configs.GameScale() * (p == null ? ALCANCE_DA_MAO : p.dx());
    }

    /** Desvio para o lado, perpendicular à mira. Vem do dy da pose. */
    private double desvioDoArco() {
        Poses.Pose p = pose();
        return p == null ? 0 : Configs.GameScale() * p.dy();
    }

    /** O alvo escolhido neste tick — o tiro mira dele, a partir da corda. */
    private Entity alvoDoTiro;

    /** Giro a mais somado à direção do tiro. Vem do graus da pose. */
    private double giroExtra() {
        Poses.Pose p = pose();
        return p == null ? 0 : Math.toRadians(p.graus());
    }

    /** O ponto onde o arco — e a flecha encaixada nele — são ancorados. */
    private java.awt.geom.Point2D.Double ancora(Player player, double recuo) {
        return ancoraDeMira(player, angle, recuo);
    }

    private final Elemento elemento;

    @Override
    public Elemento elemento() {
        return this.elemento;
    }

    @Override
    public double cadencia() {
        return elemento.cadencia();
    }

    Bow(int id) {
        this(id, Elemento.NENHUM);
    }

    Bow(int id, Elemento elemento) {
        super(id, elemento.nome("Bow"), elemento.sprite("bow"));
        this.elemento = elemento;
        addSpecifications("Arrow adds poison", "player damage",
                elemento == Elemento.NENHUM ? "shot slowed"
                        : elemento.rotulo().toLowerCase() + " damage");
    }

    /** A arte desta familia vem dos pacotes, desenhada na diagonal. */
    @Override
    protected double grausDaArte() {
        return 45;
    }

    @Override
    public void tick() {
        this.arrowSprite = SpriteSheet.getSprite(
                "sprites/objects/utility." + elemento.sprite("arrow")).getImage(0);
        Player player = Game.getPlayer();
        // O mais próximo QUE DÁ PARA ACERTAR, medido DA CORDA — que é de onde a
        // flecha parte. Medir do meio do gato aprovava tiro que nascia do outro
        // lado do bloco quando ele estava encostado numa quina.
        java.awt.geom.Point2D.Double corda = ancora(player, 0);
        java.awt.geom.Point2D.Double daCorda = bocaUsavel(player, corda.x, corda.y);
        Entity nearest = alvoAlcancavel(player, player.getRange(), daCorda.x, daCorda.y);
        if(nearest != null){
            angle = nearest.getAngle(player);
            this.alvoDoTiro = nearest;
            count++;
            // O elemento estica ou encurta o intervalo entre os quadros de
            // esticar a corda, entao o arco de terra e visivelmente mais pesado
            // de armar que o de ar — e nao so um numero diferente na ficha.
            if(count > (player.getAttackSpeed()*3.25d)/5 * cadencia()) {
                count = 0;
                countShot++;
                this.plusIndexSprite();
            }
            if(countShot >= 5) {
                countShot = 0;
                shot(player);
            }
        }else {
            this.alvoDoTiro = null;
            resetIndexSprite();
        }
    }

    /**
     * A flecha nasce NO ARCO, e não no canto da caixa do gato.
     *
     * Ela saía de {@code getX(), getY()}, que é o canto superior esquerdo da
     * hitbox — por isso aparecia acima e atrás do bicho em vez de sair da corda.
     * O deslocamento é o mesmo que o desenho do arco usa, então as duas coisas
     * não têm como divergir.
     */
    private void shot(Player shooter) {
        // Nasce na BOCA da arma, e nao no meio dela: no arco a corda fica atras do
        // miolo, e sem isso a flecha saia do centro da madeira.
        java.awt.geom.Point2D.Double a = ancora(shooter, 0);
        java.awt.geom.Point2D.Double b =
                boca(getSprite(), a.x, a.y, angle + giroExtra(), Rotate.DIAGONAL);
        // A FLECHA MIRA DA CORDA, e nao do centro do gato. A corda fica a meio
        // corpo de distancia; voar no angulo medido do bicho traca uma reta
        // paralela a mira, afastada por essa distancia, e de longe a flecha passa
        // ao lado do alvo. O angulo da arma continua o do corpo — ela so aponta.
        double tiro = alvoDoTiro == null ? angle : miraDe(b.x, b.y, alvoDoTiro);
        Arrow arrow = new Arrow(b.x, b.y, tiro, elemento.sprite("arrow"), 7d, shooter,
                (entity) -> {
            entity.strike(elemento.ataque(AttackTypes.Piercing),
                    elemento.dano(shooter.getDamage()));
            entity.getPhysical().addForce("knockback", 4, tiro);
        });
        Game.getMap().put(arrow);
        Sound.play(Sounds.Bow);
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        // ADIANTADO na direcao da mira, e nao no centro do gato: desenhado no
        // meio dele, o arco ficava por dentro do bicho em vez de na mao.
        java.awt.geom.Point2D.Double a = ancora(player, 0);
        double xx = a.x;
        double yy = a.y;
        // O arco JA APONTA PARA A DIREITA na arte: a corda e a linha reta da
        // esquerda e as pontas curvam para o lado por onde a flecha sai. Somar
        // PI/4 aqui, como estava, deixava o arco inteiro 45 graus torto.
        Rotate.apontar(getSprite(), xx, yy, angle + giroExtra(), Rotate.DIAGONAL, g);
        renderArrow(player, g);
    }

    /**
     * A flecha encaixada — ela SAI DO MEIO DO ARCO.
     *
     * A flecha é ancorada no MESMO ponto que o arco, então as duas peças não têm
     * como se separar: o miolo da flecha cai no miolo do arco e, como a flecha
     * tem 9 pixels de comprimento contra os 7 do arco, a ponta escapa na frente
     * e a empena fica atrás, em cima da corda. Era isto que faltava — antes cada
     * uma era ancorada num canto diferente da caixa do gato e cada uma girava em
     * volta de um ponto diferente, e o resultado era a flecha brotando na quina
     * do arco em vez de no meio.
     *
     * O recuo é o gesto de esticar: ela vem para trás enquanto o tiro carrega e
     * volta ao arco na hora de soltar.
     */
    private void renderArrow(Player player, Graphics2D g) {
        if(countShot < 1 || this.arrowSprite == null) {
            return;
        }
        double recuo = (5 - countShot) * Configs.GameScale();
        java.awt.geom.Point2D.Double a = ancora(player, recuo);
        double x = a.x;
        double y = a.y;
        // A flecha e desenhada EM PE, com a ponta para cima: o facing e PARA_CIMA.
        Rotate.apontar(arrowSprite, x, y, angle, Rotate.PARA_CIMA, g);
    }

}
