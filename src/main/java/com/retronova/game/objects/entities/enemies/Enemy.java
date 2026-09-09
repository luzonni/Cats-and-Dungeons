package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawSprite;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Expressao;
import com.retronova.game.objects.entities.Nascente;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.utilities.Xp;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Volatile;
import com.retronova.game.objects.particles.Word;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Enemy extends Entity implements Nascente {


    private double xpWeight;

    /**
     * Isto aqui e um chefe?
     *
     * Serve para a arena saber quando trocar a trilha. Fica no inimigo, e nao
     * numa lista de classes na arena, porque quem sabe se um bicho e chefe e o
     * bicho: chefe novo que esqueca de sobrescrever isto entra com musica de
     * arena comum, e nao quebra nada.
     */
    public boolean chefe() {
        return false;
    }

    private boolean tookDamage;
    private int countTookDamage;

    /**
     * Quadros ate o bicho estar inteiro e comecar a agir.
     *
     * A onda inteira aparecia PRONTA: no quadro em que o inimigo existe, ele ja
     * esta perseguindo. Num jogo em que a briga acontece toda de uma vez isso nao
     * e dificuldade, e sequestro de atenção — o jogador nao teve como ver de onde
     * veio nem para onde correr.
     *
     * O nascimento resolve isso com o recurso que Hades usa e que a analise de
     * design daquele jogo descreve nesses termos: o inimigo SEGURA UMA POSE COBERTO
     * DE BRANCO para sinalizar o que vem. Aqui o branco vai sumindo enquanto a cor
     * volta, e so quando ele acaba o bicho passa a se mexer.
     *
     * UM SEGUNDO E MEIO, e o numero tem razao de ser. Reagir a um estimulo visual
     * simples leva de duzentos a trezentos milissegundos, ou seja, de doze a dezoito
     * quadros; telegrafia precisa disso MAIS o tempo de ler e decidir, e a faixa que
     * se usa na pratica vai de meio segundo a tres. A primeira versao ficou em
     * setenta e cinco centesimos e passou rapido demais — porque aqui o jogador nao
     * esta reagindo a um estimulo simples: ele precisa ver ONDE nasceram varios
     * bichos de uma vez e escolher para que lado correr. Noventa quadros dao esse
     * tempo sem que a onda vire espera.
     */
    private static final int NASCENDO = 90;

    /**
     * Atraso proprio de cada bicho antes de o portal dele abrir.
     *
     * A PRIMEIRA TENTATIVA ESPACOU OS SONS e o efeito foi o oposto do pedido: com
     * cinco bichos nascendo no mesmo quadro, quatro estalos eram engolidos e a onda
     * inteira soava como um portal so. O problema nunca foi o som — era os cinco
     * portais abrirem no mesmo instante.
     *
     * Atrasando cada um por um punhado de quadros diferente, a onda vira uma
     * SEQUENCIA: os portais abrem em cascata, cada estalo sai do seu lado, e da
     * para contar quantos vem e de onde. E fica melhor de ver, alem de ouvir.
     *
     * QUARENTA QUADROS NAO BASTARAM. Com seis bichos sorteando dentro de dois
     * tercos de segundo, varios caiam no mesmo quadro ou a um quadro de distancia,
     * e estalos assim proximos se somam num so — que foi exatamente o relato: seis
     * nascendo, um ouvido. Cem quadros espalham a mesma onda por quase dois
     * segundos, e ai cada portal tem o seu instante.
     */
    private static final int ATRASO_MAXIMO = 100;

    private int nascendo = NASCENDO + Engine.RAND.nextInt(ATRASO_MAXIMO);

    /** Ainda esta se formando. Enquanto for verdade, nao age. */
    public boolean nascendo() {
        return nascendo > 0;
    }

    /**
     * A DISTANCIA EM QUE O BICHO DESISTE, em tiles.
     *
     * Ate agora a perseguicao era infinita: uma vez visto, o inimigo vinha atras
     * pela sala toda para sempre. A pratica corrente e a oposta — quem persegue tem
     * um limite, e sair dele e uma jogada valida. As discussoes de design sobre
     * agressividade batem sempre na mesma frase: nao ha razao para um bicho
     * perseguir alguem eternamente; o que existe e um raio de aggro e um raio maior
     * onde ele solta.
     *
     * Vinte tiles e mais que a tela inteira, entao dentro da arena a briga continua
     * como sempre foi. O que muda e o caso que incomodava: atravessar a sala nao
     * arrasta mais uma fila de bichos pendurada em voce para o resto do turno.
     */
    public static final double COLEIRA_EM_TILES = 20;

    protected Enemy(int ID, double x, double y, int weight) {
        super(ID, x, y, weight);
        addResistances(AttackTypes.Flat, 0);
    }

    /**
     * Conta o nascimento. Chamado pelo laco do jogo mesmo enquanto o bicho nao age.
     *
     * Fica separado do tick porque e exatamente o tick que esta suspenso: se o
     * contador morasse la dentro, ele nunca chegaria a zero e o inimigo ficaria
     * branco e parado para sempre.
     */
    /**
     * O tamanho do portal do bicho agora, de 0 a 1.
     *
     * Abre no primeiro terco, segura no meio e fecha no ultimo — o mesmo desenho de
     * tres tempos da chegada do gato. Um disco que aparece pronto no tamanho final
     * le como decalque colado no chao; crescendo, le como algo abrindo.
     */
    /**
     * De -1 (esquerda da tela) a 1 (direita), para o som sair do lado certo.
     *
     * Medido contra a CAMERA e nao contra o gato: o que o jogador ouve tem de bater
     * com o que ele ve, e o que ele ve e o recorte da camera.
     */
    private double panorama() {
        double meia = Math.max(1, Engine.window.getWidth() / 2d);
        double naTela = (getX() - Game.getCam().getX()) - meia;
        // NUNCA TOTALMENTE DE UM LADO SO.
        //
        // Os bichos nascem espalhados por trinta e quatro tiles e a camera mostra
        // uns vinte, entao quase todo nascimento acontece FORA DA TELA — e a conta
        // saturava em um inteiro, jogando o som inteiro num unico canal. Quem ouve
        // pelo lado errado simplesmente nao ouve, e foi por isso que o estalo sumiu.
        //
        // Seis decimos e o bastante para o ouvido apontar o lado e ainda deixar som
        // no outro canal. Panoramica serve para indicar direcao, nao para desligar
        // metade do jogo.
        double limite = 0.6;
        return Math.max(-limite, Math.min(limite, naTela / meia * limite));
    }

    public float aberturaDoPortal() {
        if (!nascendo()) {
            return 0f;
        }
        int decorrido = NASCENDO - restante();
        int terco = Math.max(1, NASCENDO / 3);
        if (decorrido < terco) {
            return decorrido / (float) terco;
        }
        if (restante() > terco) {
            return 1f;
        }
        return restante() / (float) terco;
    }

    /**
     * O corpo so aparece depois de o portal ter aberto.
     *
     * O primeiro terco do nascimento e do portal sozinho. Dali em diante o bicho
     * entra branco e vai ganhando cor pelos dois tercos restantes.
     */
    @Override
    public boolean corpoVisivel() {
        return restante() <= NASCENDO - NASCENDO / 3;
    }

    /** Quanto falta do nascimento propriamente dito, ja descontado o atraso. */
    private int restante() {
        return Math.min(nascendo, NASCENDO);
    }

    public void tickNascimento() {
        if (nascendo == NASCENDO) {
            // O SOM SAI DE ONDE O PORTAL ABRE.
            //
            // Numa onda os bichos nascem espalhados e fora da tela, e o jogador so
            // descobre onde eles estao quando ja chegaram nele. O estalo com
            // panoramica resolve isso sem nenhum elemento visual: o ouvido aponta o
            // lado antes de o olho achar. E, de quebra, uma onda passa a ter som de
            // acontecimento em vez de comecar em silencio.
            // O SOM PRECISA CORTAR O COMBATE, e o sopro nao cortava.
            //
            // O encanamento estava certo o tempo todo — a instrumentacao mostrou
            // cinquenta e tres disparos, sem excecao, com panoramica em faixa
            // valida, e a propria biblioteca aceita esse intervalo. O que faltava
            // era MASSA: o woosh e um sopro de meio segundo, e sopro desaparece
            // debaixo de espada, passo e grito de bicho. Um estalo seco tem
            // transiente, e transiente e o que se ouve num ambiente cheio.
            Sound.play(Sounds.Portal, panorama());
        }
        if (nascendo > 0) {
            nascendo--;
        }
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new SpriteSheet("sprites/objects/enemy", sprites));
    }

    @Override
    public void postTick() {
        super.postTick();
        if(tookDamage) {
            countTookDamage++;
            if(countTookDamage > 50) {
                countTookDamage = 0;
                tookDamage = false;
            }
        }
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        this.strike(type, damage, new Volatile("damagemobs", 0, 0));
    }

    @Override
    /**
     * Ponto unico por onde passa todo dano que o gato causa.
     *
     * O aviso ao jogador fica AQUI, e nao espalhado por arma: sao mais de vinte
     * itens chamando strike, e cada um lembrando de avisar por conta propria seria
     * uma lista que envelhece na primeira arma nova.
     */
    public void strike(AttackTypes type, double damage, Particle particle) {
        // BICHO NASCENDO NAO LEVA DANO. E o outro lado do mesmo trato: o jogo lhe
        // da um segundo e meio para ler a sala, e em troca aquele segundo e meio
        // nao vale pontos. Sem isto, a jogada otima passa a ser correr ate o ponto
        // de spawn e bater no bicho antes de ele existir — o que apaga justamente o
        // tempo de leitura que a espera existe para dar.
        if (nascendo()) {
            return;
        }
        Player jogador = Game.getPlayer();
        if (jogador != null) {
            jogador.reagirAoGolpe();
        }
        Player player = Game.getPlayer();
        damage *= Engine.RAND.nextDouble() + player.getLuck();
        super.strike(type, damage);
        this.tookDamage = true;
        double x = getX() + Engine.RAND.nextDouble(getWidth());
        double y = getY() + Engine.RAND.nextDouble(getHeight());
        particle.setX(x);
        particle.setY(y);
        if(damage >= 1d)
            Game.getMap().put(new Word(String.valueOf((int)damage), x, y, 1));
        Game.getMap().put(particle);
    }

    public BufferedImage getSprite() {
        BufferedImage currentSprite = super.getSprite();
        if (nascendo()) {
            // A COR VOLTA AOS POUCOS. Branco cheio no primeiro quadro e o desenho
            // inteiro no ultimo: e a formacao acontecendo, e nao um piscar. Um
            // clarao ligado e desligado diria "levei dano", que e outra coisa e ja
            // usa esse mesmo recurso logo abaixo.
            float quanto = restante() / (float) NASCENDO;
            return Expressao.clarao(currentSprite, quanto);
        }
        if(tookDamage) {
            currentSprite = DrawSprite.draw(currentSprite, new Color(122, 19, 17));
        }
        return currentSprite;
    }

    protected void setXpWeight(double weight) {
        this.xpWeight = weight;
    }

    protected double getXpWeight() {
        return this.xpWeight;
    }

    @Override
    public void die() {
        this.disappear();
        dropXp();
    }

    private void dropXp() {
        double luck = Game.getPlayer().getLuck();
        Xp e = new Xp(getX(), getY());
        e.setWeight(getXpWeight() * Engine.RAND.nextDouble() * luck);
        Game.getMap().put(e);
        e.getPhysical().addForce("move", 1, Math.PI*2);
    }

    public void renderLife(Graphics2D g) {
        if(getLife() == getLifeSize() || getLife() < 0)
            return;
        int x = (int)getX();
        int y = (int)getY() + getHeight() + Configs.GameScale() *2;
        int w = getWidth();
        int h = Configs.GameScale() * 3;

        g.setColor(new Color(135, 35, 65));
        g.fillRect(x, y, w, h);
        double lifeSize = w * (getLife() / getLifeSize());
        g.setColor(new Color(190, 49, 68));
        g.fillRect(x, y, (int) lifeSize, h);

        g.setStroke(new BasicStroke(Configs.GameScale()));
        g.setColor(new Color(9, 18, 44));
        g.drawRect(x, y, w, h);
    }

}
