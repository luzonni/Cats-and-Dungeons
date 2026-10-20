package com.retronova.game.items;

import com.retronova.game.objects.Investida;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Sword extends Item {

    /**
     * De que lado vem o golpe: +1 de fora para dentro, -1 de volta.
     *
     * Alterna a cada investida, entao os golpes seguidos fazem direita-esquerda em
     * vez de repetir o mesmo movimento. E o que a referencia chama de forehand e
     * backhand: repetir o mesmo lado le como a mesma animacao tocando de novo.
     */
    private int side;

    /**
     * A INVESTIDA, no lugar da varredura antiga.
     *
     * O golpe anterior era uma volta de 180 graus em velocidade CONSTANTE, sem
     * comeco nem fim: o rad andava de PI/12 em PI/12 e recomecava. Isso quebra as
     * tres coisas que toda referencia de animacao de golpe repete — antecipacao,
     * impacto rapido e sustentacao —, e o resultado e uma arma que parece girar
     * sozinha em vez de golpear.
     *
     * A Investida ja tinha essa estrutura e ja era usada pelo machado: recua
     * devagar (antecipacao), cruza a distancia inteira em tres ticks (o impacto),
     * segue adiante quase parada (a sustentacao, que e o que da peso) e volta
     * (recuperacao). Aqui ela entra na versao LEVE, 400 ms, que e o tempo de
     * espada de uma mao — o machado usa a pesada, de 800.
     */
    // NAO e inicializador de campo: os inicializadores rodam ANTES do corpo do
    // construtor, e la o elemento ainda e nulo. Montada aqui, depois que ele
    // existe, e o unico jeito de a cadencia dele valer.
    private final Investida investida;

    @Override
    public double cadencia() {
        return elemento.cadencia();
    }

    private final double damage;


    private final Elemento elemento;

    @Override
    public Elemento elemento() {
        return this.elemento;
    }

    Sword(int id) {
        this(id, Elemento.NENHUM);
    }

    /**
     * A variante elemental. Mesma espada, mesmo golpe: so o pigmento e o tipo de
     * dano mudam — e e de proposito que o gesto seja identico, para o jogador ler
     * "a mesma espada, de fogo" em vez de aprender uma arma nova.
     */
    Sword(int id, Elemento elemento) {
        super(id, elemento.nome("Sword"), elemento.sprite("sword"));
        this.elemento = elemento;
        this.investida = Investida.espada().vezes(cadencia());
        setIndexSprite(Engine.RAND.nextInt(25));
        this.damage = elemento.dano(35);
        this.side = 1;
        addSpecifications("Melee Attack", "Player damage + "+ this.damage,
                elemento == Elemento.NENHUM ? "very fast" : elemento.rotulo().toLowerCase() + " damage");
    }

    /** A arte desta familia vem dos pacotes, desenhada na diagonal. */
    @Override
    protected double grausDaArte() {
        return 45;
    }

    @Override
    public int duracaoDaReacao() {
        return investida.duracao();
    }

    @Override
    protected Porte porte() {
        return Porte.UMA_MAO;
    }

    // ------------------------------------------------------------ o golpe

    /**
     * O GOLPE MIRA, como o arco.
     *
     * A espada batia sempre para o lado — a mesma pose de quadril girando uns
     * graus, com a caixa de dano grudada a esquerda ou a direita do gato. Com o
     * bicho em cima ou embaixo, a lamina cortava o ar e o rastro aparecia longe
     * dele. Agora o golpe e um ARCO EM VOLTA DO GATO, centrado na direcao do alvo:
     * a lamina recua para um lado (antecipacao), varre ate o outro em tres ticks
     * (o corte) e segura um instante la (a sustentacao). O dano e um leque nessa
     * direcao e pega todos os bichos dentro dele, nao so o mais proximo.
     */
    private double mira;

    /** Meia abertura do arco, em radianos: de -ARCO a +ARCO em volta da mira. */
    private static final double ARCO = Math.toRadians(95);
    /** Meia abertura do leque de dano: um pouco menor que o desenho do arco. */
    private static final double LEQUE = Math.toRadians(80);
    /** Alcance do corte, em tiles, medido do meio do gato. */
    private static final double ALCANCE = 2.2;

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Enemy nearest = alvoVisivel(player, 3);
        if (nearest != null && !investida.ativa()) {
            this.mira = miraDe(centroX(player), centroY(player), nearest);
            // Alterna o lado a cada golpe: ida e volta, forehand e backhand. Com o
            // leque centrado no alvo, os dois lados acertam, entao a alternancia
            // nao custa golpe nenhum (era por isso que ela tinha saido).
            this.side = -this.side;
            investida.comecar();
        }
        investida.tick();
        this.atacando = investida.ativa();
        if (investida.acertaAgora()) {
            attack(player);
        }
    }

    private static double centroX(Player p) {
        return p.getX() + p.getWidth() / 2d;
    }

    private static double centroY(Player p) {
        return p.getY() + p.getHeight() / 2d;
    }

    /** O avanco continuo, com a fracao entre ticks: ver Investida.avancoSuave. */
    private double suave() {
        return investida.avancoSuave(Engine.alpha());
    }

    /** Onde a lamina aponta agora: a mira mais o avanco do arco. */
    private double anguloDaLamina() {
        return mira + suave() * ARCO * side;
    }

    private void attack(Player player) {
        double cx = centroX(player), cy = centroY(player);
        double alcance = ALCANCE * GameObject.SIZE();
        for (Enemy e : Game.getMap().getEntities(Enemy.class)) {
            double ex = e.getX() + e.getWidth() / 2d, ey = e.getY() + e.getHeight() / 2d;
            double dist = Math.hypot(ex - cx, ey - cy);
            if (dist > alcance + e.getWidth() / 2d) {
                continue;
            }
            double dif = Math.atan2(ey - cy, ex - cx) - mira;
            dif = Math.atan2(Math.sin(dif), Math.cos(dif));
            // Encostado no gato vale de qualquer lado: o angulo ali e ruido.
            if (Math.abs(dif) > LEQUE && dist > GameObject.SIZE() * 0.6) {
                continue;
            }
            e.strike(elemento.ataque(AttackTypes.Melee), this.damage + player.getDamage());
            e.getPhysical().addForce("knockback", 3, e.getAngle(player));
        }
        tocarGolpe(Sounds.Sword);
    }

    public void render(Graphics2D g) {
        // Parado, a pose vem do porte, igual para todas as armas. So o golpe
        // e desenhado por aqui.
        if (!atacando) {
            naMao(g, getSprite());
            return;
        }
        Player player = Game.getPlayer();
        drawAttackEffect(g, player);
        BufferedImage sprite = getSprite();
        if (sprite == null) {
            return;
        }
        // A lamina ORBITA o gato apontando para fora, como o arco aponta para o
        // alvo. No corte ela se estende um pouco: e o braco esticando no impacto.
        double lamina = anguloDaLamina();
        double raio = GameObject.SIZE() * (0.55 + 0.15 * Math.max(0, suave()));
        double x = centroX(player) + Math.cos(lamina) * raio;
        double y = centroY(player) + Math.sin(lamina) * raio;
        desenharGirado(g, sprite, x, y, lamina + Rotate.DIAGONAL);
    }

    /**
     * Gira sem borrar. O Rotate.draw usa interpolacao bilinear, e numa lamina de
     * poucos pixels isso a transformava numa mancha — parte do "feio" do golpe.
     * Sem espelhar, tambem: espelhar no meio do arco faria a espada pular.
     */
    private static void desenharGirado(Graphics2D g, BufferedImage sprite, double x, double y, double ang) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate(x, y);
        at.rotate(ang);
        at.translate(-sprite.getWidth() / 2d, -sprite.getHeight() / 2d);
        g2.drawImage(sprite, at, null);
        g2.dispose();
    }

    /**
     * O rastro do corte: uma lua crescente que segue a lamina.
     *
     * Ele so aparece do impacto em diante e some na sustentacao. Rastro aceso o
     * golpe inteiro vira parte do desenho, e nao um golpe. O arco desenhado vai
     * de onde a lamina comecou a varrer ate onde ela esta, entao ele CRESCE junto
     * com o corte em vez de surgir pronto.
     */
    private void drawAttackEffect(Graphics2D g, Player player) {
        double avanco = suave();
        if (avanco < -0.6 || !investida.ativa()) {
            return;
        }
        double inicio = mira - ARCO * side;
        double fim = anguloDaLamina();
        double raio = GameObject.SIZE() * 1.35;
        int s = Configs.GameScale();
        // Some aos poucos depois do impacto: forte no corte, fraco na sustentacao.
        float forca = (float) Math.max(0, Math.min(1, (avanco + 0.6) / 1.2));
        if (avanco > 0.86) {
            forca *= (float) ((1 - avanco) / 0.14);
        }
        Color cor = elemento == Elemento.NENHUM ? Color.WHITE : elemento.cor();
        double cx = centroX(player), cy = centroY(player);

        // UMA LUA CHEIA, E NAO DUAS LINHAS. Os dois tracos de espessura fixa liam
        // como riscos paralelos. Aqui o rastro e uma faixa preenchida que afina
        // ate sumir na cauda e engrossa perto da lamina, com a transparencia
        // acompanhando — e o borrao de movimento de um corte, nao um desenho.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double espessura = raio * 0.42;
        desenharFaixa(g2, cx, cy, inicio, fim, raio + s, espessura + 2 * s, cor, 70 * forca);   // halo
        desenharFaixa(g2, cx, cy, inicio, fim, raio, espessura, cor, 220 * forca);             // miolo
        g2.dispose();
    }

    /**
     * A faixa em CAMADAS empilhadas, e nao em gomos lado a lado.
     *
     * Gomos translucidos encostados deixam costura: onde dois se tocam a borda
     * suavizada soma duas vezes, e o rastro sai listrado. Aqui cada camada e uma
     * lua inteira, de uma forma so, que comeca um pouco mais perto da lamina que
     * a anterior. Somadas, a opacidade cresce da cauda ate a ponta sem nenhuma
     * emenda. A espessura afina ate zero na cauda de cada camada.
     */
    private static void desenharFaixa(Graphics2D g, double cx, double cy, double inicio, double fim,
                                      double raio, double espessura, Color cor, double alfaMax) {
        int camadas = 7;
        int alfa = (int) Math.max(0, Math.min(255, alfaMax / camadas * 1.6));
        if (alfa <= 0 || Math.abs(fim - inicio) < 1e-3) {
            return;
        }
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alfa));
        for (int c = 0; c < camadas; c++) {
            double u0 = c / (double) (camadas + 1);
            double a0 = inicio + (fim - inicio) * u0;
            g.fill(lua(cx, cy, a0, fim, raio, espessura * (1 - u0 * 0.35)));
        }
    }

    /** Uma lua: borda de fora no raio, borda de dentro afinando ate a cauda. */
    private static java.awt.geom.Path2D lua(double cx, double cy, double a0, double a1,
                                            double raio, double espessura) {
        int passos = 24;
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        for (int i = 0; i <= passos; i++) {
            double a = a0 + (a1 - a0) * i / passos;
            double x = cx + Math.cos(a) * raio, y = cy + Math.sin(a) * raio;
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        for (int i = passos; i >= 0; i--) {
            double u = i / (double) passos;
            // Cauda fina, barriga cheia, ponta arredondada junto da lamina.
            double e = espessura * Math.pow(u, 0.8) * Math.min(1, (1 - u) * 5 + 0.55);
            double a = a0 + (a1 - a0) * u;
            p.lineTo(cx + Math.cos(a) * (raio - e), cy + Math.sin(a) * (raio - e));
        }
        p.closePath();
        return p;
    }

}
