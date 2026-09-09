package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Debugging;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.engine.io.Resources;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Inventory;
import com.retronova.game.items.Shield;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.Alpha;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.particles.Poeira;
import com.retronova.game.objects.particles.Word;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {

    public static final Player[] TEMPLATES = new Player[] {
            build("Muffin"),
            build("Azrael"),
            build("Finn")
    };

    private static Player build(String name) {
        try {
            JSONObject json = Resources.getJsonFile("players", name);
            JSONObject values = (JSONObject) json.get("values");
            JSONArray inventory = (JSONArray) json.get("inventory");
            double life = ((Number)values.get("life")).doubleValue();
            double damage = ((Number)values.get("damage")).doubleValue();
            double speed = ((Number)values.get("speed")).doubleValue();
            double luck = ((Number)values.get("luck")).doubleValue();
            double attackSpeed = ((Number)values.get("attackSpeed")).doubleValue();
            double range = ((Number)values.get("range")).doubleValue();
            int bagSize = ((Number)values.get("bagSize")).intValue();
            int hotBarSize = ((Number)values.get("hotBarSize")).intValue();
            Player player = new Player(name, life, damage, speed, luck, attackSpeed, range, bagSize, hotBarSize);
            // Ficha de apresentacao: papel e lore ficam ao lado dos atributos, no
            // mesmo arquivo, para nao existirem dois lugares onde um personagem e
            // descrito. A arma inicial nao e declarada — e o primeiro item do
            // inventario, que ja esta logo abaixo.
            JSONObject profile = (JSONObject) json.get("profile");
            if(profile != null) {
                player.papel = String.valueOf(profile.getOrDefault("role", ""));
                player.lore = String.valueOf(profile.getOrDefault("lore", ""));
                Object voz = profile.get("voice");
                if(voz != null) {
                    try {
                        player.voz = Sounds.valueOf(String.valueOf(voz));
                    }catch (IllegalArgumentException naoExiste) {
                        System.err.println("profile.voice invalido em " + name + ": " + voz);
                    }
                }
                Object gemido = profile.get("hurt");
                if(gemido != null) {
                    try {
                        player.dor = Sounds.valueOf(String.valueOf(gemido));
                    }catch (IllegalArgumentException naoExiste) {
                        System.err.println("profile.hurt invalido em " + name + ": " + gemido);
                    }
                }
                Object cor = profile.get("color");
                if(cor != null) {
                    try {
                        player.cor = Integer.decode(String.valueOf(cor));
                    }catch (NumberFormatException naoEhCor) {
                        System.err.println("profile.color invalido em " + name + ": " + cor);
                    }
                }
            }
            if(!inventory.isEmpty()) {
                player.arma = String.valueOf(((JSONArray) inventory.get(0)).get(0));
            }
            for(int i = 0; i < inventory.size(); i++) {
                JSONArray itemValues = (JSONArray) inventory.get(i);
                for(ItemIDs itemID : ItemIDs.values()) {
                    if(itemID.name().equalsIgnoreCase((String)itemValues.get(0))) {
                        int id = itemID.ordinal();
                        int amount = 1;
                        if(itemValues.size() > 1)
                            amount = ((Number)itemValues.get(1)).intValue();
                        Item item = Item.build(id, amount);
                        player.getInventory().give(item);
                        break;
                    }
                }
            }
            return player;
        }catch (IOException ignore) {
            throw new EntityNotFound("Player não encontrado");
        }
    }

    public static Player newPlayer(int index) {
        return build(TEMPLATES[index].getName());
    }

    private final String name;
    /** Papel do personagem, em uma palavra. Serve de rotulo na selecao. */
    private String papel = "";
    /** Uma frase de historia. Aparece na selecao, nunca durante a partida. */
    private String lore = "";
    /** Nome da arma inicial, derivado do primeiro item do inventario. */
    private String arma = "";
    /**
     * Miado deste gato.
     *
     * Fica no JSON junto do papel e da lore, e nao numa tabela de nomes aqui,
     * pelo mesmo motivo dos outros campos de ficha: nao existirem dois lugares
     * onde um personagem e descrito. Se o valor nao casar com nenhum som, ele
     * cai no miado generico e o erro sai no console em vez de derrubar o jogo.
     */
    private Sounds voz = Sounds.Cat;
    /** O gemido de dor deste gato. Cai no generico se o perfil nao disser. */
    private Sounds dor = Sounds.DamageCat;
    /**
     * Cor de ambiente do personagem, para a tela de selecao.
     *
     * Nao tem efeito nenhum na partida. Existe porque tingir a tela com a cor de
     * quem esta em cena e o que da presenca propria a cada um antes mesmo de se
     * ler o nome — e o que a tela de selecao do Slay the Spire faz.
     */
    private int cor = 0x383957;
    private double XP;
    private int money;
    private int level;
    private int countAnim;

    private final double luck;
    private double rangeOfColect;

    private int countDash;
    private boolean dash;
    /**
     * Estados da folha, na ordem em que sao carregados.
     *
     * O dormindo entrou depois: o gato ja soltava "Z" quando ficava parado, mas
     * continuava com a mesma cara de acordado, e Z sem pose de sono e meia piada.
     */
    private static final int PARADO = 0, ANDANDO = 1, DORMINDO = 2;

    /**
     * Ritmo da folha de sprites, em ticks por quadro.
     *
     * As tres folhas tem quatro quadros; o compasso e o que separa uma animacao
     * da outra. As duas primeiras rodavam quase na mesma velocidade, e por isso
     * parado e andando liam igual.
     *
     * PARADO, dezoito ticks: cada pose fica 0,6 s no ar. A referencia recomenda
     * de 0,4 a 0,5 s, e esta um pouco acima de proposito — a 0,43 s a cabeca
     * ainda parecia estar balancando em vez de respirando.
     *
     * ANDANDO, oito ticks: 133 ms por quadro, dentro da faixa de 100 a 150 ms
     * recomendada para caminhada. O ciclo de quatro poses fecha em pouco mais de
     * meio segundo, que sao dois passos.
     *
     * DORMINDO, trinta e dois ticks: mais de um segundo por pose. Sono tem de ser
     * visivelmente mais lento que descanso acordado, senao os dois viram a mesma
     * animacao com os olhos diferentes.
     */
    private static final int[] TICKS_DO_ESTADO = {18, 8, 32};

    /**
     * Contador de quadros da folha. A SpriteSheet nao expoe o indice dela.
     *
     * Zerado junto com a folha toda vez que o gato troca entre parado e andando,
     * senao ele continuaria de onde a outra animacao parou e o balanco cairia no
     * quadro errado — um passo comecando no meio da passada.
     */
    private int quadroAtual;

    /**
     * Ha quantos ticks o gato esta parado.
     *
     * Depois de um tempo sem se mexer ele cochila, como o vendedor ja fazia. E o
     * mesmo raciocinio da respiracao lenta: parado tem de LER como parado, e um
     * bicho que descansa quando ninguem mexe nele diz isso melhor do que qualquer
     * quantidade de quadros de animacao.
     */
    private int ticksParado;
    private int ticksDoZ;
    private boolean cochilando;

    /**
     * Quanto tempo de quietude ate o gato cochilar, e o intervalo entre os Zs.
     *
     * Doze segundos, e nao cinco. Cinco pegava o jogador so lendo a tela ou
     * decidindo para onde ir, e o cochilo aparecia como um bug de animacao em vez
     * de uma piada: a graca depende de o gato so dormir quando o teclado foi
     * MESMO largado.
     */
    private static final int ATE_COCHILAR = 60 * 12, ENTRE_ZS = 55;

    /** O sprite avancou de quadro neste tick. */
    private boolean avancouQuadro;
    /** Estado da animacao no tick anterior, para detectar a troca de folha. */
    private int estadoAnterior = -1;

    /**
     * Ultimo lado para onde o gato andou: 1 direita, -1 esquerda.
     *
     * Nao se le a orientacao do Physical direto porque ela e o sinal da
     * velocidade, e vira ZERO assim que o gato para ou anda so na vertical. Lendo
     * dali, o sprite desespelhava sozinho no meio do caminho e o rabo pulava de
     * lado — que e o "as vezes" do defeito. Aqui o lado fica guardado ate ele
     * andar para o outro.
     *
     * Comeca em -1 porque a arte e desenhada com o rabo a direita, que e a pose
     * de quem anda para a esquerda; assim o quadro parado do spawn nao precisa
     * ser espelhado.
     */
    private int ladoDoPasso = -1;

    private final List<Consumable> passives;

    private final Inventory inventory;

    Player(String name, double life, double damage, double speed, double luck, double attackSpeed, double range, int bagSize, int hotSize) {
        super(0, 0, 0, 30);
        this.name = name;
        this.luck = luck;
        this.setRangeOfColect(5);
        this.inventory = new Inventory(bagSize, hotSize);
        this.passives = new ArrayList<>();
        setWidth(0.85);
        setHeight(0.9);
        loadSprites("player_"+name.toLowerCase()+"_idle",
                "player_"+name.toLowerCase()+"_walking",
                "player_"+name.toLowerCase()+"_sleeping");
        setLife(life);
        setDamage(damage);
        setSpeed(speed);
        setAttackSpeed(attackSpeed);
        setRange(range);
        setSolid();
        setMoney(100);
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new SpriteSheet("sprites/objects/player", sprites));
    }

    public String getName() {
        return this.name;
    }

    public String getPapel() {
        return this.papel;
    }

    public String getLore() {
        return this.lore;
    }

    public int getCor() {
        return this.cor;
    }

    public String getArma() {
        return this.arma;
    }

    public Sounds getVoz() {
        return this.voz;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    // ------------------------------------------------------------ reacoes
    //
    // Quanto tempo cada cara fica no rosto, em ticks.
    //
    // Curto de proposito. O retorno de golpe em jogo 2D vive de mudanca brusca e
    // breve: passando de uns poucos quadros, a careta deixa de ser reacao e vira
    // o estado normal do bicho — o gato ficaria permanentemente de cara feia numa
    // sala cheia. Doze ticks sao dois decimos de segundo, tempo de o olho pegar
    // sem o rosto travar naquilo.
    private static final int DOR = 18;
    private static final int GOLPE = 12;

    /**
     * Quadros de clarao branco no comeco da dor.
     *
     * So o comeco: o clarao e o aviso de QUANDO, e aviso longo deixa de ser aviso.
     * A careta continua depois dele, contando o que aconteceu.
     */
    private static final int CLARAO = 6;

    /**
     * Quadros minimos entre dois gemidos de dor.
     *
     * Um quinto de segundo. Curto o bastante para uma sequencia de golpes ainda
     * soar como sequencia, longo o bastante para golpes simultaneos virarem um som
     * so — que e o caso que estava estourando.
     */
    private static final int ENTRE_GEMIDOS = 12;

    private int desdeOGemido = ENTRE_GEMIDOS;

    private int doendo;
    private int golpeando;

    // ------------------------------------------------------------ a queda
    //
    // Ate aqui morrer era um corte seco: o gato sumia do mapa no mesmo quadro e a
    // tela de fim aparecia por cima. Nao havia o instante em que se ENTENDE o que
    // aconteceu, e sem esse instante a derrota vira surpresa administrativa em vez
    // de consequencia — some o bicho, aparece um texto.
    //
    // O que a pratica de animacao recomenda para esse momento e o contrario de
    // mais quadros: e SEGURAR. Baixar a taxa de quadros num gesto e o jeito
    // classico de dar peso a ele, e a morte e o gesto que mais pede peso no jogo
    // inteiro. Entao a sequencia aqui e lenta de proposito e tem tres tempos, na
    // ordem em que o olho consegue ler:
    //
    //   PARADA (12 ticks) — nada se move. E o mesmo recurso do impacto, o hitstop,
    //   esticado: o silencio antes da queda e o que avisa que aquilo foi diferente
    //   dos outros golpes que o gato levou na partida.
    //
    //   TOMBO (24 ticks) — o gato gira noventa graus ate o chao, desacelerando no
    //   fim. Girar, e nao afundar, porque o corpo dele tem uma silhueta em pe bem
    //   definida; deitado, ela le como tombo mesmo sem quadro desenhado para isso.
    //
    //   APAGAR (24 ticks) — deitado, ele perde cor e some. So depois disso a tela
    //   de fim entra, e ai ela chega como conclusao e nao como interrupcao.
    private static final int MORTE_PARADA = 40;
    private static final int MORTE_TOMBO = 70;
    private static final int MORTE_APAGAR = 55;

    /** Quanto a camera fecha em cima do gato durante a queda. */
    private static final float APROXIMACAO = 1.6f;
    private static final int MORTE_TOTAL = MORTE_PARADA + MORTE_TOMBO + MORTE_APAGAR;

    private int morrendo = -1;

    /** O gato esta caindo. Enquanto isto for verdade ele nao obedece ninguem. */
    public boolean morrendo() {
        return morrendo >= 0;
    }

    /**
     * O quanto a tela ja escureceu por causa da queda, de 0 a 1.
     *
     * A ESCURIDAO E DA CENA, e nao do gato. Ate agora so o corpo desaparecia: o
     * resto da arena continuava iluminado, com os bichos andando e o HUD aceso, e
     * por cima disso entrava a tela de fim. Sobrava a sensacao de que o jogo tinha
     * TROCADO DE TELA, e nao de que ele tinha acabado.
     *
     * Fechando a luz junto com o corpo, a arena sai de cena antes de a tela de fim
     * chegar — e ai ela entra num quadro que ja esta quase preto, como continuacao
     * do que se estava vendo. E o mesmo recurso do fecha-para-preto do cinema, pela
     * mesma razao: e o sinal de fim que nao precisa de palavra nenhuma.
     *
     * COMECA DEPOIS DA PARADA. Escurecer desde o primeiro quadro tiraria de vista
     * justamente o que a queda quer mostrar — os olhos se fechando, o tombo. A luz
     * so comeca a cair quando o corpo ja esta no chao.
     */
    public float escuridao() {
        if (!morrendo()) {
            return 0f;
        }
        int desde = morrendo - MORTE_PARADA - MORTE_TOMBO / 2;
        if (desde <= 0) {
            return 0f;
        }
        // Nao chega a preto total: a ultima coisa a sumir e o gato, e um preto
        // absoluto antes disso apagaria a propria queda.
        return Math.min(0.88f, desde / (float) (MORTE_TOMBO / 2 + MORTE_APAGAR));
    }

    /**
     * Comeca a queda em vez de sumir na hora.
     *
     * O gato CONTINUA NO MAPA durante a sequencia, e e isso que segura a tela de
     * fim: quem a dispara e o Game percebendo que o jogador saiu do mapa. Deixando
     * a saida para o ultimo quadro da queda, nao foi preciso inventar nenhum outro
     * caminho de aviso — a tela entra sozinha, na hora certa.
     */
    @Override
    public void die() {
        if (morrendo()) {
            return;
        }
        this.morrendo = 0;
        // UM GEMIDO POR VEZ, mesmo com quatro bichos batendo junto.
        //
        // Cada golpe tocava o proprio som, e cercado por quatro inimigos o gato
        // gemia quatro vezes no mesmo quadro — as vozes se somam e o resultado e um
        // estouro distorcido, alem de nao dizer nada: quatro gemidos nao informam
        // quatro golpes, informam ruido. Um so, com uma pausa curta antes do
        // proximo, continua avisando que voce esta apanhando e volta a ser legivel.
        if (desdeOGemido >= ENTRE_GEMIDOS) {
            desdeOGemido = 0;
            Sound.play(this.dor);
        }
    }

    private void tickMorte() {
        this.morrendo++;
        // Enquanto cai, o gato para de empurrar e de ser empurrado.
        getPhysical().addForce("morte", 0, 0);
        // A CAMERA FECHA. Ela ja tem suavizacao propria — persegue o zoom desejado
        // um oitavo por quadro — entao basta dizer para onde ir e a aproximacao
        // sai contínua de graca, no mesmo ritmo do resto do jogo.
        Game.getCam().aproximar(1 + (APROXIMACAO - 1) * (float) Math.min(1,
                morrendo / (double) (MORTE_PARADA + MORTE_TOMBO)));
        if (morrendo >= MORTE_TOTAL) {
            disappear();
        }
    }

    /**
     * A cara durante a queda: os olhos fecham em DOIS TEMPOS.
     *
     * Semicerrados primeiro, fechados depois, cada etapa ocupando metade da parada
     * inicial. Fechar de uma vez le como piscar; fechar em dois degraus, devagar,
     * le como perder a consciencia — que e o que o momento pede.
     */
    private Expressao.Cara caraDaMorte() {
        // OS OLHOS COMECAM ABERTOS, e este e o ponto.
        //
        // Antes a cara de olho cerrado valia desde o primeiro quadro da queda. O
        // resultado era um gato JA de olho fechado — e olho que ja nasce fechado
        // nunca e visto fechando. O fechamento nao e um estado, e uma TROCA: e
        // preciso ver o antes para perceber o depois.
        //
        // Entao a queda comeca com o gato de olhos abertos, parado, levando um
        // terco da pausa inicial. So depois eles se fecham, e ai a mudanca
        // acontece na frente de quem esta olhando.
        if (morrendo < MORTE_PARADA / 3) {
            return Expressao.Cara.NORMAL;
        }
        return Expressao.Cara.MACHUCADO;
    }

    /** De 0 (em pe) a 1 (deitado), com desaceleracao no fim do tombo. */
    private double tombo() {
        if (morrendo < MORTE_PARADA) {
            return 0;
        }
        double t = Math.min(1, (morrendo - MORTE_PARADA) / (double) MORTE_TOMBO);
        return 1 - Math.pow(1 - t, 3);
    }

    /** De 0 (ainda inteiro) a 1 (totalmente apagado). */
    private float apagamento() {
        int apagando = morrendo - MORTE_PARADA - MORTE_TOMBO;
        if (apagando <= 0) {
            return 0f;
        }
        return Math.min(1f, apagando / (float) MORTE_APAGAR);
    }

    /** De 1 (visivel) a 0 (apagado). */
    private float restoDeVida() {
        return 1f - apagamento();
    }

    /**
     * O quanto o corpo ja perdeu a cor.
     *
     * ELE CLAREIA ANTES DE SUMIR, e nao so fica transparente.
     *
     * Transparencia sozinha le como um desenho sendo APAGADO — a arte vai ficando
     * fraca, com as mesmas cores, ate acabar. E o que acontece quando se fecha uma
     * janela, nao quando alguem morre. Clarear e outra coisa: a silhueta perde a
     * identidade primeiro e so depois some, entao o que se ve nao e uma imagem
     * sendo removida, e um corpo virando luz.
     *
     * A CURVA NAO E A MESMA DA TRANSPARENCIA, e isso importa. O branco corre na
     * frente — chega ao maximo na metade do desaparecimento — porque as duas
     * andando juntas se cancelam: branco a vinte por cento sobre um corpo a vinte
     * por cento de opacidade e quase nada em cima de quase nada. Clareando antes, o
     * gato fica todo branco enquanto ainda da para ve-lo, e e esse quadro que
     * carrega o momento.
     */
    private float perdaDeCor() {
        return Math.min(1f, apagamento() * 2f);
    }

    private void renderMorte(Graphics2D g) {
        BufferedImage sprite = Expressao.reacao(getSprite(), caraDaMorte(), perdaDeCor());
        if (ladoDoPasso == 1) {
            sprite = SpriteHandler.flip(sprite, 1, -1);
        }
        BufferedImage apagado = Alpha.getImage(sprite, restoDeVida());
        // Gira em volta do PE do desenho, e nao do meio: tombo e queda apoiada no
        // chao. Girando pelo centro o gato subiria enquanto cai, que e o oposto.
        int x = ((int) getX() + (getWidth() - apagado.getWidth()) / 2);
        int y = ((int) getY() - apagado.getHeight() + getHeight());
        Point pe = new Point(apagado.getWidth() / 2, apagado.getHeight() - Configs.GameScale());
        Rotate.draw(apagado, x, y, tombo() * Math.PI / 2 * (ladoDoPasso == 1 ? -1 : 1), pe, g);
    }

    /** Levou pancada. */
    public void reagirAoDano() {
        this.doendo = DOR;
    }

    /**
     * Acertou alguem.
     *
     * A duracao vem DA ARMA NA MAO, e nao de um numero fixo aqui: garra e machado
     * tem ritmos que diferem em seis vezes, e uma so duracao nao acompanha os dois.
     */
    public void reagirAoGolpe() {
        Item naMao = getInventory().getItemHand();
        this.golpeando = naMao == null ? GOLPE : naMao.duracaoDaReacao();
    }

    /**
     * Qual cara esta no rosto agora.
     *
     * A DOR GANHA DO GOLPE quando as duas acontecem juntas — e acontecem o tempo
     * todo, porque trocar golpe e o normal de uma briga. Entre "acertei" e "fui
     * atingido", quem o jogador precisa ver e a segunda: ela e a que custa vida.
     */
    /** Força do clarao agora, de 1 a 0 nos primeiros quadros da dor. */
    private float clarao() {
        int desde = DOR - doendo;
        if (doendo <= 0 || desde >= CLARAO) {
            return 0f;
        }
        return 1f - desde / (float) CLARAO;
    }

    private Expressao.Cara cara() {
        if (doendo > 0) {
            return Expressao.Cara.MACHUCADO;
        }
        if (golpeando > 0) {
            return Expressao.Cara.GOLPEANDO;
        }
        return Expressao.Cara.NORMAL;
    }

    private void contarReacoes() {
        if (desdeOGemido < ENTRE_GEMIDOS) {
            desdeOGemido++;
        }
        if (doendo > 0) {
            doendo--;
        }
        if (golpeando > 0) {
            golpeando--;
        }
    }

    public BufferedImage getSprite(int index) {
        getSheet().setIndex(index);
        return getSheet().getSprite();
    }

    @Override
    public void tick() {
        if (morrendo()) {
            tickMorte();
            return;
        }
        // CHEGANDO PELO PORTAL: nao anda, nao ataca. Mesmo trato da queda — cena em
        // que o jogador nao controla nada nao pode cobrar reacao dele.
        if (chegando()) {
            return;
        }
        contarReacoes();
        boolean andando = getPhysical().isMoving();
        int horizontal = getPhysical().getOrientation()[0];
        if (horizontal != 0) {
            ladoDoPasso = horizontal;
        }
        cochilar(andando);
        int estado = andando ? ANDANDO : (cochilando ? DORMINDO : PARADO);
        if (estado != estadoAnterior) {
            // Troca de animacao: folha, indice e contador voltam para o quadro
            // zero juntos. Sem isto o balanco entra fora de fase com as pernas.
            estadoAnterior = estado;
            countAnim = 0;
            quadroAtual = 0;
            getSheet().setState(estado);
            getSheet().setIndex(0);
        }
        countAnim++;
        if (countAnim >= TICKS_DO_ESTADO[estado]) {
            countAnim = 0;
            getSheet().plusIndex();
            quadroAtual++;      // o modulo fica em quem le, ver balancoDaCaminhada
            avancouQuadro = true;
        }
        if(!(Engine.getACTIVITY() instanceof Game))
            return;
        updateMovement();
        tickItemHand();
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        // QUEM JA ESTA CAINDO NAO LEVA MAIS DANO.
        //
        // Sem isto o gato continuava piscando de dor e gemendo durante a propria
        // queda, enquanto o chefe batia num corpo que ja acabou. Alem de ler mal,
        // e contraditorio: a barra ja chegou a zero e a partida ja terminou; o
        // dano seguinte nao muda nada e so estraga a cena.
        if (morrendo()) {
            return;
        }
        if (chegando()) {
            return;
        }
        if (hasModifier(Modifiers.Dodge)) {
            double percent = valueModifier(Modifiers.Dodge) + getLuck() * 0.10d;
            double a = Engine.RAND.nextDouble(1d);
            if (a <= percent) {
                Game.getMap().put(new Word("Dodge", getX() + getWidth() / 2d, getY() + getHeight() / 2d, 1));
                return;
            }
        }
        // O escudo come parte do golpe enquanto estiver na mao. Fica aqui, e
        // nao no item, porque quem apanha e o gato: o escudo nao precisa estar
        // do lado certo da orbita para valer, senao a defesa viraria sorteio.
        if (getInventory().getItemHand() instanceof Shield) {
            damage *= 1 - Shield.absorcao();
        }
        Sound.play(this.dor);
        // A careta so entra DEPOIS da esquiva: golpe desviado nao doeu, e fazer o
        // gato se encolher num ataque que ele evitou contaria a historia errada.
        reagirAoDano();
        super.strike(type, damage);
    }

    public double getLuck() {
        if(this.hasModifier(Modifiers.Luck)) {
            double l = this.luck + this.valueModifier(Modifiers.Luck);
            if(l < 0)
                l = 0;
            return l;
        }
        return luck;
    }

    public void setRangeOfColect(double range){
        this.rangeOfColect = range * GameObject.SIZE();
    }

    public double getRangeOfColect(){
        return this.rangeOfColect;
    }

    public List<Consumable> getPassives() {
        return List.copyOf(this.passives);
    }

    public void addPassive(Consumable passive) {
        if(this.passives.contains(passive)) {
            int i = this.passives.indexOf(passive);
            int stack = this.passives.get(i).getStack();
            this.passives.get(i).setStack(stack + 1);
        }else {
            this.passives.add((Consumable) Item.build(passive.getID(), 1));
        }
    }

    public void plusXp(double weight) {
        this.XP += weight;
        while(getXp() >= getXpLength()) { //caso ganhe um xp com peso muito alto, o nivel vai aumentar até o chegar no nivel considerado
            this.XP -= getXpLength();
            this.level++;
        }
    }

    public double getXp() {
        return this.XP;
    }

    public double getXpLength() {
        if(getLevel() == 0)
            return 75;
        return getLevel() * 23.74 + 100 * getLevel()*1.33;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        // Na vitrine o dinheiro não cai. A ideia ali é ver os vinte e um itens na
        // mão, e ficar sem moeda no meio disso só interrompe a revisão.
        if (Debugging.VITRINE && money < this.money) {
            return;
        }
        this.money = money;
    }

    public boolean takeLevel(double levles) {
        if(this.level <= levles) {
            this.level -= levles;
            return true;
        }
        return false;
    }

    public int getLevel() {
        return this.level;
    }

    /**
     * Solta um "Z" de vez em quando depois de um tempo parado.
     *
     * O mesmo truque do vendedor, e o mesmo motivo: e o que faz "parado" virar um
     * estado com leitura propria em vez de ausencia de movimento. Ver
     * ATE_COCHILAR para o porque da espera ser longa.
     */
    private void cochilar(boolean andando) {
        if (andando || !(Engine.getACTIVITY() instanceof Game)) {
            ticksParado = 0;
            ticksDoZ = 0;
            cochilando = false;
            return;
        }
        ticksParado++;
        if (ticksParado < ATE_COCHILAR) {
            return;
        }
        cochilando = true;
        ticksDoZ++;
        if (ticksDoZ < ENTRE_ZS) {
            return;
        }
        ticksDoZ = 0;
        // Sai de cima da cabeca, com um respingo horizontal para os Zs nao
        // subirem todos na mesma coluna.
        double x = getX() + getWidth() / 2d + Engine.RAND.nextInt(5) - 2;
        double y = getY() - getHeight() / 3d;
        String z = Engine.RAND.nextBoolean() ? "Z" : "z";
        Game.getMap().put(new Word(z, x, y, 1));
    }

    private void tickItemHand() {
        Item item = getInventory().getItemHand();
        if(item != null)
            item.tick();
    }

    private void updateMovement() {
        if (!(Engine.getACTIVITY() instanceof Game))
            return;
        int vertical = 0;
        int horizontal = 0;
        boolean isMoving = false;

        if (KeyBoard.KeyPressing("W") || KeyBoard.KeyPressing("Up")) {
            vertical = -1;
            isMoving = true;
        }
        if (KeyBoard.KeyPressing("S") || KeyBoard.KeyPressing("Down")) {
            vertical = 1;
            isMoving = true;
        }
        if (KeyBoard.KeyPressing("A") || KeyBoard.KeyPressing("Left")) {
            horizontal = -1;
            isMoving = true;
        }
        if (KeyBoard.KeyPressing("D") || KeyBoard.KeyPressing("Right")) {
            horizontal = 1;
            isMoving = true;
        }

        countDash++;
        if (KeyBoard.KeyPressed("SPACE")) {
            if (hasModifier(Modifiers.Dash) && countDash > 45) {
                dash = true;
                countDash = 0;
            }
        }
        if (isMoving) {
            double radians = Math.atan2(vertical, horizontal);
            if (dash) {
                getPhysical().addForce("dash", getSpeed() * valueModifier(Modifiers.Dash), radians);
                getPhysical().setDrag(0.6);
                dash = false;
            }
            getPhysical().addForce("move", getSpeed(), radians);
            // A poeira e o som nascem NO QUADRO DE CONTATO, que sao os pares do
            // ciclo — os dois em que os dois pes tocam o chao. E o unico instante
            // em que faz sentido levantar poeira, e da dois passos por volta, que
            // e o que o ciclo tem. Antes era um contador solto de periodo tres
            // sobre uma folha de sete, e o sopro caia num ponto qualquer da volta.
            if (avancouQuadro && quadroAtual % 2 == 0) {
                Game.getMap().put(new Poeira(
                        getX() + getWidth() / 2d, getY() + getHeight(), radians));
                Sound.play(Sounds.Walking);
            }
        }
        avancouQuadro = false;
    }

    /**
     * Esta saindo do portal de chegada da arena.
     *
     * PERGUNTA PELA ACTIVITY ANTES DE PEDIR O MAPA. Game.getMap lanca excecao fora
     * do jogo, e este metodo e chamado do tick e do render do gato — que tambem
     * rodam na TELA DE SELECAO DE PERSONAGEM, onde um Player e criado so para a
     * previa. A primeira versao derrubava o jogo ali, antes de a partida comecar.
     */
    public boolean chegando() {
        if (!(Engine.getACTIVITY() instanceof Game)) {
            return false;
        }
        return Game.getMap() instanceof com.retronova.game.map.arena.Arena arena
                && arena.chegando();
    }

    public void render(Graphics2D g) {
        if (morrendo()) {
            renderMorte(g);
            return;
        }
        if (chegando()) {
            com.retronova.game.map.arena.Arena arena =
                    (com.retronova.game.map.arena.Arena) Game.getMap();
            arena.renderChegada(g);
            float formado = arena.formacaoDoGato();
            if (formado <= 0f) {
                return;                      // o portal abriu; o gato ainda nao
            }
            // A ARMA CHEGA COM O GATO, e nao depois dele.
            //
            // Ela era desenhada so no caminho normal, entao o gato saia do portal
            // de maos vazias e a arma aparecia no quadro seguinte, do nada. Ela faz
            // parte da silhueta dele; chegar separada e o mesmo que chegar sem a
            // cauda. Passa pelo mesmo clarao, pela mesma razao — o que se forma se
            // forma inteiro.
            renderSprite(Expressao.clarao(getSprite(), 1f - formado), g);
            drawItem(g);
            return;
        }
        BufferedImage sprite = Expressao.reacao(getSprite(), cara(), clarao());
        // O RABO FICA ATRAS. A arte tem o rabo a direita, que e a pose de quem
        // anda para a esquerda — entao quem espelha e quem anda para a DIREITA.
        // Estava ao contrario: o rabo saia na frente do gato nos dois sentidos.
        if(ladoDoPasso == 1)
            sprite = SpriteHandler.flip(sprite, 1, -1);

        // O balanco da caminhada NAO esta aqui: ele e desenhado na propria folha,
        // pelo tools/GenGatos.java, que abaixa a cabeca no quadro de contato e a
        // levanta no de passada. Deslizando o sprite inteiro, como era antes, o
        // rabo e a pata apoiada subiam junto com o corpo — e eles ficam no chao.
        renderSprite(sprite, g);

        // A arma nao balanca junto: ela orbita o gato e tem ritmo proprio.
        drawItem(g);
    }

    /**
     * Onde o item fica, na grade de 16 do sprite.
     *
     * E a linha das PATAS DIANTEIRAS, na borda do corpo. Houve por um tempo uma
     * pata desenhada no flanco so para o item ter onde encostar; ela saiu junto
     * com o redesenho, porque o vendedor — que e a referencia da arte — nao tem
     * uma, e um pixel de pata no meio da lateral lia como mancha. O item encosta
     * na altura das maos, que e onde ele estaria de qualquer jeito.
     */
    private static final int MAO_X = 11, MAO_Y = 13;

    /**
     * Onde o item deve ser desenhado para parecer seguro, e nao flutuando ao lado.
     *
     * Acompanha o lado do passo, e nao o espelhamento do sprite: o rabo fica para
     * tras e a mao para a frente, entao os dois andam em sentidos opostos. Nao
     * precisa acompanhar o balanco da caminhada porque nao existe mais balanco de
     * tela — quem sobe e desce e a cabeca, dentro da propria folha, e a linha das
     * patas fica parada.
     */
    /** 1 se o gato anda para a direita, -1 para a esquerda. Espelha o item. */
    public int getLadoDoPasso() {
        return ladoDoPasso;
    }

    public Point getMao() {
        BufferedImage sprite = getSprite();
        int px = Configs.GameScale();
        int colunas = sprite.getWidth() / px;
        int x0 = (int) getX() + (getWidth() - sprite.getWidth()) / 2;
        int y0 = (int) getY() - sprite.getHeight() + getHeight();
        // A mao vai na FRENTE, ao contrario do rabo: o item tem de ficar entre o
        // gato e o que ele esta atacando, e nao escondido atras dele.
        int coluna = ladoDoPasso == 1 ? MAO_X : colunas - 1 - MAO_X;
        return new Point(x0 + coluna * px + px / 2, y0 + MAO_Y * px + px / 2);
    }

    private void drawItem(Graphics2D g) {
        Item item = getInventory().getItemHand();
        if(item == null)
            return;
        item.render(g);
    }

    public String[] getInfo() {
        return new String[] {
                "HP: " + getLife(),
                "Attack: " + getDamage(),
                "Speed: " + getSpeedDescription(),
                "Luck: " + getLuck(),
                "Atk Speed: " + getAttackSpeed(),
                "Range: " + getRange(),
                "Bag Size: " + getInventory().getBagSize(),
                "Hotbar Size: " + getInventory().getHotbarSize()

        };
    }

    private String getSpeedDescription() {
        double speed = getSpeed();
        if (speed < 0.3) {
            return "Slow";
        } else if (speed < 0.5) {
            return "Normal";
        } else {
            return "Fast";
        }
    }
}