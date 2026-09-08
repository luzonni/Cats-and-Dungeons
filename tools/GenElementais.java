import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * As armas elementais, montadas a partir de pacotes CC0 baixados.
 *
 * CADA VARIANTE E UM DESENHO DIFERENTE, e nao a mesma arma pintada de outra cor.
 * A primeira versao deste gerador recolorava um sprite so, e o resultado era o que
 * o proprio jogador apontou: sete espadas identicas em sete cores, que nao dizem
 * nada. Os pacotes tem dezenas de silhuetas por familia — trinta modelos de
 * machado, trinta de cajado, nove de espada —, entao cada elemento leva um modelo
 * seu de verdade.
 *
 * FONTES (ver docs/TERCEIROS.md):
 *   Shade, "16x16 Assorted RPG Icons"  — espadas
 *   Shade, "16x16 Weapon RPG Icons"    — machados e cajados
 *   Bennyboi_hack, "16x16 weapon sprites free" — arcos
 *
 * O QUE O GERADOR MEXE. Tres coisas, para as armas de fora nao destoarem das de
 * dentro: o contorno vira o #222222 que todo o arsenal usa; o METAL e remapeado
 * para a rampa do elemento; e a MADEIRA do cabo e remapeada para os dois tons de
 * madeira que o jogo ja tinha. Metal e madeira sao separados pela saturacao — o
 * aco e cinza, o cabo e marrom —, e nao por posicao, que mudaria de sprite para
 * sprite.
 *
 * As rampas foram medidas na folha do Shade, que traz o mesmo desenho em oito
 * coloracoes: azul de gelo, laranja de fogo, dourado. Agua e ar nao existem la e
 * sao derivadas por matiz e saturacao, mantendo o mesmo desenho de sombra.
 *
 * Uso: java tools/GenElementais.java   (precisa dos pacotes em tools/assets/fonte)
 */
public class GenElementais {

    private static final String ITENS =
            "src/main/resources/com/retronova/resources/sprites/items/";
    private static final String FONTE = "tools/assets/fonte/";

    private static final int CONTORNO = 0x222222;

    /** Madeira do cabo, escuro -> claro. Os tons que o resto do arsenal usa. */
    private static final int[] MADEIRA = {0x5F3D21, 0x8F4029, 0xC56025};

    private static final Map<String, int[]> RAMPAS = new LinkedHashMap<>();
    static {
        RAMPAS.put("",       new int[]{0x2B2F3A, 0x414859, 0x7A8794, 0xB6CBCF, 0xFDF7ED});
        RAMPAS.put("fire",   new int[]{0x340900, 0x662412, 0xA86340, 0xDF864D, 0xFFBE6F});
        RAMPAS.put("ice",    new int[]{0x00312E, 0x195967, 0x3F8090, 0x85C4D4, 0xB3EEFF});
        RAMPAS.put("earth",  new int[]{0x412D1C, 0x5F3D21, 0x7E4E26, 0xB07E41, 0xFBBD5D});
        RAMPAS.put("legend", new int[]{0x4E2206, 0x693A0F, 0xB76C00, 0xFFD850, 0xFFF8C7});
        RAMPAS.put("water",  girar(RAMPAS.get("ice"), 0.58f, 1.15f, 1f));
        RAMPAS.put("air",    girar(new int[]{0x073427, 0x1F6951, 0x30875B, 0x66E68F, 0xCDFEE0},
                0f, 0.28f, 1.35f));

        // As tres abaixo nao sao elementos: sao a cor de armas especificas.
        // A espada COMUM e a do Muffin e sai preta, para casar com o gato preto
        // que a carrega; a varinha COMUM e a do Azrael e sai dourada, pela coroa
        // dele. Arma inicial que combina com o dono e a diferenca entre "o
        // personagem tem uma arma" e "aquela arma e dele".
        // Medidas em icons/Gato.png, a arte oficial: a lamina do Muffin da
        // #170D1E, e o orbe do Azrael da #816740 / #927348 / #E7D670. Nao sao
        // cores escolhidas — sao as que o desenhista usou.
        // PRETO, e nao roxo. A lamina na arte oficial e #170D1E, que tem um
        // fundo arroxeado; a 16 pixels, e com o brilho da rampa por cima, esse
        // roxo era a unica coisa que se via — a espada lia como roxa, nao como
        // preta. Os tons abaixo mantem a mesma escada de luz com a cor tirada.
        RAMPAS.put("dark",  new int[]{0x08080A, 0x121216, 0x1D1D23, 0x323239, 0x50505A});
        RAMPAS.put("gold",  new int[]{0x4A3A22, 0x816740, 0x927348, 0xC9B45E, 0xE7D670});
        // O machado de SANGUE tem de ser vermelho. Ele cura quem golpeia roubando
        // vida, e o nome anuncia isso; sair em aco cinza como os outros deixava a
        // unica arma com efeito proprio parecendo a mais comum de todas.
        RAMPAS.put("sangue", new int[]{0x2A0508, 0x5E0F16, 0xA51F28, 0xD9424A, 0xF7969B});
        // O laser NAO e recolorido de vermelho. Pintar o rifle inteiro de vermelho
        // comia o contraste entre cano, coronha e ferrolho, e o que sobrava era um
        // borrao rosa — parecia desenho gerado, e nao arma. Ele fica no aco do
        // pacote; o vermelho do laser quem da e o feixe, em tempo de jogo.
        RAMPAS.put("eletrico", new int[]{0x0A1A2E, 0x1B3A6B, 0x3B7FD4, 0x7FD4FF, 0xEAFBFF});
    }

    /** So estas viram flecha. As outras rampas sao cor de arma, nao elemento. */
    private static final String[] ELEMENTOS =
            {"fire", "ice", "water", "earth", "air", "legend"};

    /**
     * De onde sai cada sprite.
     *
     * @param saida   nome do PNG gerado
     * @param folha   qual pacote
     * @param col     coluna e linha da celula na folha
     * @param elem    chave da rampa; "" e a arma comum, em aco
     */
    private record Peca(String saida, String folha, int col, int lin, String elem,
                        String cabo, String truque) {
        Peca(String saida, String folha, int col, int lin, String elem) {
            this(saida, folha, col, lin, elem, null, null);
        }
        Peca(String saida, String folha, int col, int lin, String elem, String cabo) {
            this(saida, folha, col, lin, elem, cabo, null);
        }
    }

    private static final String ASSORTED = "shade/assorted/16x16 Assorted RPG Icons/weapons.png";
    private static final String STEEL = "shade/weapons/16x16 Weapons RPG Icons/steel-weapons.png";
    private static final String BENNY = "shade/weaponpack.png";
    /** Tiny gun icons, de congusbongus (CC0). Celulas de 16 com calha de 1 na horizontal. */
    private static final String GUNS = "guns/pack.png";
    /**
     * A mesma folha do Shade, na coloracao DOURADA.
     *
     * Sao quatro folhas — bronze, ferro, aco e ouro — e por muito tempo so a de
     * aco foi usada aqui. A dourada e arte lendaria pronta, feita pelo desenhista,
     * e nao a nossa rampa aplicada por cima: o brilho dela cai onde ele decidiu.
     */
    private static final String GOLD = "shade/weapons/16x16 Weapons RPG Icons/gold-weapons.png";

    /**
     * Duas varinhas de verdade, cada uma um arquivo solto de 16x16.
     *
     * Os tres pacotes de arma que o projeto ja usava nao tem cajado magico nenhum
     * — so espada, machado, maca e lanca. O que passava por varinha era um graveto
     * com losangos, e as elementais eram esse mesmo graveto repintado. Estas duas
     * vem prontas e ja na cor certa: a de agua e um orbe azul, a de fogo e uma
     * chama. Silhuetas DIFERENTES entre si, que era a queixa das familias antigas.
     */
    private static final String STAVE_AGUA = "db32/stave1.png";
    private static final String STAVE_FOGO = "db32/item_torch.png";
    /** Ninja Throwing Items Kit, de kungfu4000 (CC0). Um desenho por arquivo, em 32x32. */
    private static final String NINJA = "ninja/kunai.png";
    /**
     * Pixel Bow Pack: seis modelos de arco em seis coloracoes, celulas de 24.
     *
     * Chegou SEM arquivo de licenca — ver docs/TERCEIROS.md, onde isso esta
     * anotado como pendencia. O desenho e melhor que o que havia, entao entrou;
     * a origem precisa ser confirmada antes de qualquer distribuicao.
     */
    private static final String ARCOS = "arcos/bowpack.png";

    private static final Peca[] PECAS = {
            // ---- espadas: nove modelos na folha, um por elemento
            // A ESPADA DO MUFFIN, como esta na arte oficial: preta de ponta a
            // ponta — lamina E guarda —, com uma unica gema dourada no meio da
            // guarda. Por isso o cabo tambem sai "dark": deixa-lo marrom daria uma
            // espada preta com cabo de madeira, que nao e o desenho.
            new Peca("sword",         ASSORTED, 2, 1, "dark", "dark", "gema"),
            new Peca("swordfire",     ASSORTED, 2, 8, "fire"),
            new Peca("sword_ice",     ASSORTED, 2, 5, "ice"),
            new Peca("sword_water",   ASSORTED, 2, 3, "water"),
            new Peca("sword_earth",   ASSORTED, 2, 7, "earth"),
            new Peca("sword_air",     ASSORTED, 2, 0, "air"),
            // ---- machados
            // Dois machados: o COMUM, em aco, e o de SANGUE, em vermelho. Antes so
            // existia o bloody_axe fazendo os dois papeis.
            new Peca("axe",           STEEL,  0, 10, ""),
            new Peca("bloody_axe",    STEEL,  1, 13, "sangue"),
            new Peca("axe_fire",      STEEL,  2, 12, "fire"),
            new Peca("axe_ice",       STEEL,  2, 13, "ice"),
            new Peca("axe_water",     STEEL,  0, 15, "water"),
            new Peca("axe_earth",     STEEL,  2, 11, "earth"),
            new Peca("axe_air",       STEEL,  0, 14, "air"),
            // ---- cajados
            // O CAJADO DO AZRAEL e dourado INTEIRO na arte, haste e orbe. O cabo
            // sai dourado junto, senao sobraria uma haste de madeira no meio.
            // O cajado inicial vinha de 19,0 — um graveto reto com uma tampinha
            // em cada ponta, sem nada que desse leitura de "arma magica". Este e o
            // desenho da varinha de fogo, que tem um ornamento redondo em cima,
            // com a ponta DE BAIXO removida: sobra o cabo e o circulo, que e o
            // formato de cajado que a arte oficial mostra.
            new Peca("wand",          STEEL, 19,  4, "gold", "gold", "semPontaDeBaixo"),
            new Peca("wand_fire",     STAVE_FOGO, 0, 0, "", null, "cru"),
            new Peca("wand_ice",      STEEL, 20,  2, "ice"),
            new Peca("wand_water",    STAVE_AGUA, 0, 0, "", null, "cru"),
            new Peca("wand_earth",    STEEL, 20,  5, "earth"),
            new Peca("wand_air",      STEEL, 20,  3, "air"),
            // ---- arcos: seis modelos no pacote, o lendario repete o longbow
            new Peca("bow",           ARCOS,  1,  2, ""),
            // As celulas 4,3 e 5,3 sao meia-luas sem corda: a 16 pixels nao leem
            // como arco, leem como banana. Trocadas pelos modelos com corda.
            new Peca("bow_fire",      ARCOS,  1,  0, "fire"),
            new Peca("bow_ice",       ARCOS,  1,  1, "ice"),
            new Peca("bow_water",     ARCOS,  1,  3, "water"),
            new Peca("bow_earth",     ARCOS,  1,  4, "earth"),
            // Seis modelos para oito variantes: dois repetem desenho, em cores
            // bem distantes. Melhor que o pacote anterior, onde as repeticoes
            // eram quatro e dois dos desenhos nem tinham corda.
            new Peca("bow_air",       ARCOS,  1,  5, "air"),
            new Peca("bow_legend",    ARCOS,  1,  2, "legend"),
            new Peca("boweletric",    ARCOS,  1,  3, "eletrico"),
            // A kunai era uma faquinha EM PE, sem anel no punho e fora do estilo
            // diagonal do resto. Esta e uma adaga de punho anelado do Shade — o
            // anel e o que faz ler kunai e nao faca de cozinha.
            // ---- o laser: uma arma de cano, que e o que faltava. O sprite
            // antigo era o unico item que nao parecia do mesmo jogo.
            // O LASER E UMA ARMA DE ENERGIA, e nao um mosquete repintado. A peca
            // anterior era o rifle do Bennyboi tingido de vermelho: arte de artista,
            // sim, mas de polvora, e o vermelho por cima ainda comia o contraste.
            // Esta e um blaster de verdade, do pacote de armas do congusbongus.
            new Peca("laser",         GUNS,   0,  1, ""),
            // A kunai vem do kit ninja do kungfu4000: lamina em folha e ANEL no
            // punho. A anterior era uma adaga qualquer — e a original do jogo era
            // uma faca de cozinha.
            // A KUNAI ESTAVA NO PACOTE O TEMPO TODO. Passei por tres versoes —
            // a faca do DungeonTileset II, uma adaga qualquer e a do kit ninja —
            // sem olhar o grupo de laminas curtas desta folha, onde ha kunai com
            // lamina em folha e anel no punho, ja no estilo do resto do arsenal.
            // Kunai com barbatanas e anel no punho — a que da para reconhecer a
            // primeira vista como arma de ninja, e nao como faca pequena.
            new Peca("kunai",         STEEL,  7, 11, ""),
            // O TRIDENTE TAMBEM. Ele era desenhado a mao porque a pesquisa dizia
            // que este pacote nao tinha nenhum; tem trinta, num grupo inteiro.
            new Peca("trident",       STEEL, 21,  5, ""),
            // As lendarias saem da folha DOURADA, e nao da nossa rampa sobre o aco.
            new Peca("sword_legend",  GOLD,   2,  6, "legend"),
            new Peca("axe_legend",    GOLD,   2, 18, "legend"),
            new Peca("wand_legend",   GOLD,  19,  8, "legend"),
    };

    public static void main(String[] args) throws Exception {
        Map<String, BufferedImage> folhas = new LinkedHashMap<>();
        for (String f : new String[]{ASSORTED, STEEL, BENNY, GUNS, NINJA, ARCOS, GOLD,
                STAVE_AGUA, STAVE_FOGO}) {
            File arq = new File(FONTE + f);
            if (!arq.isFile()) {
                System.err.println("Falta o pacote: " + arq.getPath());
                System.err.println("Ver docs/TERCEIROS.md para de onde baixar.");
                System.exit(1);
            }
            folhas.put(f, ImageIO.read(arq));
        }
        for (Peca p : PECAS) {
            BufferedImage cel = recortar(folhas.get(p.folha()), p.folha(), p.col(), p.lin());
            BufferedImage fonte = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            fonte.getGraphics().drawImage(cel, 0, 0, null);
            if ("semPontaDeBaixo".equals(p.truque())) {
                tirarPontaDeBaixo(fonte);
            }
            Point bolinha = "gema".equals(p.truque()) ? bolinhaDoCabo(fonte) : null;
            int[] cabo = p.cabo() == null ? MADEIRA : RAMPAS.get(p.cabo());
            // "cru": o desenho ja vem pronto e na cor certa, entao ele NAO passa
            // pelo remapeamento de metal e madeira — repintar aqui so estragaria
            // um orbe azul ou uma chama laranja que ja estao como devem estar. O
            // unico ajuste e o espelho, porque o projeto desenha a arma apontando
            // para cima-esquerda e o pacote a desenha para cima-direita.
            BufferedImage o = "cru".equals(p.truque())
                    ? espelhar(fonte)
                    : tratar(fonte, RAMPAS.get(p.elem()), cabo, p.folha().equals(BENNY));
            if (bolinha != null) {
                gema(o, bolinha);
            }
            ImageIO.write(o, "png", new File(ITENS + p.saida() + ".png"));
            System.out.println("  sprites/items/" + p.saida() + ".png");
            if (p.saida().equals("trident")) {
                // O tridente e arremessado: quem voa e uma entidade, e ela carrega
                // o proprio sprite. Sao o mesmo desenho, entao sai dos dois lugares
                // de uma vez — dois arquivos mantidos a mao divergiriam no primeiro
                // ajuste.
                String util = "src/main/resources/com/retronova/resources/sprites/objects/utility/";
                ImageIO.write(o, "png", new File(util + "trident.png"));
                System.out.println("  sprites/objects/utility/trident.png");
            }
        }
        System.out.println("  " + PECAS.length + " armas");
        flechas();
        bolas();
    }

    /**
     * A bola de magia da varinha, uma por elemento.
     *
     * Esta e desenhada e nao recortada de pacote: nenhum dos tres tem projetil de
     * feitico, e uma bolinha de cinco pixels e das poucas coisas que sai melhor
     * calculada que garimpada. Contorno escuro, miolo na cor do elemento e um
     * brilho em cima a esquerda, que e o que da volume a uma esfera nesse tamanho.
     */
    private static void bolas() throws Exception {
        String pasta = "src/main/resources/com/retronova/resources/sprites/objects/utility/";
        int[][] forma = {{6, 9}, {5, 10}, {5, 10}, {5, 10}, {6, 9}};
        for (Map.Entry<String, int[]> e : RAMPAS.entrySet()) {
            if (!e.getKey().isEmpty() && !ehElemento(e.getKey())) {
                continue;
            }
            int[] rampa = e.getValue();
            BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int i = 0; i < forma.length; i++) {
                int y = 6 + i;
                for (int x = forma[i][0]; x <= forma[i][1]; x++) {
                    boolean borda = i == 0 || i == forma.length - 1
                            || x == forma[i][0] || x == forma[i][1];
                    im.setRGB(x, y, 0xFF000000 | (borda ? CONTORNO : rampa[3]));
                }
            }
            im.setRGB(6, 7, 0xFF000000 | rampa[4]);
            im.setRGB(7, 7, 0xFF000000 | rampa[4]);
            im.setRGB(9, 9, 0xFF000000 | rampa[1]);
            String nome = e.getKey().isEmpty() ? "bolt" : "bolt_" + e.getKey();
            ImageIO.write(im, "png", new File(pasta + nome + ".png"));
            System.out.println("  sprites/objects/utility/" + nome + ".png");
        }
    }

    private static boolean ehElemento(String chave) {
        for (String e : ELEMENTOS) {
            if (e.equals(chave)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uma flecha por elemento, so trocando a cor.
     *
     * Aqui a recoloracao E a resposta certa, ao contrario das armas: flecha e um
     * objeto de tres pixels de largura que atravessa a tela em meio segundo. Mudar
     * a SILHUETA dela nao seria percebido, e o que o jogador precisa ler no voo e
     * de que arco aquilo saiu — ou seja, a cor. A base e a flecha que ja existe,
     * para o formato continuar o mesmo.
     */
    private static void flechas() throws Exception {
        String pasta = "src/main/resources/com/retronova/resources/sprites/objects/utility/";
        BufferedImage base = ImageIO.read(new File(pasta + "arrow.png"));
        for (String elem : ELEMENTOS) {
            Map.Entry<String, int[]> e = Map.entry(elem, RAMPAS.get(elem));
            List<Integer> tons = new ArrayList<>();
            for (int y = 0; y < base.getHeight(); y++) {
                for (int x = 0; x < base.getWidth(); x++) {
                    int p = base.getRGB(x, y);
                    int rgb = p & 0xFFFFFF;
                    if ((p >>> 24) > 16 && !escuro(rgb)) {
                        tons.add(rgb);
                    }
                }
            }
            Map<Integer, Integer> mapa = distribuir(tons, e.getValue());
            BufferedImage o = new BufferedImage(base.getWidth(), base.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < base.getHeight(); y++) {
                for (int x = 0; x < base.getWidth(); x++) {
                    int p = base.getRGB(x, y);
                    if ((p >>> 24) <= 16) {
                        continue;
                    }
                    int rgb = p & 0xFFFFFF;
                    o.setRGB(x, y, 0xFF000000 | (escuro(rgb) ? CONTORNO
                            : mapa.getOrDefault(rgb, rgb)));
                }
            }
            String nome = "arrow_" + e.getKey();
            ImageIO.write(o, "png", new File(pasta + nome + ".png"));
            System.out.println("  sprites/objects/utility/" + nome + ".png");
        }
    }

    /**
     * Recorta o quadro de 16 da folha certa.
     *
     * Cada pacote empacota de um jeito: o Shade cola as celulas sem calha, o
     * Bennyboi deixa um pixel entre elas, o pacote de armas deixa um pixel so na
     * horizontal, e o kit ninja traz um desenho por arquivo em 32x32 — esse ultimo
     * e centralizado num quadro de 16, o que cabe porque o desenho em si tem 12
     * por 6. Passo errado corta a arma pela metade, entao a regra fica aqui, num
     * lugar so, e nao espalhada por chamadas de getSubimage.
     */
    /** Espelha na horizontal, para a arma apontar para o lado que o jogo espera. */
    private static BufferedImage espelhar(BufferedImage cel) {
        BufferedImage o = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        o.getGraphics().drawImage(cel, 16, 0, -16, 16, null);
        return o;
    }

    private static BufferedImage recortar(BufferedImage folha, String qual, int col, int lin) {
        if (qual.equals(NINJA)) {
            return centralizar(folha);
        }
        if (qual.equals(ARCOS)) {
            return encolher(folha.getSubimage(col * 24, lin * 24, 24, 24));
        }
        int passoX = qual.equals(BENNY) ? 17 : qual.equals(GUNS) ? 17 : 16;
        int passoY = qual.equals(BENNY) ? 17 : 16;
        return folha.getSubimage(col * passoX, lin * passoY, 16, 16);
    }

    /**
     * Traz um desenho de 24 para 16 APARANDO as pontas, e nao reduzindo.
     *
     * Tentei reduzir de duas formas antes, e as duas falharam pelo mesmo motivo:
     * a corda do arco tem UM pixel de largura, e nenhuma reducao para dois tercos
     * preserva uma linha de um pixel. Tirar uma linha a cada tres transformou a
     * corda em tracejado; amostrar pelo vizinho mais proximo, idem. Nao e questao
     * de achar o algoritmo certo — nao ha espaco.
     *
     * Aparar mantem cada pixel do tamanho que era, entao a corda continua uma
     * linha continua e o contorno continua fechado. O preco e cortar as PONTAS das
     * hastes, igualmente dos dois lados para o arco nao ficar torto. A troca vale:
     * a 16 pixels, um arco um pouco mais curto le como arco, e um arco tracejado
     * nao le como nada.
     */
    private static BufferedImage encolher(BufferedImage cel) {
        int x0 = 999, y0 = 999, x1 = -1, y1 = -1;
        for (int y = 0; y < cel.getHeight(); y++) {
            for (int x = 0; x < cel.getWidth(); x++) {
                if ((cel.getRGB(x, y) >>> 24) > 16) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        BufferedImage o = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        if (x1 < 0) {
            return o;
        }
        int larg = x1 - x0 + 1, alt = y1 - y0 + 1;
        int cx = larg > 16 ? x0 + (larg - 16 + 1) / 2 : x0;
        int cy = alt > 16 ? y0 + (alt - 16 + 1) / 2 : y0;
        int w = Math.min(16, larg), h = Math.min(16, alt);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                o.setRGB((16 - w) / 2 + x, (16 - h) / 2 + y, cel.getRGB(cx + x, cy + y));
            }
        }
        return o;
    }

    /** Poe o desenho de um arquivo solto no meio de um quadro de 16. */
    private static BufferedImage centralizar(BufferedImage im) {
        int x0 = 999, y0 = 999, x1 = -1, y1 = -1;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                if ((im.getRGB(x, y) >>> 24) > 16) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        BufferedImage o = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        if (x1 < 0) {
            return o;
        }
        int larg = Math.min(16, x1 - x0 + 1), alt = Math.min(16, y1 - y0 + 1);
        int dx = (16 - larg) / 2, dy = (16 - alt) / 2;
        for (int y = 0; y < alt; y++) {
            for (int x = 0; x < larg; x++) {
                o.setRGB(dx + x, dy + y, im.getRGB(x0 + x, y0 + y));
            }
        }
        return o;
    }

    /**
     * Traduz a celula para a paleta do jogo.
     *
     * @param recortar o pacote do Bennyboi vem com fundo cinza OPACO, e o mesmo
     *                 cinza aparece dentro dos desenhos. Por isso o fundo e tirado
     *                 por preenchimento a partir das bordas, e nao trocando a cor
     *                 em todo lugar — trocar em todo lugar abriria buracos no meio
     *                 das armas.
     */
    private static BufferedImage tratar(BufferedImage cel, int[] rampa, int[] cabo, boolean recortar) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        im.getGraphics().drawImage(cel, 0, 0, null);
        if (recortar) {
            tirarFundo(im);
        }

        // Metal e madeira sao separados pela saturacao, e ordenados por
        // luminosidade: e a ORDEM das sombras que da volume, e trocar cor por cor
        // sem respeitar essa ordem achata o desenho.
        boolean soMadeira = true;
        for (int y = 0; y < 16 && soMadeira; y++) {
            for (int x = 0; x < 16; x++) {
                int p = im.getRGB(x, y);
                int rgb = p & 0xFFFFFF;
                if ((p >>> 24) > 16 && !escuro(rgb) && !saturado(rgb)) {
                    soMadeira = false;
                    break;
                }
            }
        }
        List<Integer> metal = new ArrayList<>(), lenho = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int p = im.getRGB(x, y);
                if ((p >>> 24) <= 16) {
                    continue;
                }
                int rgb = p & 0xFFFFFF;
                if (escuro(rgb)) {
                    continue;                       // contorno, tratado a parte
                }
                // Arma que so tem madeira — os arcos do Bennyboi sao de madeira
                // inteira — nao teria onde mostrar o elemento: um arco de fogo
                // sairia marrom igual ao comum. Nesse caso a rampa vale para tudo.
                (saturado(rgb) && !soMadeira ? lenho : metal).add(rgb);
            }
        }
        Map<Integer, Integer> mapa = new LinkedHashMap<>();
        mapa.putAll(distribuir(metal, rampa));
        mapa.putAll(distribuir(lenho, cabo));

        BufferedImage o = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int p = im.getRGB(x, y);
                if ((p >>> 24) <= 16) {
                    continue;
                }
                int rgb = p & 0xFFFFFF;
                int nova = escuro(rgb) ? CONTORNO : mapa.getOrDefault(rgb, rgb);
                o.setRGB(x, y, 0xFF000000 | nova);
            }
        }
        return o;
    }

    /** Distribui os tons achados pela rampa, do mais escuro ao mais claro. */
    private static Map<Integer, Integer> distribuir(List<Integer> cores, int[] rampa) {
        List<Integer> unicas = new ArrayList<>();
        for (int c : cores) {
            if (!unicas.contains(c)) {
                unicas.add(c);
            }
        }
        unicas.sort((a, b) -> Double.compare(luz(a), luz(b)));
        // DESENHO DE POUCOS TONS NAO USA A RAMPA INTEIRA. Espalhar duas cores de
        // ponta a ponta joga a principal no extremo escuro, e o resultado e uma
        // arma quase preta — foi o que aconteceu com a kunai, que vem do pacote
        // com um cinza so e um brilho. Com poucos tons, a distribuicao comeca no
        // meio da rampa, onde ainda ha contraste contra o chao da masmorra.
        int inicio = unicas.size() <= 3 ? 2 : 0;
        Map<Integer, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < unicas.size(); i++) {
            int j = unicas.size() == 1 ? rampa.length - 2
                    : inicio + Math.round(i * (rampa.length - 1 - inicio)
                            / (float) (unicas.size() - 1));
            mapa.put(unicas.get(i), rampa[j]);
        }
        return mapa;
    }

    /**
     * A bolinha no alto do punho — onde a gema vai.
     *
     * Na celula original a guarda e o cabo vem em MARROM, e o centro da guarda,
     * logo acima de onde a mao segura, e o pixel mais claro desse marrom: ali o
     * desenhista ja tinha posto um brilho. E exatamente a "bolinha" que se ve na
     * arte oficial da espada do Muffin, entao e ali que a gema entra.
     *
     * Procurar o pixel mais claro do cabo, e nao o centro da mancha, foi o que
     * consertou: com a espada inteira preta, cabo e lamina passaram a ter a mesma
     * cor e o "centro do cabo" virava o centro da ESPADA — a gema ia parar no meio
     * da lamina. Por isso a conta e feita na celula ORIGINAL, antes de recolorir.
     */
    private static Point bolinhaDoCabo(BufferedImage cel) {
        Point melhor = null;
        double maisClaro = -1;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int p = cel.getRGB(x, y);
                int rgb = p & 0xFFFFFF;
                if ((p >>> 24) <= 16 || escuro(rgb) || !saturado(rgb)) {
                    continue;
                }
                if (luz(rgb) > maisClaro) {
                    maisClaro = luz(rgb);
                    melhor = new Point(x, y);
                }
            }
        }
        return melhor;
    }

    /**
     * A gema dourada, medida na arte oficial. UM pixel so.
     *
     * Tinha uma sombra logo abaixo, para dar volume. Nao da: a 16 pixels, com a
     * espada inteira preta, dois pixels amarelos leem como dois enfeites e nao
     * como um com sombra. O ponto tem de ser um ponto.
     */
    private static void gema(BufferedImage im, Point onde) {
        im.setRGB(onde.x, onde.y, 0xFFE4C262);
    }

    /**
     * Corta a ponta de baixo, deixando so o ornamento de cima.
     *
     * O cajado e desenhado do canto de cima a esquerda para o de baixo a direita,
     * entao o fim dessa diagonal e a ponta de baixo. Ela e APAGADA, e nao
     * repintada: repintar deixava a mesma silhueta com outra cor, e como o cabo
     * inteiro e dourado o resultado era um calombo de ouro na base. Apagando, a
     * haste termina onde a mao segura — que e o formato de cajado da arte oficial:
     * cabo liso e um circulo em cima.
     *
     * Depois do corte o contorno e refeito na borda nova, senao o cajado terminava
     * numa fatia de metal exposto.
     */
    private static void tirarPontaDeBaixo(BufferedImage im) {
        final int corte = 22;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (x + y > corte) {
                    im.setRGB(x, y, 0);
                }
            }
        }
        BufferedImage antes = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        antes.getGraphics().drawImage(im, 0, 0, null);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if ((antes.getRGB(x, y) >>> 24) <= 16 || escuro(antes.getRGB(x, y) & 0xFFFFFF)) {
                    continue;
                }
                if (vazio(antes, x - 1, y) || vazio(antes, x + 1, y)
                        || vazio(antes, x, y - 1) || vazio(antes, x, y + 1)) {
                    im.setRGB(x, y, 0xFF000000);
                }
            }
        }
    }

    private static boolean vazio(BufferedImage im, int x, int y) {
        return x < 0 || y < 0 || x > 15 || y > 15 || (im.getRGB(x, y) >>> 24) <= 16;
    }

    /** Preenchimento a partir das bordas: so o que encosta na moldura e fundo. */
    private static void tirarFundo(BufferedImage im) {
        int fundo = im.getRGB(0, 0) & 0xFFFFFF;
        boolean[][] visto = new boolean[16][16];
        Deque<int[]> fila = new ArrayDeque<>();
        for (int i = 0; i < 16; i++) {
            fila.add(new int[]{i, 0});
            fila.add(new int[]{i, 15});
            fila.add(new int[]{0, i});
            fila.add(new int[]{15, i});
        }
        while (!fila.isEmpty()) {
            int[] p = fila.poll();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x > 15 || y > 15 || visto[x][y]) {
                continue;
            }
            if ((im.getRGB(x, y) & 0xFFFFFF) != fundo) {
                continue;
            }
            visto[x][y] = true;
            im.setRGB(x, y, 0);
            fila.add(new int[]{x + 1, y});
            fila.add(new int[]{x - 1, y});
            fila.add(new int[]{x, y + 1});
            fila.add(new int[]{x, y - 1});
        }
    }

    /** Contorno: quase preto em qualquer um dos pacotes. */
    private static boolean escuro(int rgb) {
        return luz(rgb) < 40;
    }

    /** Marrom do cabo contra o cinza do metal. */
    private static boolean saturado(int rgb) {
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        return hsb[1] > 0.30f && hsb[0] > 0.02f && hsb[0] < 0.18f;
    }

    private static double luz(int rgb) {
        return 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
    }

    private static int[] girar(int[] rampa, float matiz, float sat, float val) {
        int[] o = new int[rampa.length];
        for (int i = 0; i < rampa.length; i++) {
            float[] hsb = Color.RGBtoHSB((rampa[i] >> 16) & 0xFF, (rampa[i] >> 8) & 0xFF,
                    rampa[i] & 0xFF, null);
            float h = matiz > 0 ? matiz : hsb[0];
            o[i] = Color.HSBtoRGB(h, Math.min(1f, hsb[1] * sat), Math.min(1f, hsb[2] * val))
                    & 0xFFFFFF;
        }
        return o;
    }
}
