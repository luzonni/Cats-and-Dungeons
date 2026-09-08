package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.exceptions.NotFound;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteHandler;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.utilities.Projetil;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public abstract class Item {

    private static final Map<String, BufferedImage[]> sheet;

    static {
        sheet = new HashMap<>();
    }

    /**
     * As armas elementais, montadas a partir do nome do proprio ID.
     *
     * SwordIce vira {@code new Sword(id, Elemento.GELO)}. Uma tabela em vez de
     * ramos soltos: acrescentar um elemento novo passa a ser uma linha em
     * Elemento, uma linha em ItemIDs e nada aqui.
     */
    private static Item variante(ItemIDs type, int id) {
        // SwordFire e a excecao: o nome dela termina em "Fire" e cairia nesta
        // tabela, mas a arte dela e swordfire.png, feita a mao antes desta
        // esteira e sem o sublinhado das geradas. Fica com o ramo antigo.
        if (type == ItemIDs.SwordFire) {
            return null;
        }
        for (Elemento e : Elemento.values()) {
            if (e == Elemento.NENHUM) {
                continue;
            }
            String nome = type.name();
            if (!nome.endsWith(sufixoDoId(e))) {
                continue;
            }
            String base = nome.substring(0, nome.length() - sufixoDoId(e).length());
            switch (base) {
                case "Sword" -> { return new Sword(id, e); }
                case "Axe" -> { return new BloodyAxe(id, e); }
                case "Bow" -> { return new Bow(id, e); }
                case "Wand" -> { return new Wand(id, e); }
                default -> { return null; }
            }
        }
        return null;
    }

    /** Como o elemento aparece no fim do nome do ID: FOGO -> "Fire". */
    private static String sufixoDoId(Elemento e) {
        String s = e.sprite("");            // "_fire"
        return Character.toUpperCase(s.charAt(1)) + s.substring(2);
    }

    /** Sprites ja tingidos, por (imagem, cor). Tingir varre pixel a pixel. */
    private static final Map<String, BufferedImage> TINGIDOS = new HashMap<>();

    /**
     * O mesmo desenho na cor do elemento.
     *
     * Serve para o RASTRO do golpe: o corte da espada e um sprite branco, e
     * branco e a unica cor que aceita ser multiplicada por qualquer outra sem
     * sujar. Assim a espada de fogo corta laranja e a de gelo corta azul sem
     * precisar de um PNG de rastro por elemento.
     *
     * O alfa e preservado — tingir tem de mudar a cor, nao a silhueta.
     */
    protected static BufferedImage tingir(BufferedImage fonte, Color cor) {
        String chave = System.identityHashCode(fonte) + ":" + cor.getRGB();
        BufferedImage cache = TINGIDOS.get(chave);
        if (cache != null) {
            return cache;
        }
        BufferedImage o = new BufferedImage(fonte.getWidth(), fonte.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < fonte.getHeight(); y++) {
            for (int x = 0; x < fonte.getWidth(); x++) {
                int p = fonte.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) {
                    continue;
                }
                int r = ((p >> 16) & 0xFF) * cor.getRed() / 255;
                int g = ((p >> 8) & 0xFF) * cor.getGreen() / 255;
                int b = (p & 0xFF) * cor.getBlue() / 255;
                o.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        TINGIDOS.put(chave, o);
        return o;
    }

    public static Item build(int id, Object... values) {
        ItemIDs type = ItemIDs.values()[id];
        int stack = 1;
        if(values.length >= 1) {
            stack = (int)values[0];
        }
        // As variantes elementais entram ANTES do switch: sao vinte e tres casos
        // que so mudam de elemento, e escrever vinte e tres ramos identicos seria
        // vinte e tres lugares para errar um sprite ou um tipo de dano.
        Item variante = variante(type, id);
        if (variante != null) {
            return variante;
        }
        switch (type) {
            case Axe -> {
                return BloodyAxe.comum(id);
            }
            case Silk -> {
                return new ItemSilk(id);
            }
            case Sword -> {
                return new Sword(id);
            }
            case Bow -> {
                return new Bow(id);
            }
            case Bomb -> {
                return new ItemBomb(id);
            }
            case Feed -> {
                Consumable consumable = new Feed(id);
                consumable.setStack(stack);
                return consumable;
            }
            case Catnip -> {
                Consumable consumable = new Catnip(id);
                consumable.setStack(stack);
                return consumable;
            }
            case Laser -> {
                return new Laser(id);
            }
            case Acorn -> {
                Consumable consumable = new Acorn(id);
                consumable.setStack(stack);
                return consumable;
            }
            case Watermelon -> {
                Consumable consumable = new Watermelon(id);
                consumable.setStack(stack);
                return consumable;
            }
            case Wand -> {
                return new Wand(id);
            }
            case Kunai -> {
                return new Kunai(id);
            }
            case ClawBlades -> {
                return new ClawBlades(id);
            }
            case GasBomb -> {
                return new GasBomb(id);
            }
            case BloodyAxe -> {
                return new BloodyAxe(id);
            }
            // O ImA (MagneticOrb) foi retirado do jogo. A constante do enum FICA:
            // o ordinal dela e o identificador gravado nos JSON de mapa e de
            // personagem, e tirar do meio renumeraria todos os itens seguintes.
            case MagneticOrb -> {
                Consumable consumable = new MagneticOrb(id);
                consumable.setStack(stack);
                return consumable;
            }
            case BowEletric -> {
                return new BowElectric(id);
            }
            case SwordFire -> {
                return new SwordFire(id);
            }
            case DangerousWand -> {
                // O ID continua DangerousWand porque o nome esta gravado nos
                // JSON de mapa e de personagem; o item virou escudo. Ver Shield.
                return new Shield(id);
            }
            case Trident -> {
                return new Trident(id);
            }
            case Sickle -> {
                return new Sickle(id);
            }
            case Furball -> {
                return new Furball(id);
            }
        }
        throw new NotFound("Item not found");
    }

    private final int id;
    private final String name;
    /** O nome do PNG, que e a chave da pose ajustada no editor. Ver Poses. */
    private final String nomeDoSprite;
    private int indexSprite;

    private final BufferedImage[] sprite;
    private String[] specifications;

     Item(int id, String name, String sprite) {
        this.id = id;
        this.name = name;
        this.nomeDoSprite = sprite;
        if(!sheet.containsKey(sprite)) {
            SpriteHandler sheet = new SpriteHandler("sprites/items", sprite, Configs.GameScale());
            int length = sheet.getWidth() / 16;
            this.sprite = new BufferedImage[length];
            for (int i = 0; i < length; i++) {
                this.sprite[i] = sheet.getSpriteWithIndex(i, 0);
            }
        }else {
            this.sprite = sheet.get(sprite);
        }
        addSpecifications("espec");
    }

    protected void addSpecifications(String... specifications) {
         this.specifications = specifications;
    }

    public String[] getSpecifications() {
         return this.specifications;
    }

    public int getID() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public boolean stackable() {
         return this instanceof Consumable;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj instanceof Item item) {
            return this.getID() == item.getID();
        }
        return false;
    }

    public BufferedImage getSprite() {
        return this.sprite[indexSprite];
    }

    protected void plusIndexSprite() {
        this.indexSprite++;
        if(indexSprite > sprite.length-1) {
            indexSprite = 0;
        }
    }

    /**
     * Escolhe o quadro, dando a volta se o indice passar do fim.
     *
     * Era um laco que subtraia {@code length-1} ate caber, e com UM quadro so
     * isso e subtrair zero para sempre — o jogo travava ao criar o item, sem
     * erro nenhum no console. Apareceu quando a espada deixou de ter quatro
     * variantes; e como a espada esta no inventario inicial do Muffin, travava
     * logo ao entrar na partida. O resto tambem estava errado: subtrair
     * repetidamente distorce o sorteio em vez de distribuir.
     */
    protected void setIndexSprite(int index) {
        this.indexSprite = sprite.length == 0 ? 0 : Math.floorMod(index, sprite.length);
    }

    protected void resetIndexSprite() {
        this.indexSprite = 0;
    }

    public abstract void tick();

    /**
     * Desenha o item na mão do gato.
     *
     * Deixou de ser abstrato porque metade dos itens não implementava nada: a
     * varinha, a bomba, a seda, a bola de pelo e todos os consumíveis tinham o
     * método vazio e simplesmente não apareciam. Pegar a varinha como Azrael e
     * não ver arma nenhuma era o caso mais visível.
     *
     * Quem tem movimento próprio — espada que gira, arco que estica, kunai que
     * mira — continua sobrescrevendo. Quem não tem ganha o desenho parado, que é
     * melhor do que não existir.
     */
    public void render(Graphics2D g) {
        naMao(g, getSprite());
    }

    /**
     * Como o item é carregado quando não está atacando.
     *
     * Os três números são o quanto ele inclina, em graus, e o deslocamento em
     * pixels de arte a partir da mão. Todos são espelhados quando o gato anda
     * para o outro lado.
     */
    public enum Porte {
        /** Uma mão, ao lado do corpo, ponta um pouco para fora. */
        UMA_MAO(25, 2, 0),
        /**
         * Duas mãos, atravessada à frente do peito.
         *
         * Mais deitada e mais baixa que a de uma mão: é o que faz um machado
         * parecer pesado. A referência de animação diz a mesma coisa por outro
         * caminho — arma pesada se lê pelo corpo inteiro comprometido com ela,
         * não por ser maior.
         */
        DUAS_MAOS(62, 2, 1),
        /**
         * Cajado e varinha: em pé, ADIANTADA em relação ao corpo.
         *
         * O empurrão para a frente não é enfeite. Apoiada junto ao corpo ela
         * cobria metade do gato; adiantada, sai da frente dele e ainda lê como
         * quem estende a varinha para lançar.
         */
        CAJADO(8, 3, -1),
        /** Miúdo — frasco, bomba, comida: junto à mão, sem inclinação. */
        MIUDO(0, 1, 0);

        final int graus, dx, dy;

        Porte(int graus, int dx, int dy) {
            this.graus = graus;
            this.dx = dx;
            this.dy = dy;
        }
    }

    /**
     * O item está no meio de um ataque.
     *
     * Quem tem animação de ataque marca isto no tick. Enquanto está falso, o
     * desenho é o de porte — assim a pose parada é a mesma regra para todos, e
     * cada arma só cuida do próprio golpe.
     */
    protected boolean atacando;

    /**
     * Um golpe em quatro fases, contadas em ticks.
     *
     * É a estrutura que a referência de animação descreve, e o que separa um
     * golpe que "bate" de um que só troca de sprite:
     *
     *   PREPARO       a arma recua. Quanto mais longo, mais pesada ela parece —
     *                 e é a única coisa que avisa o jogador que o golpe vem.
     *   CORTE         o deslocamento rápido. É aqui que o dano sai.
     *   EXTENSÃO      a arma segue ALÉM do alvo. Sem isso o golpe parece
     *                 cortado no quadro do impacto, que é o defeito mais comum.
     *   RECUPERAÇÃO   volta à pose de porte.
     *
     * Os tempos vêm da mesma referência, convertidos de milissegundos para os
     * 60 ticks por segundo do jogo: leve fecha em 400 ms, lança em 550, e arma
     * pesada em 800 — com o preparo e a extensão levando quase tudo.
     */
    protected static final class Investida {

        private final int preparo, corte, extensao, recuperacao;
        private int t = -1;

        protected Investida(int preparo, int corte, int extensao, int recuperacao) {
            this.preparo = preparo;
            this.corte = corte;
            this.extensao = extensao;
            this.recuperacao = recuperacao;
        }

        /**
         * Rapida: 170 ms. Garras e laminas curtas.
         *
         * O preparo cabe em tres ticks e a recuperacao em dois — quase nada. E o
         * que separa uma arma de assassino de uma espada: a espada ganha peso
         * SEGURANDO os quadros, e estas ganham ameaca por nao segurar nenhum. A
         * proporcao entre as quatro fases e a mesma da leve, so comprimida.
         */
        protected static Investida rapida() {
            return new Investida(3, 2, 3, 2);
        }

        /** Leve: 400 ms. Espada, faca, foice. */
        protected static Investida leve() {
            return new Investida(6, 3, 6, 3);
        }

        /** Média: 550 ms. Lança e haste. */
        protected static Investida media() {
            return new Investida(12, 3, 9, 9);
        }

        /** Pesada: 800 ms, quase metade só de preparo e extensão. */
        protected static Investida pesada() {
            return new Investida(15, 3, 18, 12);
        }

        protected int total() {
            return preparo + corte + extensao + recuperacao;
        }

        protected void comecar() {
            if (t < 0) {
                t = 0;
            }
        }

        protected void tick() {
            if (t >= 0 && ++t >= total()) {
                t = -1;
            }
        }

        protected boolean ativa() {
            return t >= 0;
        }

        /** O quadro exato do impacto: um só, para o dano não sair repetido. */
        protected boolean acertaAgora() {
            return t == preparo + corte;
        }

        /**
         * Onde a arma está, de -1 (recuo máximo) a +1 (extensão máxima).
         *
         * O recuo é lento e a ida é rápida, que é o que dá o peso: a mesma
         * distância percorrida em três ticks depois de quinze de preparo.
         */
        protected double avanco() {
            if (t < 0) {
                return 0;
            }
            if (t < preparo) {
                // Recua devagar, desacelerando no fim do preparo.
                double f = t / (double) preparo;
                return -Math.sin(f * Math.PI / 2);
            }
            if (t < preparo + corte) {
                double f = (t - preparo) / (double) corte;
                return -1 + 2 * f;
            }
            if (t < preparo + corte + extensao) {
                double f = (t - preparo - corte) / (double) extensao;
                return 1 - 0.15 * f;         // segue adiante, quase parado
            }
            double f = (t - preparo - corte - extensao) / (double) Math.max(1, recuperacao);
            return 0.85 * (1 - f);
        }
    }

    /** Como este item é carregado. Miúdo por padrão. */
    protected Porte porte() {
        return Porte.MIUDO;
    }

    /**
     * O PUNHO do sprite: onde a mão do gato o segura.
     *
     * Fica no meio de baixo porque é ali que {@code tools/GenItens.java} assenta
     * toda arma — centrada na horizontal, apoiada no fundo do quadro.
     *
     * Isto existe num lugar só de propósito. Antes cada arma trazia o seu, todos
     * copiados como {@code (3, 12)}, que era o canto de baixo à esquerda de
     * quando as armas eram desenhadas na diagonal. Com as armas em pé aquele
     * ponto virou o vazio ao lado do cabo, e a arma girava por fora da mão —
     * exatamente o defeito que a literatura de sprite descreve: girar pelo centro
     * do sprite em vez de pelo punho separa a arma da mão.
     */
    /**
     * O punho, onde a mao do gato encosta.
     *
     * Em arma DESENHADA EM PE o punho e o meio da base do quadro. Em arma
     * diagonal nao: o cabo desce para o canto inferior ESQUERDO, e usar o meio da
     * base ancorava a arma cinco pixels ao lado do cabo — era isso que metia a
     * espada e a varinha para dentro do gato. Aqui o ponto e procurado: o pixel
     * opaco mais proximo do canto de baixo a esquerda.
     */
    private Point punhoDiagonal(BufferedImage sprite, int lado) {
        int melhorX = sprite.getWidth() / 2, melhorY = sprite.getHeight() - 1;
        int melhor = Integer.MAX_VALUE;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) <= 16) {
                    continue;
                }
                // De baixo para cima sempre; na horizontal, do lado do cabo.
                //
                // O CABO FICA A DIREITA. Foi medido, nao suposto: nos pacotes de
                // icone a arma e desenhada com a ponta em cima a esquerda e o
                // punho embaixo a direita. Procurando a esquerda, como estava, o
                // ponto de apoio caia na PONTA DA LAMINA — e a arma inteira ficava
                // pendurada pela ponta, longe da mao. Espelhada, o cabo troca de
                // lado junto com o desenho.
                int dx = lado == -1 ? x : sprite.getWidth() - 1 - x;
                int custo = dx + (sprite.getHeight() - 1 - y);
                if (custo < melhor) {
                    melhor = custo;
                    melhorX = x;
                    melhorY = y;
                }
            }
        }
        return new Point(melhorX, melhorY);
    }

    protected Point empunhadura(BufferedImage sprite) {
        return empunhadura(sprite, 1);
    }

    /**
     * O punho segundo a pose ajustada no editor, ou o calculado quando nao ha.
     *
     * Serve para os golpes que NAO passam por naMao — a varredura da espada tem
     * deslocamento e giro proprios, mas o ponto onde a mao segura e o mesmo. Sem
     * isto, mexer no punho no editor mudava a arma parada e nao mudava o golpe, e
     * a previa deixava de valer justo na hora que interessa.
     */
    /**
     * Onde o tiro nasce: a PONTA da arma, achada sozinha.
     *
     * O PADRAO É ESTE — um ponto preso à arma, que gira junto com ela, e não um
     * deslocamento fixo em relação ao personagem. É o "muzzle" das engines: um
     * marcador na ponta do cano cuja transformada acompanha a do desenho.
     *
     * Achar em vez de perguntar quer dizer que arma nova já nasce certa. Se a pose
     * trouxer um ponto marcado à mão, ele ganha — para o caso raro em que o desenho
     * engana.
     *
     * A PROJEÇÃO É FEITA NO ESPAÇO DO DESENHO, e isso não é detalhe. A primeira
     * versão girava o pixel e projetava no eixo já girado, o que em álgebra se
     * cancela: {@code (ox·c − oy·s)·c + (ox·s + oy·c)·s} é exatamente {@code ox}.
     * Ou seja, ela devolvia sempre o pixel mais à direita do arquivo,
     * independentemente da rotação — no cajado, o pé dele. Aqui a direção de busca
     * é a que o desenho aponta em repouso, que é o oposto do {@code facing}.
     *
     * @param ancoraX,ancoraY onde o MIOLO do desenho foi posto na tela
     * @param mira            para onde a arma aponta
     * @param facing          quanto o desenho já está girado em repouso
     */
    protected java.awt.geom.Point2D.Double boca(BufferedImage sprite, double ancoraX,
                                                double ancoraY, double mira, double facing) {
        java.awt.Point miolo = Rotate.centroDoDesenho(sprite);
        double giro = mira + facing;
        double c = Math.cos(giro), s = Math.sin(giro);
        Poses.Pose p = pose();
        double bx, by;
        if (p != null && p.bocaX() >= 0) {
            int px = Configs.GameScale();
            bx = p.bocaX() * px + px / 2.0 - miolo.x;
            by = p.bocaY() * px + px / 2.0 - miolo.y;
        } else {
            java.awt.Point ponta = pontaDoDesenho(sprite, -facing);
            bx = ponta.x - miolo.x;
            by = ponta.y - miolo.y;
        }
        return new java.awt.geom.Point2D.Double(
                ancoraX + bx * c - by * s, ancoraY + bx * s + by * c);
    }

    /**
     * O pixel opaco mais avançado numa direção, no espaço do desenho.
     *
     * @param direcao para onde é "a frente" dentro do arquivo, em radianos
     */
    protected static java.awt.Point pontaDoDesenho(BufferedImage sprite, double direcao) {
        double dx = Math.cos(direcao), dy = Math.sin(direcao);
        double frente = -Double.MAX_VALUE;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) > 16) {
                    frente = Math.max(frente, x * dx + y * dy);
                }
            }
        }
        // A BOCA E O MEIO DA FACE DA FRENTE, e nao um pixel qualquer dela.
        //
        // Duas tentativas antes disto falharam. A primeira pegava o ultimo pixel
        // varrido da coluna mais avancada, que numa pistola e a quina de baixo do
        // cano. A segunda desempatava pelo centro da caixa do desenho — so que a
        // COTONHA puxa esse centro para baixo, e a boca continuava um pixel abaixo
        // do cano. Tirando a media dos pixels que estao na frente, o ponto cai no
        // meio da abertura, que e onde o tiro sai de verdade.
        double somaX = 0, somaY = 0;
        int n = 0;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) <= 16) {
                    continue;
                }
                if (x * dx + y * dy >= frente - Configs.GameScale()) {
                    somaX += x;
                    somaY += y;
                    n++;
                }
            }
        }
        if (n == 0) {
            return new java.awt.Point(sprite.getWidth() / 2, sprite.getHeight() / 2);
        }
        return new java.awt.Point((int) Math.round(somaX / n), (int) Math.round(somaY / n));
    }

    /** O pixel opaco mais distante de um ponto — para armas seguradas, a ponta. */
    protected static java.awt.Point maisLongeDe(BufferedImage sprite, java.awt.Point de) {
        double melhor = -1;
        java.awt.Point achado = new java.awt.Point(sprite.getWidth() / 2, 0);
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) <= 16) {
                    continue;
                }
                double d = (x - de.x) * (x - de.x) + (y - de.y) * (y - de.y);
                if (d > melhor) {
                    melhor = d;
                    achado = new java.awt.Point(x, y);
                }
            }
        }
        return achado;
    }

    /**
     * O inimigo mais proximo que esta ao alcance E na linha de visao.
     *
     * UM LUGAR SO PARA ESSA DECISAO. Antes cada arma chamava getNearest por conta
     * propria, e getNearest so sabe de distancia — nao de parede. O resultado era
     * o que se via jogando: a arma mirava e atirava contra o bloco enquanto o
     * inimigo passeava do outro lado, e a animacao de ataque rodava sem parar
     * contra um alvo impossivel. Quatro armas de longo alcance nunca tiveram esse
     * teste; as outras tinham copias parecidas, cada uma com o seu jeito.
     *
     * Vale tambem para corpo a corpo: golpe de tres tiles atravessando parede e o
     * mesmo defeito, so que mais curto.
     */
    protected static Enemy alvoVisivel(Player jogador, double alcanceEmTiles) {
        return alvoAlcancavel(jogador, alcanceEmTiles);
    }

    /**
     * O inimigo mais proximo QUE DA PARA ACERTAR.
     *
     * A diferenca para "o mais proximo, e depois checar se da" e o defeito que o
     * gato mostrava parado no canto com o ZZZ de sono: se o vizinho mais perto
     * estava atras de um bloco, a arma desistia e ficava mirando nele para sempre,
     * mesmo com outro inimigo a descoberto do lado. Escolher ja considerando o
     * caminho e o que faz a arma trocar de alvo em vez de travar.
     */
    protected static Enemy alvoAlcancavel(Player jogador, double alcanceEmTiles) {
        return alvoAlcancavel(jogador, alcanceEmTiles,
                jogador.getX() + jogador.getWidth() / 2d,
                jogador.getY() + jogador.getHeight() / 2d);
    }

    /**
     * O mesmo, mas com a linha medida DE ONDE O TIRO NASCE.
     *
     * Isso importa exatamente no caso que o jogador descreveu como "num canto
     * assim": a boca da arma fica a meio corpo do gato, entao encostado numa quina
     * ela pode estar do OUTRO LADO do bloco. Medir do centro do bicho aprovava um
     * caminho que o projetil nao tinha, e o tiro morria no primeiro tick, colado
     * na arma. Quem atira e a boca; e dela que a pergunta tem de ser feita.
     */
    protected static Enemy alvoAlcancavel(Player jogador, double alcanceEmTiles,
                                          double origemX, double origemY) {
        double limite = GameObject.SIZE() * alcanceEmTiles;
        Enemy escolhido = null;
        double menor = Double.MAX_VALUE;
        List<Enemy> inimigos = Game.getMap().getEntities(Enemy.class);
        for (int i = 0; i < inimigos.size(); i++) {
            Enemy e = inimigos.get(i);
            double d = e.getDistance(jogador);
            if (d >= limite || d >= menor || !podeAcertar(origemX, origemY, e)) {
                continue;
            }
            menor = d;
            escolhido = e;
        }
        return escolhido;
    }

    /**
     * O inimigo mais proximo, HAJA PAREDE OU NAO. Serve para APONTAR.
     *
     * Mirar e atirar sao duas decisoes, e junta-las dava um defeito visivel: com o
     * alvo atras de um bloco, a arma nao recebia alvo nenhum e CONGELAVA no ultimo
     * angulo — a kunai ficava virada para o proprio gato enquanto o inimigo andava
     * na frente dele. Uma arma aponta para onde esta a ameaca; o que a parede
     * impede e o tiro sair, nao o cano acompanhar.
     */
    protected static Enemy alvoParaMirar(Player jogador, double alcanceEmTiles) {
        return jogador.getNearest(alcanceEmTiles, Enemy.class);
    }

    /**
     * A direcao DE ONDE O TIRO NASCE ate o alvo.
     *
     * Nao e a mesma coisa que a direcao do gato ate o alvo, e a diferenca era um
     * defeito visivel: o projetil nascia na ponta da arma, a meio corpo de
     * distancia do bicho, mas voava no angulo medido a partir do CENTRO dele. As
     * duas retas ficam paralelas, afastadas por esse meio corpo — de perto o
     * inimigo e largo o bastante para absorver o desvio, de longe o tiro passa ao
     * lado e segue para tras dele.
     *
     * Quem atira de um cano deslocado tem de mirar do cano.
     */
    protected static double miraDe(double origemX, double origemY, Entity alvo) {
        return Math.atan2(alvo.getY() + alvo.getHeight() / 2d - origemY,
                alvo.getX() + alvo.getWidth() / 2d - origemX);
    }

    /** Ha caminho livre daqui ate o alvo? E o que libera o DISPARO. */
    protected static boolean podeAcertar(Player jogador, Entity alvo) {
        return podeAcertar(jogador.getX() + jogador.getWidth() / 2d,
                jogador.getY() + jogador.getHeight() / 2d, alvo);
    }

    /** Ha caminho de UM PONTO QUALQUER ate o alvo — normalmente, da boca da arma. */
    protected static boolean podeAcertar(double origemX, double origemY, Entity alvo) {
        if (alvo == null) {
            return false;
        }
        boolean livre = Game.getMap().caminhoDeTiro(origemX, origemY,
                alvo.getBounds(), Projetil.larguraEmPixels());
        anotarLinha(origemX, origemY, alvo, livre);
        return livre;
    }

    // ---------------------------------------------------------------- depuracao
    //
    // A ULTIMA LINHA DE TIRO PERGUNTADA, para poder ser DESENHADA.
    //
    // Ate aqui a discussao sobre "por que a arma atira se nao da para acertar" foi
    // feita olhando print e adivinhando qual bloco estava no meio. Com a linha na
    // tela — verde quando ha caminho, vermelha quando nao ha — a resposta passa a
    // ser visivel no quadro em que acontece. Aparece junto com as hitboxes, na
    // mesma tecla que ja liga elas.
    private static double linhaX1, linhaY1, linhaX2, linhaY2;
    private static boolean linhaLivre;
    private static boolean temLinha;

    private static void anotarLinha(double x1, double y1, Entity alvo, boolean livre) {
        linhaX1 = x1;
        linhaY1 = y1;
        linhaX2 = alvo.getX() + alvo.getWidth() / 2d;
        linhaY2 = alvo.getY() + alvo.getHeight() / 2d;
        linhaLivre = livre;
        temLinha = true;
    }

    /** Desenha a ultima linha de tiro perguntada. So com a depuracao ligada. */
    public static void renderLinhaDeTiro(java.awt.Graphics2D g) {
        if (!temLinha) {
            return;
        }
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setStroke(new java.awt.BasicStroke(Configs.GameScale() / 2f));
        g2.setColor(linhaLivre ? new java.awt.Color(80, 230, 120, 200)
                : new java.awt.Color(240, 80, 80, 200));
        g2.drawLine((int) linhaX1, (int) linhaY1, (int) linhaX2, (int) linhaY2);
        int r = Configs.GameScale();
        g2.fillOval((int) linhaX1 - r, (int) linhaY1 - r, r * 2, r * 2);
        g2.dispose();
        temLinha = false;
    }

    /** Distancia usada quando a arma ainda nao tem pose ajustada. */
    protected static final double ALCANCE_DA_MAO = 7;

    /**
     * Onde uma arma APONTADA e ancorada: adiantada na direcao da mira, com o
     * desvio lateral da pose.
     *
     * @param recuo quanto puxar de volta — o arco usa para o gesto de esticar
     */
    protected java.awt.geom.Point2D.Double ancoraDeMira(Player jogador, double mira,
                                                        double recuo) {
        Poses.Pose p = pose();
        int px = Configs.GameScale();
        // O dx DA POSE E A DISTANCIA INTEIRA, sem base somada.
        //
        // Eu tinha acrescentado uma base aqui para tirar o laser de dentro do
        // gato, e com isso empurrei para fora TODAS as armas de mira — os arcos
        // sairam do lugar onde o editor os tinha deixado. Quem estava errado era
        // um numero de uma pose, nao a regra: o ajuste certo foi no dx do laser.
        double dist = (p == null ? ALCANCE_DA_MAO : p.dx()) * px - recuo;
        double lateral = (p == null ? 0 : p.dy()) * px;
        double cx = jogador.getX() + jogador.getWidth() / 2d;
        double cy = jogador.getY() + jogador.getHeight() / 2d;
        return new java.awt.geom.Point2D.Double(
                cx + Math.cos(mira) * dist + Math.cos(mira + Math.PI / 2) * lateral,
                cy + Math.sin(mira) * dist + Math.sin(mira + Math.PI / 2) * lateral);
    }

    /** A pose ajustada no editor, ou null. */
    protected Poses.Pose pose() {
        return Poses.de(nomeDoSprite);
    }

    protected Point punhoAjustado(BufferedImage sprite) {
        Poses.Pose pose = Poses.de(nomeDoSprite);
        if (pose == null) {
            return empunhadura(sprite);
        }
        int px = Configs.GameScale();
        return new Point(pose.punhoX() * px + px / 2, pose.punhoY() * px + px / 2);
    }

    /**
     * @param lado para que lado o gato olha. O sprite chega aqui JA ESPELHADO
     *             quando ele olha para a esquerda, e espelhar joga o cabo do
     *             canto de baixo a esquerda para o de baixo a direita. Procurando
     *             sempre a esquerda, a arma era ancorada pela PONTA e saia voando
     *             para longe do gato — foi o que aconteceu com a espada e a
     *             varinha quando o gato virava.
     */
    protected Point empunhadura(BufferedImage sprite, int lado) {
        if (grausDaArte() != 0) {
            return punhoDiagonal(sprite, lado);
        }
        return new Point(sprite.getWidth() / 2, sprite.getHeight() - Configs.GameScale());
    }

    /** O sprite com o punho na mão do gato, na pose do porte. */
    protected void naMao(Graphics2D g, BufferedImage sprite) {
        naMao(g, sprite, 0, 0);
    }

    /**
     * A arma no meio do golpe, com o giro e o avanco vindos da pose.
     *
     * Os dois numeros eram fixos no codigo da arma. Passaram para a pose pelo
     * mesmo motivo que a posicao passou: quanto uma machadada gira e avanca e
     * coisa que se julga vendo, e cada desenho pede um valor diferente — uma
     * machadinha curta e um machado de duas laminas nao descrevem o mesmo arco.
     *
     * @param avanco de -1 (recuo maximo) a +1 (extensao), vindo da Investida
     */
    protected void naMaoGolpeando(Graphics2D g, BufferedImage sprite, double avanco) {
        Poses.Pose pose = Poses.de(nomeDoSprite);
        int graus = pose == null ? Poses.GOLPE_GRAUS : pose.golpeGraus();
        int px = pose == null ? Poses.GOLPE_PX : pose.golpePx();
        naMao(g, sprite, avanco * graus, avanco * px);
    }

    /**
     * O mesmo, com um giro e um avanço a mais — é por aqui que os golpes passam.
     *
     * Os extras são somados à pose de porte em vez de a substituírem: assim o
     * golpe parte de onde a arma estava, e não de uma posição inventada, que é o
     * que fazia a arma "pular" para outro lugar no primeiro quadro do ataque.
     */
    /**
     * Quantos graus a ARTE ja esta girada em repouso.
     *
     * As armas dos pacotes de icone vem desenhadas na diagonal, apontando para
     * cima e para a direita — quarenta e cinco graus ANTES da vertical. Para a
     * pose de porte, que foi calculada para arma em pe, valer de novo, e preciso
     * girar de volta: o valor e quanto somar, e nao onde a arte esta. Somando -45
     * em vez de +45 a arma ficava noventa graus fora, deitada e por dentro do
     * gato — que foi exatamente como a espada e a varinha apareceram.
     */
    protected double grausDaArte() {
        return 0;
    }

    protected void naMao(Graphics2D g, BufferedImage sprite, double grausExtra, double avancoPx) {
        if (sprite == null) {
            return;
        }
        Player jogador = Game.getPlayer();
        if (jogador == null) {
            return;
        }
        int px = Configs.GameScale();
        int lado = jogador.getLadoDoPasso();
        Poses.Pose ajuste = Poses.de(nomeDoSprite);
        // DOIS ESPELHOS, E UM SÓ FLIP. Um vem do gato: a arma vira com ele, senão
        // andar para a esquerda deixava o gume do machado apontando para trás. O
        // outro vem da pose, porque os pacotes desenham toda arma virada para o
        // mesmo lado e às vezes o desenho precisa nascer trocado. Aplicados juntos
        // eles se cancelam — por isso a conta é um "ou exclusivo", e não dois
        // espelhamentos em sequência.
        boolean virar = (lado == -1) ^ (ajuste != null && ajuste.espelhar());
        if (virar) {
            sprite = SpriteHandler.flip(sprite, 1, -1);
        }
        Point mao = jogador.getMao();
        // A pose ajustada a mão no editor tem prioridade sobre o Porte. Arma sem
        // linha no arquivo continua no padrão — ver Poses.
        Point punho;
        double graus;
        int dx, dy;
        if (ajuste != null) {
            int colunas = Math.max(1, sprite.getWidth() / px);
            int coluna = virar ? colunas - 1 - ajuste.punhoX() : ajuste.punhoX();
            punho = new Point(coluna * px + px / 2, ajuste.punhoY() * px + px / 2);
            graus = ajuste.graus();
            dx = ajuste.dx();
            dy = ajuste.dy();
        } else {
            punho = empunhadura(sprite, virar ? -1 : 1);
            Porte porte = porte();
            graus = porte.graus + grausDaArte();
            dx = porte.dx;
            dy = porte.dy;
        }
        int x = mao.x - punho.x + (int) ((dx + avancoPx) * px * lado);
        int y = mao.y - punho.y + dy * px;
        Rotate.draw(sprite, x, y, Math.toRadians((graus + grausExtra) * lado), punho, g);
    }

}
