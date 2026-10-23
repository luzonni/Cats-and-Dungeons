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

    /**
     * Dano da propria arma, somado ao do gato.
     *
     * ERA ZERO, e isso era descuido e nao desenho. A espada soma 35 e a varinha 20;
     * a flecha saia com o dano do gato e mais nada, entao o Finn batia por NOVE
     * enquanto o Muffin batia por quarenta e cinco — cinco vezes menos, antes de
     * qualquer resistencia entrar na conta.
     *
     * Doze e menos que a varinha de proposito: o arco dispara a cada doze quadros
     * contra os trinta dela, entao em dano por segundo ele passa na frente mesmo
     * somando menos por flecha. E acerta de longe, que ja e uma vantagem.
     */
    private static final double DANO = 12;

    /** Angulo entre duas flechas vizinhas do voleio: 4 flechas cobrem uns 27 graus. */
    private static final double ABERTURA_DO_VOLEIO = Math.toRadians(9);

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
        cargaAntes = carga;
        anguloVistoAntes = anguloVisto;
        if (soltura > 0) {
            soltura--;
        }
        boolean atirou = false;
        if(nearest != null){
            angle = nearest.getAngle(player);
            this.alvoDoTiro = nearest;
            count++;
            // O elemento estica ou encurta o intervalo entre os quadros de
            // esticar a corda, entao o arco de terra e visivelmente mais pesado
            // de armar que o de ar — e nao so um numero diferente na ficha.
            double limiar = (player.getAttackSpeed()*3.25d)/5 * cadencia();
            if(count > limiar) {
                count = 0;
                countShot++;
                this.plusIndexSprite();
            }
            if(countShot >= 5) {
                countShot = 0;
                shot(player);
                atirou = true;
            }
            // A CARGA E A MESMA CONTA DO DISPARO, continua: cinco degraus de
            // countShot e a fracao do count dentro de cada um. Quando ela chega a
            // um, a flecha sai — a corda puxada no desenho e o tempo real do tiro.
            carga = Math.min(1, (countShot + Math.min(1, count / Math.max(1e-6, limiar))) / 5d);
        }else {
            this.alvoDoTiro = null;
            resetIndexSprite();
            carga = 0;
        }
        if (atirou) {
            // SOLTOU: a corda parte de onde estava puxada e o arco recua. O angulo
            // visto encaixa no do tiro, para a flecha sair de dentro do arco.
            cordaNaSoltura = Math.max(cargaAntes, 0.8);
            soltura = SOLTURA_TICKS;
            carga = cargaAntes = 0;
            anguloVisto = anguloVistoAntes = angle;
        } else {
            anguloVisto += diferenca(anguloVisto, angle) * GIRO_SUAVE;
        }
    }

    // ---------------------------------------------------------------- animacao

    /**
     * Quanto o angulo DESENHADO alcanca o da mira por tick.
     *
     * A mira troca de alvo de um tick para o outro, e o arco girava junto, de uma
     * vez: com o bicho mais perto passando de um lado para o outro do gato, ele
     * pulava meia volta. O tiro continua usando a mira exata; so o desenho a
     * persegue, e chega em uns seis ticks.
     */
    private static final double GIRO_SUAVE = 0.35;
    /** Quanto a corda vem para tras com a carga cheia, em pixels de arte. */
    private static final double PUXADA_PX = 4;
    /** A partir de que carga a corda treme de tensao. */
    private static final double TENSAO = 0.85;
    /** Ticks da soltura: o recuo do arco e a corda voltando e vibrando. */
    private static final int SOLTURA_TICKS = 10;
    /** Recuo do arco no tiro, em pixels de arte, e o giro do coice, em graus. */
    private static final double RECUO_PX = 2, COICE_GRAUS = 7;

    private double carga, cargaAntes;
    private double anguloVisto, anguloVistoAntes;
    private int soltura;
    private double cordaNaSoltura;

    /** O menor giro de um angulo ate outro, de -PI a PI. */
    private static double diferenca(double de, double para) {
        double d = para - de;
        return Math.atan2(Math.sin(d), Math.cos(d));
    }

    /**
     * A corda depois do tiro: parte puxada, passa do ponto de repouso para a
     * frente e assenta, como corda de verdade. Em fracao da puxada, de 1 a 0.
     */
    static double cordaVoltando(double k) {
        if (k >= 1) {
            return 0;
        }
        return Math.exp(-4 * k) * Math.cos(3 * Math.PI * k) * (1 - k);
    }

    /** O coice do arco no tiro, de 1 no tick do tiro a 0 no fim da soltura. */
    static double coice(double k) {
        if (k >= 1 || k < 0) {
            return 0;
        }
        return (1 - k) * (1 - k);
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
        // VOLEIO: n flechas, com o dano dividido por raiz de n (ver
        // Bonus.danoPorFlecha). Sem a passiva, n = 1 e o tiro e o de sempre.
        //
        // AS FLECHAS A MAIS PROCURAM OUTROS BICHOS. A primeira vai no alvo de
        // sempre; cada uma das outras vai no proximo inimigo mais perto que ainda
        // nao tem flecha. So quando falta inimigo a flecha sobra para o leque em
        // volta da mira principal — e ai e que o voleio concentra.
        int n = com.retronova.game.objects.entities.Bonus.flechas(shooter);
        double fracao = com.retronova.game.objects.entities.Bonus.danoPorFlecha(n);
        java.util.List<com.retronova.game.objects.entities.enemies.Enemy> outros = n > 1
                ? alvosAlcancaveis(shooter, shooter.getRange(), b.x, b.y, n - 1, alvoDoTiro)
                : java.util.List.of();
        int noLeque = 0;
        for (int i = 0; i < n; i++) {
            double direcao;
            if (i == 0) {
                direcao = tiro;
            } else if (i - 1 < outros.size()) {
                direcao = miraDe(b.x, b.y, outros.get(i - 1));
            } else {
                // Alterna os lados do leque: +9, -9, +18...
                noLeque++;
                int passo = (noLeque + 1) / 2;
                direcao = tiro + (noLeque % 2 == 1 ? passo : -passo) * ABERTURA_DO_VOLEIO;
            }
            final double dirFinal = direcao;
            Arrow arrow = new Arrow(b.x, b.y, dirFinal, elemento.sprite("arrow"), 7d, shooter,
                    (entity) -> {
                entity.strike(elemento.ataque(AttackTypes.Piercing),
                        elemento.dano(DANO + shooter.getDamage()) * fracao);
                entity.getPhysical().addForce("knockback", 4 * fracao, dirFinal);
            });
            Game.getMap().put(arrow);
        }
        tocarGolpe(Sounds.Bow);
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        float alpha = com.retronova.engine.Engine.alpha();
        double mira = anguloVistoAntes + diferenca(anguloVistoAntes, anguloVisto) * alpha;
        double k = soltura > 0 ? (SOLTURA_TICKS - soltura + alpha) / SOLTURA_TICKS : 1;
        double coice = coice(k);
        int px = Configs.GameScale();

        // ADIANTADO na direcao da mira, e nao no centro do gato: desenhado no
        // meio dele, o arco ficava por dentro do bicho em vez de na mao. No tiro
        // ele recua um pouco e da um coice para cima: o arco reage a soltura.
        java.awt.geom.Point2D.Double a = ancoraDeMira(player, mira, RECUO_PX * px * coice);
        AnimacaoDaArma anim = animacao();
        anim.calcular(player, alpha);
        double xx = a.x;
        double yy = a.y + Math.round(anim.desce * px);
        int lado = Math.cos(mira) < 0 ? -1 : 1;
        // O arco JA APONTA PARA A DIREITA na arte: a corda e a linha reta da
        // esquerda e as pontas curvam para o lado por onde a flecha sai. Somar
        // PI/4 aqui, como estava, deixava o arco inteiro 45 graus torto.
        double giro = mira + giroExtra() - Math.toRadians(COICE_GRAUS * coice) * lado;

        double c = cargaAntes + (carga - cargaAntes) * alpha;
        CordaDoArco corda = CordaDoArco.de(getSprite(), px);
        if (corda == null) {
            Rotate.apontar(getSprite(), xx, yy, giro, Rotate.DIAGONAL, g);
            renderArrow(player, g, mira, c, null, 0, 0);
            return;
        }

        // A PUXADA: suave no comeco e pesada no fim (ease-in), que e como se
        // estica um arco. Na carga cheia a corda treme um pixel de tela.
        double puxada = PUXADA_PX * px * c * c;
        double tremor = c >= TENSAO && (anim.tempo() / 2) % 2 == 0 ? px / 3d : 0;
        if (soltura > 0) {
            puxada = PUXADA_PX * px * cordaNaSoltura * cordaNaSoltura * cordaVoltando(k);
        }
        java.awt.geom.AffineTransform at = transformada(getSprite(), xx, yy, giro);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(corda.semCorda, at, null);

        pontoA.setLocation(corda.ax, corda.ay);
        at.transform(pontoA, pontoA);
        pontoB.setLocation(corda.bx, corda.by);
        at.transform(pontoB, pontoB);
        double dx = Math.cos(mira), dy = Math.sin(mira);
        double ex = (pontoA.x + pontoB.x) / 2 - dx * puxada - dy * tremor;
        double ey = (pontoA.y + pontoB.y) / 2 - dy * puxada + dx * tremor;

        // A CORDA, dois segmentos das pontas ao encaixe. Sem suavizacao: e uma
        // linha de um pixel de arte, como a que estava pintada no PNG.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setStroke(traco(px));
        g2.setColor(corda.cor);
        g2.drawLine((int) Math.round(pontoA.x), (int) Math.round(pontoA.y), (int) Math.round(ex), (int) Math.round(ey));
        g2.drawLine((int) Math.round(ex), (int) Math.round(ey), (int) Math.round(pontoB.x), (int) Math.round(pontoB.y));
        g2.dispose();
        renderArrow(player, g, mira, c, corda, ex, ey);
    }

    private final java.awt.geom.Point2D.Double pontoA = new java.awt.geom.Point2D.Double();
    private final java.awt.geom.Point2D.Double pontoB = new java.awt.geom.Point2D.Double();
    private final java.awt.geom.AffineTransform transformada = new java.awt.geom.AffineTransform();
    private static BasicStroke traco;

    /** O traco da corda, de um pixel de arte. Refeito so quando a escala muda. */
    private static BasicStroke traco(int px) {
        if (traco == null || traco.getLineWidth() != px) {
            traco = new BasicStroke(px, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
        }
        return traco;
    }

    /**
     * A MESMA transformada do Rotate.apontar, montada a mao: o arco sem corda
     * tem de cair exatamente onde o arco inteiro cairia, e as pontas da corda
     * passam por ela para irem junto. O espelho que o apontar faz trocando de
     * imagem entra aqui como uma escala -1, para valer tambem para os pontos.
     */
    private java.awt.geom.AffineTransform transformada(BufferedImage sprite, double x, double y, double mira) {
        boolean espelha = Math.cos(mira) < 0;
        double facing = espelha ? -Rotate.DIAGONAL : Rotate.DIAGONAL;
        Point miolo = Rotate.centroDoDesenho(espelha ? Rotate.espelhadoNaVertical(sprite) : sprite);
        java.awt.geom.AffineTransform at = transformada;
        at.setToIdentity();
        at.translate((int) (x - miolo.x), (int) (y - miolo.y));
        at.rotate(mira + facing, miolo.x, miolo.y);
        if (espelha) {
            at.translate(0, sprite.getHeight());
            at.scale(1, -1);
        }
        return at;
    }

    /** O arco não usa a pose de porte: ele aponta para a mira, sem a flecha. */
    @Override
    public void renderNaMao(Graphics2D g, java.awt.image.BufferedImage sprite) {
        java.awt.geom.Point2D.Double a = ancora(Game.getPlayer(), 0);
        Rotate.apontar(sprite, a.x, a.y, angle + giroExtra(), Rotate.DIAGONAL, g);
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
    private void renderArrow(Player player, Graphics2D g, double mira, double carga,
                             CordaDoArco corda, double encaixeX, double encaixeY) {
        // Sem alvo, sem flecha. Logo depois do tiro tambem nao: a que saiu esta
        // voando, e a proxima so e encaixada na metade da soltura.
        if (carga <= 0 || this.arrowSprite == null || soltura > SOLTURA_TICKS / 2) {
            return;
        }
        int px = Configs.GameScale();
        double dx = Math.cos(mira), dy = Math.sin(mira);
        double x, y;
        if (corda != null) {
            // A CAUDA NO ENCAIXE DA CORDA: a flecha anda para tras junto com a
            // corda puxada, e nao por conta propria.
            double metade = comprimentoDaFlecha() / 2d;
            x = encaixeX + dx * metade;
            y = encaixeY + dy * metade;
        } else {
            // A conta antiga ia ao contrario: o recuo era (5 - countShot), entao a
            // flecha ANDAVA PARA A FRENTE enquanto o tiro carregava. Agora ela
            // vem para tras com a carga, como a corda.
            java.awt.geom.Point2D.Double a = ancoraDeMira(player, mira, PUXADA_PX * px * carga * carga);
            x = a.x;
            y = a.y;
        }
        // A flecha e desenhada EM PE, com a ponta para cima: o facing e PARA_CIMA.
        Rotate.apontar(arrowSprite, x, y, mira, Rotate.PARA_CIMA, g);

        // ARCO EVOLUIDO ACENDE A PONTA enquanto carrega: o elemental e o lendario
        // mostram a carga crescendo na cor deles. O comum fica so com a puxada.
        int forca = intensidade();
        if (forca > 0) {
            double ponta = comprimentoDaFlecha() / 2d;
            com.retronova.engine.graphics.Brilho.disco(g, x + dx * ponta, y + dy * ponta,
                    px * (1.2 + forca) * carga,
                    familiaDoEfeito(com.retronova.engine.graphics.Familia.ACO)
                            .tom(com.retronova.engine.graphics.Familia.LUZ, (int) (90 * forca * carga)));
        }
    }

    private BufferedImage flechaMedida;
    private int comprimentoMedido;

    /** O comprimento da flecha desenhada, em pixels de tela: a altura dos pixels opacos. */
    private int comprimentoDaFlecha() {
        if (arrowSprite != flechaMedida) {
            int topo = -1, base = -1;
            for (int y = 0; y < arrowSprite.getHeight(); y++) {
                for (int x = 0; x < arrowSprite.getWidth(); x++) {
                    if ((arrowSprite.getRGB(x, y) >>> 24) > 16) {
                        if (topo < 0) {
                            topo = y;
                        }
                        base = y;
                        break;
                    }
                }
            }
            comprimentoMedido = topo < 0 ? arrowSprite.getHeight() : base - topo + 1;
            flechaMedida = arrowSprite;
        }
        return comprimentoMedido;
    }

}
