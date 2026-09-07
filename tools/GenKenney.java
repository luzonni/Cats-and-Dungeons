import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deriva os sprites de cenário a partir do pacote Kenney "Tiny Dungeon" (CC0).
 *
 *     java tools/GenKenney.java <pasta Tiles do pacote> [--preview arquivo.png]
 *
 * O pacote original NÃO está versionado — só o resultado. Baixe em
 * https://kenney.nl/assets/tiny-dungeon e aponte para a pasta {@code Tiles/},
 * que traz os 132 tiles de 16x16 já recortados. A licença está registrada em
 * docs/TERCEIROS.md.
 *
 * A chama é a única exceção: o Kenney não tem fogo, então os quadros vêm de uma
 * segunda folha CC0, pequena o bastante para ficar versionada em
 * tools/assets/ — ver o README de lá.
 *
 * POR QUE UM GERADOR, E NÃO COPIAR OS PNG
 *
 * A arte do Kenney é azul-acinzentada e o chão é areia clara — bonito, mas de
 * outro jogo. Copiar direto traria duas paletas convivendo na mesma tela, que é
 * exatamente o problema que documentation/padroes/ARTE-CENARIO.md descreve. Aqui
 * cada cor do pacote é remapeada para uma rampa nossa, então o resultado nasce
 * dentro da identidade do jogo e continua reproduzível: mudou a rampa, roda de
 * novo e todo o cenário acompanha.
 *
 * AS TRÊS FAMÍLIAS
 *
 *  - PEDRA: estrutura. Frio, dessaturado. É o corpo da dungeon.
 *  - CHÃO: laje. Mesma família da pedra, porém comprimida num intervalo estreito
 *    e escuro — o piso não pode competir com o que está em cima dele.
 *  - MADEIRA: o único calor tolerado fora do fogo. Fica abafado de propósito,
 *    longe do carmesim da UI.
 *
 * O deslocamento na rampa é o que resolve a parede de duas faces: o mesmo tijolo
 * entra claro no topo e escuro na face frontal, e é o contraste entre os dois
 * que dá volume.
 */
public class GenKenney {

    // ------------------------------------------------------------------ rampas

    /** Pedra fria, do contorno ao brilho. */
    static final int[] PEDRA = {
        0xff141a20, 0xff1a2028, 0xff243840, 0xff2f434a, 0xff355157, 0xff3f5f64,
        0xff4a6b70, 0xff5a7d81, 0xff6d8f92, 0xff85a5a6, 0xff9fbcbc
    };

    /** Laje. Intervalo curto: o piso é fundo, não assunto. */
    static final int[] CHAO = {
        0xff2b3037, 0xff31373e, 0xff3a4048, 0xff434a53, 0xff4a515c, 0xff545c68
    };

    /**
     * Degraus. Cinza neutro, na mesma família do piso e só um pouco acima dele.
     * Na rampa de pedra os degraus saíam num verde-azulado que virava uma mancha
     * clara no meio do salão, competindo com a parede em vez de continuar o chão.
     */
    static final int[] DEGRAUS = {
        0xff2b3037, 0xff31373e, 0xff3a4048, 0xff434a53, 0xff4a515c, 0xff545c68,
        0xff5e6774, 0xff6a7480, 0xff76818e, 0xff838f9c, 0xff909caa
    };

    /** Rocha nao iluminada. Quase plana: e fundo de tela, nao assunto. */
    static final int[] ROCHA = {
        0xff0e1218, 0xff11161c, 0xff141a20, 0xff171e25, 0xff1a222a, 0xff1d262e
    };

    /** Madeira abafada. Quente o bastante para contrastar, longe do carmesim. */
    static final int[] MADEIRA = {
        0xff1c130f, 0xff33211a, 0xff4a3123, 0xff63432d, 0xff7d5738, 0xff9a6f47
    };

    /** Cor do Kenney -> degrau na rampa de pedra. */
    static final Map<Integer, Integer> DEGRAU_PEDRA = new LinkedHashMap<>();
    /** Cor do Kenney -> degrau na rampa de chão. */
    static final Map<Integer, Integer> DEGRAU_CHAO = new LinkedHashMap<>();
    /** Cor do Kenney -> degrau na rampa de madeira. */
    static final Map<Integer, Integer> DEGRAU_MADEIRA = new LinkedHashMap<>();

    static {
        DEGRAU_PEDRA.put(0xff262b44, 0);   // azul quase preto, contorno
        DEGRAU_PEDRA.put(0xff3f2631, 1);   // sombra
        DEGRAU_PEDRA.put(0xff763b36, 2);   // preenchimento de parede
        DEGRAU_PEDRA.put(0xff52607c, 3);   // pedra na sombra
        DEGRAU_PEDRA.put(0xff5a6988, 4);
        DEGRAU_PEDRA.put(0xff8b9bb4, 6);   // pedra, meio-tom
        DEGRAU_PEDRA.put(0xffaab7cc, 7);
        DEGRAU_PEDRA.put(0xffc0cbdc, 8);   // pedra na luz
        DEGRAU_PEDRA.put(0xffe4edf9, 9);
        DEGRAU_PEDRA.put(0xffffffff, 10);

        DEGRAU_CHAO.put(0xffcf8254, 0);
        DEGRAU_CHAO.put(0xffe19a65, 1);
        DEGRAU_CHAO.put(0xffeaa56c, 2);    // a areia base do pacote
        DEGRAU_CHAO.put(0xfff7c282, 4);
        DEGRAU_CHAO.put(0xfffec99c, 5);

        DEGRAU_MADEIRA.put(0xff3f2631, 0);
        DEGRAU_MADEIRA.put(0xff763b36, 1);
        DEGRAU_MADEIRA.put(0xffcf8254, 3);
        DEGRAU_MADEIRA.put(0xffbd6c4a, 3);
        DEGRAU_MADEIRA.put(0xffeaa56c, 4);
        DEGRAU_MADEIRA.put(0xfff7c282, 5);
    }

    /**
     * Preenchimentos de fundo do pacote. São chapados e ocupam a tela inteira
     * atrás dos props; viram transparência para o prop virar entidade.
     */
    static final int MARROM_FUNDO = 0xff763b36;
    static final int AREIA_FUNDO  = 0xffeaa56c;

    static File tiles;
    static File destino;
    static final Map<String, BufferedImage> gerados = new LinkedHashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("uso: java tools/GenKenney.java <pasta Tiles> [--preview saida.png]");
            System.exit(1);
        }
        tiles = new File(args[0]);
        if (!new File(tiles, "tile_0000.png").isFile()) {
            System.err.println("Não achei tile_0000.png em " + tiles.getAbsolutePath());
            System.exit(1);
        }
        destino = new File("src/main/resources/com/retronova/resources/sprites/objects");
        if (!destino.isDirectory()) {
            System.err.println("Rode a partir da raiz do repositório.");
            System.exit(1);
        }

        chao();
        paredes();
        props();
        fogo(new File("tools/assets/torch_anim_cc0.png"));

        String preview = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--preview") && i + 1 < args.length) {
                preview = args[i + 1];
            }
        }
        if (preview != null) {
            Preview.escrever(gerados, new File(preview));
            System.out.println("preview: " + preview + " (" + gerados.size() + " sprites, nada gravado)");
            return;
        }
        for (Map.Entry<String, BufferedImage> e : gerados.entrySet()) {
            File f = new File(destino, e.getKey());
            f.getParentFile().mkdirs();
            ImageIO.write(e.getValue(), "png", f);
        }
        System.out.println("ok: " + gerados.size() + " sprites em " + destino.getPath());
    }

    // ------------------------------------------------------------------- chão

    /**
     * Piso de laje.
     *
     * O Kenney entrega terra chapada com cascalho espalhado. A textura é boa e a
     * estrutura não existe, então a laje é desenhada por cima: um sulco de argamassa
     * na borda direita e inferior, e um fio de luz na esquerda e no topo. É o
     * mesmo princípio da parede — uma face que pega luz e outra que não — na
     * escala de um tile.
     */
    static void chao() {
        gerados.put("tile/dungeonFloor.png", laje(48, 0));
        gerados.put("tile/dungeonFloorCracked.png", laje(49, 0));
        gerados.put("tile/dungeonFloorMossy.png", laje(53, 0x2e4a2c));
        gerados.put("tile/dungeonFloorBroken.png", quebrada(laje(51, 0)));
        gerados.put("tile/bedrock.png", rocha());
        gerados.put("particle/walking.png", poeira());
    }

    static BufferedImage laje(int indice, int limo) {
        BufferedImage t = mapear(tile(indice), DEGRAU_CHAO, CHAO, 0);
        int argamassa = CHAO[0];
        int luz = CHAO[3];
        for (int i = 0; i < 16; i++) {
            t.setRGB(i, 15, argamassa);
            t.setRGB(15, i, argamassa);
            if (i < 15) {
                t.setRGB(i, 0, luz);
                t.setRGB(0, i, luz);
            }
        }
        if (limo != 0) {
            manchar(t, limo);
        }
        return t;
    }

    /**
     * Laje lascada: um canto cedeu e sobraram cacos em volta.
     *
     * A primeira versao era uma fenda diagonal atravessando o tile. Isolada
     * ficava boa; repetida pelo salao, todas as fendas apontavam para o mesmo
     * lado e o chao virava um hachurado — parecia textura, nao estrago. Um canto
     * afundado nao tem direcao tao marcada e some no conjunto, que e o que se
     * espera de desgaste.
     */
    static BufferedImage quebrada(BufferedImage t) {
        int buraco = 0xff23272d, borda = CHAO[0], caco = CHAO[4];
        // Canto inferior direito afundado, com a borda irregular.
        int[] recuo = {6, 5, 5, 4, 3, 3, 2};
        for (int i = 0; i < recuo.length; i++) {
            int y = 15 - i;
            for (int x = 16 - recuo[i]; x < 16; x++) {
                t.setRGB(x, y, buraco);
            }
            int x0 = 16 - recuo[i] - 1;
            if (x0 >= 0) {
                t.setRGB(x0, y, borda);
            }
        }
        // Cacos soltos, longe do buraco, para o estrago nao ficar so num canto.
        for (int[] p : new int[][]{{3, 4}, {6, 2}, {2, 9}, {8, 6}}) {
            t.setRGB(p[0], p[1], caco);
        }
        return t;
    }

    /** Mancha orgânica de limo, semente fixa para o tile sair igual toda vez. */
    static void manchar(BufferedImage t, int cor) {
        java.util.Random r = new java.util.Random(0x10D0);
        for (int n = 0; n < 4; n++) {
            int cx = 3 + r.nextInt(10), cy = 3 + r.nextInt(10), raio = 1 + r.nextInt(2);
            for (int y = cy - raio; y <= cy + raio; y++) {
                for (int x = cx - raio; x <= cx + raio; x++) {
                    if (x < 1 || y < 1 || x > 14 || y > 14) {
                        continue;
                    }
                    if ((x - cx) * (x - cx) + (y - cy) * (y - cy) > raio * raio) {
                        continue;
                    }
                    t.setRGB(x, y, misturar(t.getRGB(x, y), 0xff000000 | cor, 0.40));
                }
            }
        }
    }

    // ---------------------------------------------------------------- paredes

    /**
     * Parede em duas faces.
     *
     * O mesmo tijolo do Kenney entra duas vezes: claro no topo, três degraus mais
     * escuro na face. A regra vem da pesquisa — o topo de uma parede é muito mais
     * claro que a lateral, e é esse salto de valor que cria a profundidade, não o
     * desenho. A face ainda ganha duas linhas escurecidas embaixo, que a assentam
     * no chão como uma sombra de contato.
     */
    static void paredes() {
        gerados.put("tile/stoneTop.png", mapear(tile(40), DEGRAU_PEDRA, PEDRA, 0));
        gerados.put("tile/stoneFace.png", assentar(mapear(tile(40), DEGRAU_PEDRA, PEDRA, -3)));
        // Variante com vãos: quebra a repetição em paredes longas e sugere que há
        // algo do outro lado.
        gerados.put("tile/stoneGrate.png", assentar(mapear(tile(28), DEGRAU_PEDRA, PEDRA, -3)));
        // Degraus. Fiadas horizontais lidas de cima: cada linha clara é um nível.
        // O tile original tem as laterais escuras, que viram colunas quando ele se
        // repete; sem elas as fiadas correm sem emenda pela largura da plataforma.
        gerados.put("tile/stoneSteps.png",
                semEmendaLateral(mapear(tile(36), DEGRAU_PEDRA, DEGRAUS, -2)));

        // Relevo e flâmula são variantes da FACE da parede, não props soltos: no
        // original a arte já vem desenhada sobre a alvenaria. Viram tile porque
        // pertencem à parede, e o vocabulário da antecâmara pede os dois — o
        // relevo é a marca de quem construiu isto, a flâmula é sinal de que
        // alguém esteve aqui e acendeu fogo.
        gerados.put("tile/stoneRelief.png", assentar(mapear(tile(19), DEGRAU_PEDRA, PEDRA, -3)));
        gerados.put("tile/stoneBanner.png", assentar(mapear(tile(29), DEGRAU_PEDRA, PEDRA, -3)));
    }

    /**
     * Substitui as colunas de borda chapadas pela primeira coluna com desenho.
     *
     * O Kenney fecha alguns tiles com uma moldura escura de largura variável. Ela
     * some no tile isolado e vira uma coluna preta assim que o tile se repete, que
     * é o oposto do que um piso contínuo precisa.
     */
    static BufferedImage semEmendaLateral(BufferedImage t) {
        int esq = 0, dir = t.getWidth() - 1;
        while (esq < t.getWidth() && colunaChapada(t, esq)) {
            esq++;
        }
        while (dir >= 0 && colunaChapada(t, dir)) {
            dir--;
        }
        for (int y = 0; y < t.getHeight(); y++) {
            for (int x = 0; x < esq; x++) {
                t.setRGB(x, y, t.getRGB(esq, y));
            }
            for (int x = dir + 1; x < t.getWidth(); x++) {
                t.setRGB(x, y, t.getRGB(dir, y));
            }
        }
        return t;
    }

    static boolean colunaChapada(BufferedImage t, int x) {
        int primeira = t.getRGB(x, 0);
        for (int y = 1; y < t.getHeight(); y++) {
            if (t.getRGB(x, y) != primeira) {
                return false;
            }
        }
        return true;
    }

    /** Escurece as duas últimas linhas, dando à face contato com o piso. */
    static BufferedImage assentar(BufferedImage t) {
        for (int y = 14; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double peso = (y == 15) ? 0.55 : 0.3;
                t.setRGB(x, y, misturar(t.getRGB(x, y), PEDRA[0], peso));
            }
        }
        return t;
    }

    // ------------------------------------------------------------------ props

    static void props() {
        // Pilar de dois tiles: capitel em cima, fuste embaixo. O sprite é quadrado
        // porque o Sheeter fatia folhas em quadrados; a arte fica centrada.
        gerados.put("furniture/pillar.png",
                sombra(quadrado(empilhar(pedra(6), pedra(18)), 32), 32));

        gerados.put("furniture/gate.png", portao());
        gerados.put("furniture/entrance.png", entrada());
        gerados.put("furniture/plate.png", placa());

        gerados.put("furniture/rubble.png", pedra(24));

        gerados.put("furniture/barrel.png", madeira(73));
        gerados.put("furniture/shelf.png", madeira(75));
        gerados.put("furniture/table.png", madeira(72));
        gerados.put("furniture/rug.png", madeira(66));
        gerados.put("furniture/chest.png", madeira(90));
        gerados.put("furniture/bed.png", pedra(54));
        gerados.put("furniture/anvil.png", pedra(74));
    }



    /**
     * Placa pregada na parede, 16x16.
     *
     * Ela e colocada NO PROPRIO TILE DE PAREDE, e nao no chao a frente dele —
     * por isso tem o tamanho de um tile e nada de poste. A primeira versao era um
     * cartaz fincado no chao e nao lia como placa: lia como um item solto no meio
     * da sala. Ficar em cima da alvenaria e o que a torna parede, e nao movel.
     *
     * O que faz alguem entender que ali ha algo escrito nao e o formato, e o
     * conjunto: moldura, rebites nos cantos e tres sulcos de comprimentos
     * diferentes, que e o desenho universal de texto sem precisar de letra.
     */
    static BufferedImage placa() {
        BufferedImage o = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // Sombra na alvenaria, a direita e abaixo: e o que despega a tabua da
        // parede em vez de deixar ela parecendo pintada nela.
        preencher(o, 3, 4, 13, 11, 0x66000000 | (PEDRA[0] & 0xFFFFFF));
        preencher(o, 1, 2, 13, 11, MADEIRA[0]);
        preencher(o, 2, 3, 11, 9, MADEIRA[3]);
        preencher(o, 2, 3, 11, 1, MADEIRA[4]);      // topo pega luz
        preencher(o, 2, 11, 11, 1, MADEIRA[1]);     // base na sombra
        // Sulcos: tres linhas de comprimentos diferentes, como texto.
        preencher(o, 4, 5, 7, 1, MADEIRA[1]);
        preencher(o, 4, 7, 5, 1, MADEIRA[1]);
        preencher(o, 4, 9, 6, 1, MADEIRA[1]);
        // Rebites nos quatro cantos.
        for (int x : new int[]{2, 12}) {
            for (int y : new int[]{3, 11}) {
                o.setRGB(x, y, PEDRA[8]);
            }
        }
        return o;
    }

    static void preencher(BufferedImage img, int x, int y, int w, int h, int cor) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                if (i >= 0 && j >= 0 && i < img.getWidth() && j < img.getHeight()) {
                    img.setRGB(i, j, cor);
                }
            }
        }
    }

    /**
     * Rocha macica em volta da sala.
     *
     * Escura e quase sem desenho de proposito: e o que a camera mostra quando
     * chega na borda do mapa, e ali nao pode haver nada que puxe o olho. O que
     * importa e que deixou de ser preto.
     */
    static BufferedImage rocha() {
        // Sem sulco nas bordas, ao contrario da laje: aqui a emenda entre tiles
        // nao pode aparecer, ou a margem vira um xadrez que puxa o olho.
        return mapear(tile(49), DEGRAU_CHAO, ROCHA, 0);
    }

    // ------------------------------------------------------------------ fogo

    /** Quadros da chama, na folha CC0 de 48x32 — cinco de 16x16, o sexto vazio. */
    static final int[][] QUADROS = {{0, 0}, {16, 0}, {32, 0}, {0, 16}, {16, 16}};

    /** As quatro cores da chama. Passam intactas: sao o unico acento quente. */
    static boolean chama(int p) {
        return p == 0xfffdd835 || p == 0xffffb300 || p == 0xfffb8c00 || p == 0xfff4511e;
    }

    /**
     * Tocha e braseiro, animados.
     *
     * A chama vem de outro pacote CC0 porque o Kenney nao tem fogo, e fogo
     * parado nao convence: o braseiro e a tocha sao o que diz que alguem esteve
     * aqui ha pouco, e isso se le no movimento antes de se ler no desenho.
     *
     * Os dois saem como TIRA HORIZONTAL de quadros quadrados, que e como o
     * Sheeter fatia: cinco de 16 viram 80x16, cinco de 32 viram 160x32.
     */
    static void fogo(File folhaChama) {
        BufferedImage folha;
        try {
            folha = ImageIO.read(folhaChama);
        } catch (Exception e) {
            throw new RuntimeException("chama: " + folhaChama, e);
        }
        gerados.put("furniture/torch.png", tira(folha, 16, GenKenney::tocha));
        gerados.put("furniture/brazier.png", tira(folha, 32, GenKenney::braseiro));
    }

    /** Monta os cinco quadros lado a lado. */
    static BufferedImage tira(BufferedImage folha, int lado, java.util.function.BiFunction<BufferedImage, Integer, BufferedImage> quadro) {
        BufferedImage o = new BufferedImage(lado * QUADROS.length, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        for (int i = 0; i < QUADROS.length; i++) {
            BufferedImage f = folha.getSubimage(QUADROS[i][0], QUADROS[i][1], 16, 16);
            g.drawImage(quadro.apply(f, lado), i * lado, 0, null);
        }
        g.dispose();
        return o;
    }

    /** Tocha: a folha original, com o cabo passado para a madeira do jogo. */
    static BufferedImage tocha(BufferedImage quadro, int lado) {
        BufferedImage o = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int p = quadro.getRGB(x, y);
                if ((p >>> 24) == 0) {
                    continue;
                }
                o.setRGB(x, y, chama(p) ? p : madeiraPorLuz(p));
            }
        }
        return o;
    }

    /**
     * Braseiro: a bacia de pedra do Kenney com a chama dentro.
     *
     * Dois tiles de altura, ancorado pela base — o fogo fica na altura do olho e
     * a bacia toca o chao, entao ele le como movel e nao como decalque no piso.
     */
    static BufferedImage braseiro(BufferedImage quadro, int lado) {
        BufferedImage o = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(pedra(31), 8, 16, null);
        g.dispose();
        // So a parte de cima do quadro: o cabo da tocha nao entra no braseiro.
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 16; x++) {
                int p = quadro.getRGB(x, y);
                if ((p >>> 24) == 0 || !chama(p)) {
                    continue;
                }
                o.setRGB(8 + x, 10 + y, p);
            }
        }
        return o;
    }

    /** Marrons quase iguais do cabo original viram a rampa de madeira daqui. */
    static int madeiraPorLuz(int p) {
        int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
        double lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
        int i = (int) Math.round((lum - 0.15) / 0.045);
        return MADEIRA[Math.max(0, Math.min(MADEIRA.length - 1, i))];
    }


    // ---------------------------------------------------------------- portais

    /*
     * AS DUAS PASSAGENS DA SALA
     *
     * Antes eram um vao de um tile aberto na parede, e o resultado lia como
     * bloco faltando: um retangulo escuro no meio da alvenaria, com uma portinha
     * de 16px embaixo. Nenhum pacote CC0 resolve isso — todos sao grade de 16, e
     * porta neles e sempre um tile.
     *
     * Entao as duas passam a ser um PORTAL construido: moldura de pedra de tres
     * tiles com cornija por cima, e um miolo de 32x32 — quatro vezes a area da
     * porta anterior. A moldura e identica nas duas; so o miolo muda. Isso e o
     * que faz a antecamara ter uma gramatica: por onde se entra e por onde se
     * sai sao a mesma arquitetura, uma fechada e a outra aberta.
     */

    /**
     * Moldura do portal, 48x48, com o miolo de 32x32 encaixado.
     *
     * A arte ocupa as duas linhas de parede mais uma acima delas — a cornija, que
     * e o que da altura ao portal. Ela e macica de proposito: a versao anterior
     * deixava essa faixa escura e o olho lia buraco, nao verga.
     */
    static BufferedImage portal(BufferedImage miolo) {
        BufferedImage o = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        BufferedImage tijoloClaro = mapear(tile(40), DEGRAU_PEDRA, PEDRA, 1);
        BufferedImage jambaTopo = mapear(tile(57), DEGRAU_PEDRA, PEDRA, 0);
        BufferedImage jambaFace = assentar(mapear(tile(57), DEGRAU_PEDRA, PEDRA, -3));

        Graphics2D g = o.createGraphics();
        // Verga: alvenaria atravessando os tres tiles, uma linha acima da parede.
        for (int i = 0; i < 3; i++) {
            g.drawImage(tijoloClaro, i * 16, 0, null);
        }
        // Jambas: meio tile de cada lado, continuando a parede em que o portal
        // esta encaixado.
        g.drawImage(jambaTopo, 0, 16, null);
        g.drawImage(espelhar(jambaTopo), 32, 16, null);
        g.drawImage(jambaFace, 0, 32, null);
        g.drawImage(espelhar(jambaFace), 32, 32, null);
        g.dispose();

        // Cornija: duas fiadas mais claras no topo, e uma linha de sombra logo
        // abaixo. E o mesmo par de faces do resto do cenario, na escala do vao —
        // o topo pega luz, o que vem depois dele cai na sombra.
        for (int x = 0; x < 48; x++) {
            o.setRGB(x, 0, PEDRA[8]);
            o.setRGB(x, 1, PEDRA[7]);
            o.setRGB(x, 14, PEDRA[1]);
            o.setRGB(x, 15, PEDRA[0]);
        }
        // Ombreiras: engrossam a verga sobre o vao, para ela nao parecer solta.
        preencher(o, 4, 2, 40, 2, PEDRA[7]);
        preencher(o, 6, 12, 36, 2, PEDRA[3]);

        g = o.createGraphics();
        g.drawImage(miolo, 8, 16, null);
        g.dispose();

        // Sombra interna: o miolo fica recuado dentro da moldura.
        for (int y = 16; y < 48; y++) {
            o.setRGB(8, y, PEDRA[0]);
            o.setRGB(39, y, PEDRA[0]);
        }
        for (int x = 8; x < 40; x++) {
            o.setRGB(x, 16, PEDRA[0]);
        }
        return o;
    }

    /**
     * Porta dupla de madeira, 32x32. O miolo da entrada.
     *
     * Duas folhas de tabuas verticais, duas cintas de ferro e as argolas no
     * encontro delas. E desenhada aqui porque nenhum dos pacotes tem porta maior
     * que um tile; o que garante que ela combine e a paleta, que sai das mesmas
     * rampas do resto do cenario.
     */
    static BufferedImage portaMadeira() {
        BufferedImage o = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        preencher(o, 0, 0, 32, 32, MADEIRA[2]);
        // Tabuas: seis, com o fio de luz na esquerda de cada uma.
        for (int x = 0; x < 32; x += 5) {
            preencher(o, x, 0, 1, 32, MADEIRA[1]);
            preencher(o, x + 1, 0, 1, 32, MADEIRA[3]);
        }
        // Encontro das duas folhas.
        preencher(o, 15, 0, 2, 32, MADEIRA[0]);
        // Cintas de ferro, com rebites.
        for (int y : new int[]{5, 21}) {
            preencher(o, 0, y, 32, 4, PEDRA[5]);
            preencher(o, 0, y, 32, 1, PEDRA[7]);
            preencher(o, 0, y + 3, 32, 1, PEDRA[1]);
            for (int x = 2; x < 32; x += 6) {
                o.setRGB(x, y + 1, PEDRA[9]);
            }
        }
        // Argolas, uma em cada folha.
        for (int x : new int[]{11, 19}) {
            preencher(o, x, 14, 3, 1, PEDRA[8]);
            o.setRGB(x - 1, 15, PEDRA[6]);
            o.setRGB(x + 3, 15, PEDRA[6]);
            preencher(o, x, 16, 3, 1, PEDRA[6]);
        }
        // Base na sombra: a porta toca o chao.
        preencher(o, 0, 30, 32, 2, MADEIRA[0]);
        return o;
    }

    /**
     * Passagem aberta, 32x32. O miolo do portao.
     *
     * Escuridao que se aprofunda para baixo, com a grade recolhida na verga. A
     * grade recolhida e o detalhe que faz o portao ler como passagem e nao como
     * barreira: alguem a levantou, da para atravessar.
     */
    static BufferedImage passagem() {
        BufferedImage o = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            int degrau = y < 6 ? 2 : (y < 14 ? 1 : 0);
            preencher(o, 0, y, 32, 1, PEDRA[degrau]);
        }
        // Soleira: a laje que o gato pisa ao entrar no vao.
        preencher(o, 0, 28, 32, 4, CHAO[0]);
        preencher(o, 0, 28, 32, 1, CHAO[2]);
        // Grade recolhida: viga na verga e as barras penduradas, com as pontas
        // aparecendo. Desenhada barra a barra em vez de recortada de um tile —
        // recortando, os dentes viravam dois blocos claros soltos no escuro.
        preencher(o, 0, 0, 32, 3, PEDRA[5]);
        preencher(o, 0, 0, 32, 1, PEDRA[7]);
        for (int x = 2; x < 32; x += 6) {
            preencher(o, x, 3, 2, 4, PEDRA[6]);
            o.setRGB(x, 7, PEDRA[4]);
            o.setRGB(x + 1, 7, PEDRA[4]);
            o.setRGB(x, 8, PEDRA[3]);
        }
        return o;
    }

    static BufferedImage portao() {
        return portal(passagem());
    }

    static BufferedImage entrada() {
        return portal(portaMadeira());
    }

    // ------------------------------------------------------------- particulas

    /**
     * Poeira do passo, cinco quadros de 16x16.
     *
     * A folha anterior crescia, encolhia e CRESCIA DE NOVO dentro de uma unica
     * vida — dois sopros num passo so, e era boa parte do que fazia a caminhada
     * parecer errada. A referencia de animacao e sempre monotona: nuvem pequena,
     * nuvem maior, nuvem se desfazendo. E o que esta aqui.
     *
     * Duas outras mudancas de fundo: a cor sai da familia do chao, e nao mais do
     * branco puro — branco sobre laje escura le faisca, nao poeira; e o
     * esmaecimento e gravado no alfa de cada quadro, ja que a particula e
     * desenhada como sprite direto, sem controle de opacidade em tempo de jogo.
     */
    static BufferedImage poeira() {
        final int[] TOM = {0xff5e6774, 0xff6a7480, 0xff76818e, 0xff838f9c};
        BufferedImage o = new BufferedImage(16 * 5, 16, BufferedImage.TYPE_INT_ARGB);

        // Tres primeiros quadros: nuvem inteira, crescendo e subindo.
        final int[] largura = {4, 7, 9};
        final int[] altura  = {2, 3, 4};
        final int[] subida  = {0, 1, 3};
        final int[] alfa    = {255, 220, 165};
        for (int f = 0; f < 3; f++) {
            int cx = f * 16 + 8;
            int base = 13 - subida[f];
            for (int y = 0; y < altura[f]; y++) {
                int w = Math.max(1, largura[f] - y * 2);
                for (int x = -w / 2; x <= w / 2; x++) {
                    ponto(o, cx + x, base - y, TOM[Math.min(TOM.length - 1, y)], alfa[f]);
                }
            }
        }

        // Dois ultimos: a nuvem ja se desfez, sobram grãos subindo e abrindo. Em
        // vez de furar a nuvem — que deixava a silhueta rendada e artificial —,
        // aqui os grãos sao postos um a um, em posicoes fixas.
        final int[][] graos3 = {{-4, 7}, {-2, 5}, {0, 4}, {2, 6}, {4, 8}, {1, 8}};
        for (int[] g : graos3) {
            ponto(o, 3 * 16 + 8 + g[0], 13 - g[1], TOM[2], 115);
        }
        final int[][] graos4 = {{-5, 10}, {-1, 8}, {3, 11}};
        for (int[] g : graos4) {
            ponto(o, 4 * 16 + 8 + g[0], 13 - g[1], TOM[3], 65);
        }
        return o;
    }

    /** Pinta um pixel do quadro, ignorando o que cair fora dele. */
    static void ponto(BufferedImage o, int x, int y, int cor, int alfa) {
        if (x < 0 || y < 0 || x >= o.getWidth() || y >= o.getHeight()) {
            return;
        }
        o.setRGB(x, y, (alfa << 24) | (cor & 0xFFFFFF));
    }

    static BufferedImage pedra(int indice) {
        return mapear(semFundo(tile(indice), MARROM_FUNDO, AREIA_FUNDO), DEGRAU_PEDRA, PEDRA, 0);
    }

    static BufferedImage madeira(int indice) {
        BufferedImage t = semFundo(tile(indice), MARROM_FUNDO, AREIA_FUNDO);
        t = mapear(t, DEGRAU_PEDRA, PEDRA, 0);
        return mapear(semFundo(tile(indice), MARROM_FUNDO, AREIA_FUNDO), DEGRAU_MADEIRA, MADEIRA, 0, t);
    }

    // ----------------------------------------------------------------- pixels

    static BufferedImage tile(int indice) {
        try {
            return ImageIO.read(new File(tiles, String.format("tile_%04d.png", indice)));
        } catch (Exception e) {
            throw new RuntimeException("tile " + indice, e);
        }
    }

    static BufferedImage mapear(BufferedImage src, Map<Integer, Integer> degraus, int[] rampa, int desloc) {
        return mapear(src, degraus, rampa, desloc, null);
    }

    /**
     * Troca as cores conhecidas pelo degrau correspondente da rampa.
     *
     * O que não estiver na tabela passa intacto: são os acentos do pacote — fogo,
     * cristal, sangue — e eles têm função justamente por serem os únicos pontos
     * saturados da cena.
     *
     * @param base quando informado, o pixel não mapeado vem dela em vez do original.
     */
    static BufferedImage mapear(BufferedImage src, Map<Integer, Integer> degraus, int[] rampa,
                                int desloc, BufferedImage base) {
        BufferedImage o = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                if ((p >>> 24) == 0) {
                    continue;
                }
                Integer d = degraus.get(p);
                if (d == null) {
                    o.setRGB(x, y, base == null ? p : base.getRGB(x, y));
                    continue;
                }
                int i = Math.max(0, Math.min(rampa.length - 1, d + desloc));
                o.setRGB(x, y, rampa[i]);
            }
        }
        return o;
    }

    /**
     * Apaga o preenchimento de fundo a partir das bordas.
     *
     * Inundar da borda, em vez de trocar a cor em toda a imagem, preserva a mesma
     * cor quando ela aparece dentro do prop — a tábua escura de uma estante é o
     * mesmo marrom do fundo da parede.
     */
    static BufferedImage semFundo(BufferedImage src, int... fundos) {
        BufferedImage o = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        int w = o.getWidth(), h = o.getHeight();
        boolean[] visto = new boolean[w * h];
        Deque<int[]> fila = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            fila.add(new int[]{x, 0});
            fila.add(new int[]{x, h - 1});
        }
        for (int y = 0; y < h; y++) {
            fila.add(new int[]{0, y});
            fila.add(new int[]{w - 1, y});
        }
        while (!fila.isEmpty()) {
            int[] p = fila.poll();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h || visto[y * w + x]) {
                continue;
            }
            visto[y * w + x] = true;
            int cor = o.getRGB(x, y);
            boolean ehFundo = (cor >>> 24) == 0;
            for (int f : fundos) {
                ehFundo |= cor == f;
            }
            if (!ehFundo) {
                continue;
            }
            o.setRGB(x, y, 0);
            fila.add(new int[]{x + 1, y});
            fila.add(new int[]{x - 1, y});
            fila.add(new int[]{x, y + 1});
            fila.add(new int[]{x, y - 1});
        }
        return o;
    }

    static BufferedImage empilhar(BufferedImage cima, BufferedImage baixo) {
        BufferedImage o = new BufferedImage(cima.getWidth(),
                cima.getHeight() + baixo.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(cima, 0, 0, null);
        g.drawImage(baixo, 0, cima.getHeight(), null);
        g.dispose();
        return o;
    }

    /** Centra a arte numa tela quadrada, apoiada na base — o Sheeter exige quadrado. */
    static BufferedImage quadrado(BufferedImage src, int lado) {
        BufferedImage o = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(src, (lado - src.getWidth()) / 2, lado - src.getHeight(), null);
        g.dispose();
        return o;
    }

    /**
     * Sombra projetada na base.
     *
     * Sem ela o objeto não toca o chão e o olho o lê deitado — foi exatamente o
     * que aconteceu com a primeira versão do pilar. Curta, contida no tile e
     * sempre para o mesmo lado, como manda a pesquisa.
     */
    static BufferedImage sombra(BufferedImage src, int lado) {
        BufferedImage o = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        int base = lado - 1;
        int meio = lado / 2;
        // Mais larga que o fuste, senão o próprio pilar a cobre por inteiro e o
        // objeto volta a flutuar. Deslocada para a direita: a luz do cenário vem
        // sempre da esquerda.
        for (int y = base - 2; y <= base; y++) {
            int largura = 26 - (base - y) * 6;
            for (int x = meio - largura / 2 + 2; x <= meio + largura / 2 + 2; x++) {
                if (x >= 0 && x < lado) {
                    o.setRGB(x, y, 0x77000000 | (PEDRA[0] & 0xFFFFFF));
                }
            }
        }
        Graphics2D g = o.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return o;
    }

    static BufferedImage espelhar(BufferedImage src) {
        BufferedImage o = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                o.setRGB(src.getWidth() - 1 - x, y, src.getRGB(x, y));
            }
        }
        return o;
    }

    static int misturar(int a, int b, double t) {
        int aa = a >>> 24;
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return (aa << 24) | (r << 16) | (g << 8) | bl;
    }

    /** Folha de contato dos sprites gerados, para conferir antes de gravar. */
    static class Preview {
        static void escrever(Map<String, BufferedImage> mapa, File saida) throws Exception {
            int z = 6, cols = 6, cell = 48 * z + 20;
            int linhas = (mapa.size() + cols - 1) / cols;
            BufferedImage o = new BufferedImage(cols * cell + 20, linhas * cell + 20,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = o.createGraphics();
            g.setColor(new java.awt.Color(0x101014));
            g.fillRect(0, 0, o.getWidth(), o.getHeight());
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 12));
            int i = 0;
            for (Map.Entry<String, BufferedImage> e : mapa.entrySet()) {
                BufferedImage im = e.getValue();
                boolean piso = e.getKey().contains("Floor") || e.getKey().contains("stone");
                BufferedImage arte = piso ? ladrilhar(im, 3) : im;
                int x = 10 + (i % cols) * cell, y = 10 + (i / cols) * cell;
                g.setColor(new java.awt.Color(0x2a2a32));
                g.fillRect(x, y, 48 * z, 48 * z);
                int lz = 48 * z / Math.max(arte.getWidth(), arte.getHeight());
                g.drawImage(arte, x, y, arte.getWidth() * lz, arte.getHeight() * lz, null);
                g.setColor(new java.awt.Color(0xffcc44));
                g.drawString(e.getKey().replace("furniture/", "").replace("tile/", ""),
                        x, y + 48 * z + 14);
                i++;
            }
            g.dispose();
            ImageIO.write(o, "png", saida);
        }

        static BufferedImage ladrilhar(BufferedImage t, int n) {
            BufferedImage o = new BufferedImage(t.getWidth() * n, t.getHeight() * n,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = o.createGraphics();
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    g.drawImage(t, c * t.getWidth(), r * t.getHeight(), null);
                }
            }
            g.dispose();
            return o;
        }
    }
}
