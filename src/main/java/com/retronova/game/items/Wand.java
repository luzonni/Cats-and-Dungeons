package com.retronova.game.items;

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
        if (perto == null) {
            contador = 0;
            return;
        }
        if (++contador < CADENCIA * cadencia()) {
            return;
        }
        contador = 0;
        this.aceso = BRILHO;
        atirar(player, perto, origem);
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

        double giro = Math.toRadians(graus * lado);
        double ox = ponta.x - punho.x, oy = ponta.y - punho.y;
        double c = Math.cos(giro), s = Math.sin(giro);
        Point mao = player.getMao();
        return new java.awt.geom.Point2D.Double(
                mao.x + dx * px * lado + ox * c - oy * s,
                mao.y + dy * px + ox * s + oy * c);
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
            alvoAtingido.getPhysical().addForce("knockback", 2, alvoAtingido.getAngle(player));
        });
        Game.getMap().put(bola);
        this.ultimoAngulo = angulo;
    }

    private double ultimoAngulo;

    @Override
    public void render(Graphics2D g) {
        naMao(g, getSprite());
        if (aceso <= 0) {
            return;
        }
        Player player = Game.getPlayer();
        // Um CLARAO curto na ponta, e nao um raio ate o alvo: quem viaja agora e
        // a bola. Raio ate o inimigo somado a bola voando seria a mesma coisa
        // contada duas vezes, e o olho leria o raio primeiro.
        java.awt.geom.Point2D.Double ponta = pontaDaVarinha(player);
        double x = ponta.x;
        double y = ponta.y;
        float f = aceso / (float) BRILHO;
        int raio = (int) (Configs.GameScale() * (1.5f + f * 2f));
        Color c = elemento.cor();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (150 * f)));
        g2.fillOval((int) x - raio, (int) y - raio, raio * 2, raio * 2);
        g2.setColor(new Color(255, 255, 255, (int) (200 * f)));
        g2.fillOval((int) x - raio / 2, (int) y - raio / 2, raio, raio);
        g2.dispose();
    }
}