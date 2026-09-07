package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.engine.io.Resources;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Inventory;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
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

    /** Quanto tempo de quietude ate o gato cochilar, e o intervalo entre os Zs. */
    private static final int ATE_COCHILAR = 60 * 5, ENTRE_ZS = 55;

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

    public BufferedImage getSprite(int index) {
        getSheet().setIndex(index);
        return getSheet().getSprite();
    }

    @Override
    public void tick() {
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
        if (hasModifier(Modifiers.Dodge)) {
            double percent = valueModifier(Modifiers.Dodge) + getLuck() * 0.10d;
            double a = Engine.RAND.nextDouble(1d);
            if (a <= percent) {
                Game.getMap().put(new Word("Dodge", getX() + getWidth() / 2d, getY() + getHeight() / 2d, 1));
                return;
            }
        }
        Sound.play(Sounds.DamageCat);
        super.strike(type, damage);
    }

    @Override
    public void die() {
        disappear();
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
     * estado com leitura propria em vez de ausencia de movimento. Cinco segundos
     * e tempo de o jogador ter mesmo largado o teclado — no meio de uma luta
     * ninguem fica tanto tempo sem andar, entao o gato nao vai cochilar na hora
     * errada.
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

    @Override
    public void render(Graphics2D g) {
        BufferedImage sprite = getSprite();
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