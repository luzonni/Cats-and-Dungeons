package com.retronova.game.items;

import com.retronova.engine.sound.Sounds;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.Arrow;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A varinha. Dispara um raio curto no inimigo mais proximo que ela enxerga.
 *
 * ELA NAO FAZIA NADA. O {@code tick()} era um metodo vazio: a varinha aparecia na
 * loja, ocupava casa na hotbar e nunca atacou. Passou despercebido porque o gato
 * tem dano proprio e continua batendo — a arma so nao acrescentava coisa alguma.
 * Com a familia elemental entrando, seis varinhas inuteis seriam seis casas de
 * loja mentindo para o jogador, entao ela ganhou o ataque que sempre faltou.
 *
 * ELA ATIRA, e nao acerta de longe sem nada sair. A primeira versao aplicava o
 * dano direto no inimigo mais proximo e so desenhava um raio de seis quadros; sem
 * nada viajando, a leitura que sobrava era a de um golpe corpo a corpo dado a
 * distancia — "hit como se fosse espada". Agora sai uma bola de magia, que voa,
 * pode errar e bate ao encostar, que e o que separa varinha de espada.
 *
 * O projetil e a mesma Arrow, com outro sprite: ela ja e um corpo generico que
 * voa reto e aplica um efeito ao colidir. Reaproveitar evita uma segunda entidade
 * fazendo a mesma coisa com outro nome.
 */
public class Wand extends Item {

    private final Elemento elemento;

    @Override
    public Elemento elemento() {
        return this.elemento;
    }

    @Override
    public double cadencia() {
        return elemento.cadencia();
    }
    private final double dano;

    /** Ticks entre um disparo e o outro. */
    private static final int CADENCIA = 30;

    /** Por quantos ticks o clarao fica na ponta depois do disparo. */
    private static final int BRILHO = 5;

    /**
     * Velocidade da bola, em pixels de arte por tick.
     *
     * Metade da flecha. A flecha e comprida e fina, entao a 7 ela ainda le como um
     * risco; a bola tem cinco pixels de lado e, na mesma velocidade, andava quase
     * trinta pixels de tela por quadro — sumia entre um quadro e outro, e o que
     * sobrava era o inimigo perdendo vida sem nada visivel sair da arma.
     */
    private static final double VELOCIDADE_DA_BOLA = 3.5;

    private int contador;
    private int aceso;

    Wand(int id) {
        this(id, Elemento.NENHUM);
    }

    Wand(int id, Elemento elemento) {
        // O arquivo se chamava magicstick.png por heranca da arte antiga, e isso
        // deixava a varinha comum longe das outras na lista do editor e em
        // qualquer busca por "wand". Agora e wand.png, como as seis elementais.
        super(id, elemento.nome("Wand"), elemento.sprite("wand"));
        this.elemento = elemento;
        this.dano = elemento.dano(20);
        addSpecifications("Magic bolt", "player damage + " + (int) dano,
                elemento == Elemento.NENHUM ? "sorcery damage"
                        : elemento.rotulo().toLowerCase() + " damage");
    }

    /** A arte desta familia vem dos pacotes, desenhada na diagonal. */
    @Override
    protected double grausDaArte() {
        return 45;
    }

    @Override
    protected Porte porte() {
        return Porte.CAJADO;
    }

    @Override
    public void tick() {
        if (aceso > 0) {
            aceso--;
        }
        Player player = Game.getPlayer();
        // A pergunta e feita da PONTA DO CAJADO — mas so enquanto der para sair
        // dela. Com a ponta encostada num bloco a varinha ficava muda mesmo com o
        // inimigo a descoberto na frente; ai a origem recua para o meio do gato.
        java.awt.geom.Point2D.Double bico = pontaDaVarinha(player);
        java.awt.geom.Point2D.Double origem = bocaUsavel(player, bico.x, bico.y);
        Entity perto = alvoAlcancavel(player, player.getRange(), origem.x, origem.y);
        tickFaiscas(player);
        if (perto == null) {
            contador = 0;
            return;
        }
        double limiar = CADENCIA * cadencia();
        if (++contador < limiar) {
            // A ANTECIPACAO E O TEMPO QUE FALTA, contado pelo mesmo contador que
            // decide o tiro: o cajado sobe nos ultimos ticks antes de ele sair, e
            // nao num relogio paralelo que poderia escorregar.
            animacao().preparar((contador - (limiar - ANTECIPACAO)) / ANTECIPACAO);
            return;
        }
        contador = 0;
        this.aceso = BRILHO;
        atirar(player, perto, origem);
        animacao().disparou();
        soltarFaiscas(FAISCAS_NO_TIRO);
    }

    // ---------------------------------------------------------------- animacao

    /** Ticks antes do tiro em que o cajado se ergue: os ultimos 40% da cadencia base. */
    private static final double ANTECIPACAO = 12;

    /**
     * O gesto de lancar: ergue e recolhe na antecipacao, estende rapido no tiro e
     * volta devagar. Tres ticks de acao, porque e o tempo que o olho precisa para
     * ver o golpe e ainda e mais curto que o voo da bola ate sair da ponta.
     */
    private static final AnimacaoDaArma.Gesto GESTO =
            new AnimacaoDaArma.Gesto(3, 12, 16, 20, 1, 2, 2);

    @Override
    protected AnimacaoDaArma.Gesto gesto() {
        return GESTO;
    }

    /** A cor do efeito da varinha comum: o verde do brilho do orbe na arte oficial (icons/Gato.png). */
    private static final com.retronova.engine.graphics.Familia SEM_ELEMENTO =
            com.retronova.engine.graphics.Familia.VERDE;

    /**
     * Do pixel mais alto da arma ate o meio do orbe, em pixels de arte. O halo e
     * as faiscas nascem no MEIO da esfera; o tiro continua saindo da ponta.
     */
    private static final double ORBE_PX = 2.5;

    /** Raio do halo do orbe, em pixels de arte, e quanto ele cresce com a carga. */
    private static final double HALO_PX = 3.2, HALO_CARGA_PX = 2.2;
    /** Opacidade do halo em repouso e a mais na carga cheia (0-255). */
    private static final int HALO_ALFA = 55, HALO_ALFA_CARGA = 70;
    /** Pulso do halo: periodo em ticks e amplitude relativa. */
    private static final int PULSO_PERIODO = 48;
    private static final double PULSO = 0.12;

    // AS FAISCAS DO ORBE. Um punhado fixo, reaproveitado: nenhuma particula do
    // mapa e criada, nada e alocado por quadro, e elas ficam por cima do cajado
    // em vez de cair na ordenacao das entidades do mundo.
    private static final int FAISCAS = 10;
    /** Ticks entre duas faiscas, pela intensidade (comum, elemental, lendaria). */
    private static final int[] FAISCA_A_CADA = {16, 10, 6};
    /** Quantas saem de uma vez no tiro. */
    private static final int FAISCAS_NO_TIRO = 3;
    /** Vida de uma faisca, em ticks. */
    private static final int FAISCA_VIDA = 36;

    private final double[] fx = new double[FAISCAS], fy = new double[FAISCAS];
    private final double[] fxAntes = new double[FAISCAS], fyAntes = new double[FAISCAS];
    private final double[] fvx = new double[FAISCAS], fvy = new double[FAISCAS];
    private final int[] fvida = new int[FAISCAS];
    /**
     * Sorteio SO DO ENFEITE, separado do Engine.RAND: faisca nao pode consumir
     * numeros do sorteio que decide horda e cartas.
     */
    private static final java.util.Random VISUAL = new java.util.Random();
    private int proximaFaisca;
    /** Onde o orbe estava no ultimo desenho: e dali que as faiscas saem. */
    private double orboX = Double.NaN, orboY;

    /**
     * Como a faisca anda, por elemento: o fogo sobe rapido, o gelo e a agua caem
     * devagar (floco e gota), a terra cai pesada, o ar sobe girando. Em pixels de
     * arte por tick, vertical e o quanto ela serpenteia para os lados.
     */
    private double subidaDaFaisca() {
        return switch (elemento) {
            case FOGO -> -0.45;
            case GELO -> 0.12;
            case AGUA -> 0.28;
            case TERRA -> 0.34;
            case AR -> -0.22;
            default -> -0.25;
        };
    }

    private double serpenteio() {
        return elemento == Elemento.AR ? 0.35 : 0.12;
    }

    private void tickFaiscas(Player player) {
        int px = Configs.GameScale();
        for (int i = 0; i < FAISCAS; i++) {
            fxAntes[i] = fx[i];
            fyAntes[i] = fy[i];
            if (fvida[i] <= 0) {
                continue;
            }
            fvida[i]--;
            fx[i] += fvx[i] * px + Math.sin((animacao().tempo() + i * 7) / 6d) * serpenteio() * px;
            fy[i] += fvy[i] * px;
        }
        if (animacao().tempo() % FAISCA_A_CADA[intensidade()] == 0) {
            soltarFaiscas(1);
        }
    }

    private void soltarFaiscas(int quantas) {
        if (Double.isNaN(orboX)) {
            return;
        }
        int px = Configs.GameScale();
        for (int k = 0; k < quantas; k++) {
            int i = proximaFaisca;
            proximaFaisca = (proximaFaisca + 1) % FAISCAS;
            double ang = VISUAL.nextDouble() * Math.PI * 2;
            double r = VISUAL.nextDouble() * 2 * px;
            fx[i] = fxAntes[i] = orboX + Math.cos(ang) * r;
            fy[i] = fyAntes[i] = orboY + Math.sin(ang) * r;
            fvx[i] = (VISUAL.nextDouble() - 0.5) * 0.2;
            fvy[i] = subidaDaFaisca() * (0.7 + VISUAL.nextDouble() * 0.6);
            fvida[i] = FAISCA_VIDA - VISUAL.nextInt(10);
        }
    }

    /**
     * De onde a bola sai: O TOPO DO CAJADO.
     *
     * E o que se espera de uma varinha — o feitico nasce na ponta e vai reto ao
     * alvo — e e o mesmo padrao que as engines chamam de muzzle: um ponto preso a
     * arma, que gira com ela, e nao um deslocamento fixo em relacao ao gato.
     *
     * O topo e achado como o pixel opaco MAIS LONGE DO PUNHO. Serve para qualquer
     * cajado sem ninguem marcar nada, e nao depende de o desenho apontar para um
     * lado especifico do arquivo — que foi onde a versao anterior errou: ela
     * projetava num eixo ja girado, a conta se cancelava, e o resultado era sempre
     * o pixel mais a direita do arquivo, ou seja, o PE do cajado.
     *
     * A transformada e a mesma de naMao, e tem de ser: o sprite e girado em volta
     * do punho e o punho vai parar na mao. Entao a ponta no mundo e a mao, mais o
     * deslocamento da pose, mais a diferenca ponta-punho girada.
     */
    private java.awt.geom.Point2D.Double pontaDaVarinha(Player player) {
        return pontaDaVarinha(player, 0, 0, 0, 0);
    }

    /**
     * A ponta com o movimento da mao somado — so para DESENHAR.
     *
     * O tiro continua nascendo da ponta parada, acima: a animacao nao pode mudar
     * de onde a bola sai nem a linha de tiro que libera o disparo. O brilho e as
     * faiscas, que sao enfeite, seguem o orbe onde ele esta na tela.
     */
    private java.awt.geom.Point2D.Double pontaDaVarinha(Player player, double grausA,
                                                        double avancoA, double desceA, double recuoPx) {
        int px = Configs.GameScale();
        int lado = player.getLadoDoPasso();
        Poses.Pose p = pose();
        boolean virar = (lado == -1) ^ (p != null && p.espelhar());
        BufferedImage desenho = virar
                ? SpriteHandler.flip(getSprite(), 1, -1) : getSprite();
        int colunas = Math.max(1, desenho.getWidth() / px);

        Point punho;
        double graus = 0;
        int dx = 0, dy = 0;
        if (p != null) {
            int coluna = virar ? colunas - 1 - p.punhoX() : p.punhoX();
            punho = new Point(coluna * px + px / 2, p.punhoY() * px + px / 2);
            graus = p.graus();
            dx = p.dx();
            dy = p.dy();
        } else {
            punho = empunhadura(desenho, virar ? -1 : 1);
        }

        Point ponta;
        if (p != null && p.bocaX() >= 0) {
            int coluna = virar ? colunas - 1 - p.bocaX() : p.bocaX();
            ponta = new Point(coluna * px + px / 2, p.bocaY() * px + px / 2);
        } else {
            ponta = maisLongeDe(desenho, punho);
        }

        double giro = Math.toRadians((graus + grausA) * lado);
        double ox = ponta.x - punho.x, oy = ponta.y - punho.y;
        double comprimento = Math.hypot(ox, oy);
        if (recuoPx > 0 && comprimento > 0) {
            double k = Math.max(0, comprimento - recuoPx * px) / comprimento;
            ox *= k;
            oy *= k;
        }
        double c = Math.cos(giro), s = Math.sin(giro);
        Point mao = player.getMao();
        return new java.awt.geom.Point2D.Double(
                mao.x + Math.round((dx + avancoA) * px * lado) + ox * c - oy * s,
                mao.y + dy * px + Math.round(desceA * px) + ox * s + oy * c);
    }

    private void atirar(Player player, Entity alvo, java.awt.geom.Point2D.Double origem) {
        // Nasce E MIRA do mesmo ponto de onde a linha foi medida. Misturar os dois
        // — nascer na ponta e mirar do corpo — traca uma reta paralela a mira,
        // afastada por meio corpo, e de longe a bola passa ao lado do alvo.
        double x = origem.x;
        double y = origem.y;
        double angulo = miraDe(x, y, alvo);
        double total = dano + player.getDamage();
        Arrow bola = new Arrow(x, y, angulo, elemento.sprite("bolt"), VELOCIDADE_DA_BOLA,
                player, alvoAtingido -> {
            alvoAtingido.strike(elemento.ataque(AttackTypes.Sorcery), total);
            tocarGolpe(Sounds.Laser);
            alvoAtingido.getPhysical().addForce("knockback", 2, alvoAtingido.getAngle(player));
            // Explosao Arcana do Azrael: so faz algo se o gato tiver a habilidade.
            com.retronova.game.objects.entities.Habilidades.explosaoArcana(player, alvoAtingido, total,
                    elemento.ataque(AttackTypes.Sorcery), elemento == Elemento.NENHUM
                            ? com.retronova.game.items.Passiva.EXPLOSAO_ARCANA.cor() : elemento.cor());
        });
        Game.getMap().put(bola);
        this.ultimoAngulo = angulo;
    }

    private double ultimoAngulo;

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        if (player == null) {
            naMao(g, getSprite());
            return;
        }
        float alpha = com.retronova.engine.Engine.alpha();
        AnimacaoDaArma anim = animacao();
        anim.calcular(player, alpha);
        java.awt.geom.Point2D.Double ponta = pontaDaVarinha(player, anim.graus, anim.avanco, anim.desce, ORBE_PX);
        orboX = ponta.x;
        orboY = ponta.y;
        com.retronova.engine.graphics.Familia fam = familiaDoEfeito(SEM_ELEMENTO);
        int px = Configs.GameScale();

        // O HALO VEM ANTES DO CAJADO, para ficar atras do orbe: e luz em volta
        // da esfera, como na arte oficial, e nao uma mancha por cima dela.
        double pulso = 1 + PULSO * Math.sin(2 * Math.PI * (anim.tempo() + alpha) / PULSO_PERIODO);
        double carga = Math.max(anim.carga, anim.clarao);
        int forca = intensidade();
        double raio = (HALO_PX + HALO_CARGA_PX * carga + forca * 0.6) * px * pulso;
        int alfa = (int) (HALO_ALFA + forca * 15 + HALO_ALFA_CARGA * carga);
        com.retronova.engine.graphics.Brilho.disco(g, ponta.x, ponta.y, raio,
                fam.tom(com.retronova.engine.graphics.Familia.BASE, alfa));
        com.retronova.engine.graphics.Brilho.disco(g, ponta.x, ponta.y, raio * 0.5,
                fam.tom(com.retronova.engine.graphics.Familia.LUZ, (int) (alfa * 0.8)));

        naMao(g, getSprite());

        renderFaiscas(g, fam, alpha);
        if (aceso <= 0 && anim.clarao <= 0) {
            return;
        }
        // Um CLARAO curto na ponta, e nao um raio ate o alvo: quem viaja agora e
        // a bola. Raio ate o inimigo somado a bola voando seria a mesma coisa
        // contada duas vezes, e o olho leria o raio primeiro.
        double f = anim.clarao;
        double r = px * (1.5 + f * 2.5 + forca * 0.5);
        com.retronova.engine.graphics.Brilho.disco(g, ponta.x, ponta.y, r,
                fam.tom(com.retronova.engine.graphics.Familia.LUZ, (int) (170 * f)));
        com.retronova.engine.graphics.Brilho.disco(g, ponta.x, ponta.y, r * 0.5,
                fam.tom(com.retronova.engine.graphics.Familia.BRILHO, (int) (230 * f)));
    }

    /** As faiscas: quadrados de um pixel de arte, apagando no fim da vida. */
    private void renderFaiscas(Graphics2D g, com.retronova.engine.graphics.Familia fam, float alpha) {
        int px = Configs.GameScale();
        for (int i = 0; i < FAISCAS; i++) {
            if (fvida[i] <= 0) {
                continue;
            }
            double x = fxAntes[i] + (fx[i] - fxAntes[i]) * alpha;
            double y = fyAntes[i] + (fy[i] - fyAntes[i]) * alpha;
            double vida = fvida[i] / (double) FAISCA_VIDA;
            // Nascem no tom mais claro e esfriam para a base antes de sumir.
            int degrau = vida > 0.5 ? com.retronova.engine.graphics.Familia.BRILHO
                    : com.retronova.engine.graphics.Familia.LUZ;
            g.setColor(fam.tom(degrau, (int) (255 * Math.min(1, vida * 1.6))));
            g.fillRect((int) Math.round(x - px / 2d), (int) Math.round(y - px / 2d), px, px);
        }
    }
}