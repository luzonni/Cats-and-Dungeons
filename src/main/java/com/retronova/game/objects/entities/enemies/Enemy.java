package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawSprite;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Investida;
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
        // Flat com zero nao fazia nada: resistir zero por cento e nao resistir.
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

    /**
     * A folha de particula que combina com o tipo de dano.
     *
     * QUATRO DAS SEIS JA ESTAVAM EM particle/, E DUAS SEM USO NENHUM. O floco que
     * estilhaca (ice) e a gota que estoura (water) estavam no repositorio desde
     * sempre, sem uma linha de codigo referenciando — a mesma historia dos WAV
     * elementais. Sao impactos prontos, no traco da casa. Terra e ar sao os unicos
     * que precisaram existir, e saem de tools/GenParticulasElementais.java.
     *
     * O PADRAO CONTINUA SENDO O BRILHO DOURADO. Todo tipo nao-elemental — corte,
     * perfuracao, explosao, laser — cai nele, e cair no padrao e o certo: o
     * dourado diz "acertou", que e o que esses golpes tem a dizer. So o elemento
     * tem uma segunda frase, e por isso so ele ganha desenho proprio.
     */
    private static String particulaDe(AttackTypes type) {
        return switch (type) {
            case Fire -> "fire";
            case Ice -> "ice";
            case Water -> "water";
            case Earth -> "earth";
            case Air -> "air";
            default -> "damagemobs";
        };
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        // O ELEMENTO PRECISAVA APARECER, E NAO SO SOAR.
        //
        // Ate aqui todo golpe do jogo produzia a MESMA faisca dourada, viesse de
        // espada, de fogo ou de gelo. Com o elemento virando a escolha central da
        // corrida, a tela era o unico lugar que nao tomava conhecimento dela: a
        // agua se ouvia, a arma era de agua, e o impacto continuava generico.
        //
        // A ESCOLHA MORA AQUI, e nao em cada arma, pelo mesmo motivo que o aviso de
        // dano ja morava: sao mais de vinte itens chamando strike, e cada um
        // lembrando de escolher a propria particula seria uma lista que envelhece
        // na primeira arma nova. Quem sabe o elemento do golpe e o TIPO DE DANO, e
        // ele chega aqui de graca.
        this.strike(type, damage, new Volatile(particulaDe(type), 0, 0));
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
        // A ARMADURA COME O GOLPE ANTES DA VIDA, e o que sobra passa.
        //
        // O EXCEDENTE PASSAR e deliberado: sem isso, um golpe enorme contra uma
        // armadura minuscula seria desperdicado por inteiro, e quem guardou o
        // ataque pesado para o momento certo seria punido por acertar.
        //
        // O aviso visual continua saindo mesmo quando a armadura absorve tudo. Um
        // golpe que nao produz nada na tela le como golpe que nao aconteceu, e o
        // jogador conclui que a arma esta quebrada em vez de que o bicho esta duro
        // — e a barra amarela esta logo ali dizendo o contrario.
        if (armadura > 0) {
            double sobra = damage - armadura;
            armadura = Math.max(0, armadura - damage);
            damage = Math.max(0, sobra);
        }
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
        if(getLife() == getLifeSize() || getLife() < 0) {
            // A ARMADURA APARECE MESMO COM A VIDA CHEIA. E o caso normal: enquanto
            // ela nao acaba a vida nem comecou a cair, e um bicho sem barra nenhuma
            // que nao morre le como bug.
            if (temArmadura()) {
                renderArmadura(g);
            }
            return;
        }
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
        renderArmadura(g);
    }

    /**
     * A barra da armadura, ACIMA da vida e em amarelo.
     *
     * Acima e nao no lugar: as duas sao quantidades diferentes e o jogador precisa
     * ver as duas para saber se esta progredindo. Amarelo porque e a cor que o
     * Hades usa para armadura, e porque nao colide com o vermelho da vida nem com
     * o clarao branco do golpe.
     */
    private void renderArmadura(Graphics2D g) {
        if (armaduraCheia <= 0 || armadura <= 0) {
            return;
        }
        int x = (int) getX();
        int h = Configs.GameScale() * 3;
        int y = (int) getY() + getHeight() + Configs.GameScale() * 2 - h - Configs.GameScale();
        int w = getWidth();
        g.setColor(new Color(90, 70, 25));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(232, 200, 74));
        g.fillRect(x, y, (int) (w * (armadura / armaduraCheia)), h);
        g.setStroke(new BasicStroke(Configs.GameScale()));
        g.setColor(new Color(9, 18, 44));
        g.drawRect(x, y, w, h);
    }


    // ------------------------------------------------------------ o golpe
    //
    // POR QUE ISTO EXISTE, e o que ele substitui.
    //
    // Sete dos dez inimigos machucavam por COLISAO: bastava a caixa do gato tocar
    // a do bicho e o dano saia, com um contador como unico freio. Isso quebra as
    // duas coisas que fazem um combate ser combate.
    //
    // A PRIMEIRA e que colisao NAO PODE SER TELEGRAFADA. Nao existe instante entre
    // "encostou" e "tomou", entao nao ha nada que o jogador possa ler, prever ou
    // evitar — apanhar deixa de ser um erro dele e vira uma consequencia de estar
    // perto. A literatura de design e unanime: sem aviso, o golpe chega do nada e
    // levar dano parece arbitrario; com aviso, vira uma troca legivel em que o
    // jogador reconhece a deixa e responde.
    //
    // A SEGUNDA e que colisao nao tem INTENCAO. O bicho nao decidiu bater; ele
    // estava andando e voce estava no caminho. Os jogadores descrevem exatamente
    // este caso como o mais irritante — tomar dano quando esta claro que o inimigo
    // nao tentou acertar nada.
    //
    // Aqui todo golpe passa pela Investida, a MESMA estrutura que as armas do gato
    // usam: preparo, corte, extensao, recuperacao. Isso importa para alem de
    // reaproveitar codigo — e o que faz o jogador reconhecer a mesma gramatica dos
    // dois lados da briga, em vez de aprender um caso por inimigo.
    //
    // E O BICHO SE COMPROMETE. Ele para de andar ao iniciar o preparo, e o dano so
    // sai se o gato AINDA estiver no alcance no quadro do impacto. Sair da frente
    // depois do aviso passa a funcionar — que e a definicao de "dar tempo de
    // desviar".

    private Investida golpe;
    private double alcanceDoGolpe;
    private double danoDoGolpe;
    private AttackTypes tipoDoGolpe = AttackTypes.Melee;
    private double empurraoDoGolpe;
    private int descanso;
    private int desdeOGolpe;
    private Sounds vozDoGolpe;

    /**
     * Ensina este bicho a bater, com aviso.
     *
     * @param investida  as quatro fases; ver Investida.rapida/leve/media/pesada
     * @param alcance    a que distancia ele comeca e ate onde o golpe acerta, em tiles
     * @param descanso   quadros de espera entre um golpe e o proximo
     */
    protected void golpeCorpoACorpo(Investida investida, double alcance, double dano,
                                    AttackTypes tipo, double empurrao, int descanso,
                                    Sounds voz) {
        this.golpe = investida;
        this.alcanceDoGolpe = alcance;
        this.danoDoGolpe = dano;
        this.tipoDoGolpe = tipo;
        this.empurraoDoGolpe = empurrao;
        this.descanso = descanso;
        this.vozDoGolpe = voz;
        this.desdeOGolpe = descanso;
    }

    /**
     * Roda o golpe.
     *
     * @return true enquanto o bicho estiver PRESO no golpe — quem chama deve parar
     *         de andar. Um inimigo que ataca andando nao tem preparo visivel: o
     *         aviso so existe se ele se plantar para dar o golpe.
     */
    protected boolean tickGolpe() {
        if (golpe == null) {
            return false;
        }
        desdeOGolpe++;
        Player gato = Game.getPlayer();
        if (!golpe.ativa() && desdeOGolpe >= descanso && noAlcance(gato)) {
            golpe.comecar();
        }
        boolean estava = golpe.ativa();
        golpe.tick();
        if (golpe.acertaAgora()) {
            // O ALVO E CONFERIDO DE NOVO NO QUADRO DO IMPACTO. E isto, e nao o
            // preparo, que faz o aviso valer: quem saiu do alcance depois de ver a
            // deixa nao leva o golpe. Sem esta linha o preparo seria enfeite.
            if (noAlcance(gato)) {
                gato.strike(tipoDoGolpe, danoDoGolpe);
                if (empurraoDoGolpe > 0) {
                    gato.getPhysical().addForce("knockback", empurraoDoGolpe,
                            gato.getAngle(this) + Math.PI);
                }
            }
            if (vozDoGolpe != null) {
                Sound.play(vozDoGolpe);
            }
        }
        if (estava && !golpe.ativa()) {
            desdeOGolpe = 0;
        }
        return golpe.ativa();
    }

    private boolean noAlcance(Player gato) {
        return gato != null && !gato.morrendo()
                && getDistance(gato) <= GameObject.SIZE() * alcanceDoGolpe;
    }

    /**
     * O quanto o bicho esta "carregado", de 0 a 1. Zero quando nao ha golpe.
     *
     * E O AVISO, e ele e desenhado sem arte nova: o corpo do bicho vai ficando
     * branco durante o preparo e estoura no impacto. O jogo ja usa exatamente este
     * clarao quando o gato leva pancada, entao o jogador nao precisa aprender um
     * simbolo novo — ele ja sabe que branco significa "acontecendo agora".
     */
    protected float cargaDoGolpe() {
        if (golpe == null || !golpe.ativa()) {
            return 0f;
        }
        double avanco = golpe.avanco();
        // O avanco vai de -1 (recuo maximo, fim do preparo) a +1 (impacto). O
        // clarao acompanha o recuo: quanto mais recuado, mais perto de bater.
        return (float) Math.max(0, Math.min(1, -avanco));
    }

    /** O bicho esta preso no proprio golpe e nao deve se mover. */
    protected boolean golpeando() {
        return golpe != null && golpe.ativa();
    }

    /**
     * Desenha o bicho com o aviso do golpe por cima.
     *
     * Passa a ser o caminho de todos: cada inimigo desenhava o proprio sprite
     * direto, e um aviso que so alguns tivessem seria pior do que nenhum — o
     * jogador aprenderia a confiar no clarao e seria punido justamente pelos que
     * nao o tem.
     */
    protected void renderComAviso(BufferedImage sprite, Graphics2D g) {
        float carga = cargaDoGolpe();
        renderSprite(carga > 0.01f
                ? com.retronova.game.objects.entities.Expressao.clarao(sprite, carga)
                : sprite, g);
    }

    // ------------------------------------------------------- dificuldade

    /**
     * ESCALAR PELA SALA, E NUNCA PELO PODER DO JOGADOR.
     *
     * A distincao decide se a progressao existe ou nao, e ela e a queixa mais
     * repetida que jogadores fazem a jogos com escalonamento: "quanto mais subo de
     * nivel, mais fraco fico". Quando o inimigo acompanha os SEUS atributos, toda
     * carta que voce escolhe e imediatamente anulada — e uma esteira que acelera
     * junto com voce, e o progresso vira ilusao.
     *
     * Pela sala e outra coisa. A curva e fixa e conhecida; a sua build cresce mais
     * rapido que ela, entao voce SENTE que ficou forte e ainda assim a sala quinze
     * e mais dura que a primeira. E como Vampire Survivors escala — por tempo
     * decorrido, nao por poder — e e o que o Hades faz com biomas, so que la os
     * inimigos mais fundos sao OUTROS em vez dos mesmos mais duros.
     *
     * Os dois numeros sao diferentes de proposito. A VIDA sobe mais rapido que o
     * DANO porque vida que sobe alonga a briga e dano que sobe encurta a sua vida:
     * subir os dois juntos deixaria a sala vinte impossivel em vez de longa. Sao
     * valores de partida, para sentir e ajustar.
     */
    private static final double CRESCIMENTO_VIDA = 0.07;
    private static final double CRESCIMENTO_DANO = 0.04;

    /**
     * Deixa este bicho na dureza da sala em que ele nasceu.
     *
     * Chamado pelo Waves, que e o unico lugar que cria inimigos de onda. Quem e
     * posto pelo JSON de mapa — o saguao — nao passa por aqui e continua no valor
     * de fabrica, que e o certo: aquela sala nao tem numero.
     */
    public void escalarPara(int sala) {
        if (sala <= 0) {
            return;
        }
        escalarVida(1 + CRESCIMENTO_VIDA * sala);
        double porDano = 1 + CRESCIMENTO_DANO * sala;
        this.danoDoGolpe *= porDano;
        // O MouseExplode nao bate com golpe: o estouro dele le o getDamage.
        setDamage(getDamage() * porDano);
    }

    // ---------------------------------------------------------- armadura

    /**
     * VIDA EXTRA QUE NAO OLHA O TIPO DO DANO.
     *
     * E o que o Hades usa no lugar de resistencia, e a diferenca entre as duas
     * ideias e exatamente o que este jogo precisava. Resistencia pergunta COM O QUE
     * voce esta batendo, e por isso ela favorece um personagem e pune outro sem que
     * nenhum dos dois possa fazer nada a respeito. Armadura pergunta QUANTO — todo
     * mundo atravessa, ninguem atravessa de graca.
     *
     * Ela deixa o bicho duro sem deixa-lo duro PARA VOCE ESPECIFICAMENTE, que era o
     * defeito. E continua sendo uma diferenca de comportamento legivel: enquanto
     * houver armadura, a barra amarela em cima da vermelha diz que ainda nao
     * comecou a valer.
     */
    private double armadura;
    private double armaduraCheia;

    protected void setArmadura(double quanto) {
        this.armadura = Math.max(0, quanto);
        this.armaduraCheia = this.armadura;
    }

    public boolean temArmadura() {
        return this.armadura > 0;
    }


}
