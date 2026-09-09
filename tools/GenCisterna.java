import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A CISTERNA INUNDADA: tiles e planta da primeira arena.
 *
 * POR QUE ESTA ARENA EXISTIA E NAO PARECIA NADA.
 *
 * A arena antiga tinha TRES cores no mapa — chao roxo, tijolo e vazio — contra as
 * onze da antecamara. Nao era uma sala, era uma caixa: um retangulo de chao com
 * borda. Nenhuma decisao de espaco, nenhum lugar para se esconder, nada que diga
 * onde o jogador esta. Ambientar nao e enfeitar; e dar ao lugar uma ideia.
 *
 * A IDEIA: a cisterna sob o castelo. A antecamara e pedra fria azul-acinzentada,
 * e a corrida DESCE a partir dela — entao a primeira arena e o reservatorio de
 * agua logo abaixo: agua parada esverdeada, canaletas no piso, grades por onde a
 * agua escorre. Continua o mesmo castelo, mais fundo e mais abandonado, o que faz
 * a passagem parecer um caminho e nao uma troca de cenario.
 *
 * DE ONDE VEM A ARTE. Do pacote Puny Dungeon, CC0, do mesmo autor (Shade) das
 * armas que o jogo ja usa — entao o cenario e as armas conversam sem ninguem
 * ajustar paleta. Nada aqui e desenhado: sao recortes de dezesseis por dezesseis
 * da folha original, gravados um a um como os outros tiles do jogo.
 *
 * A PLANTA TAMBEM SAI DAQUI. O mapa e um PNG em que cada cor e um tipo de tile, e
 * ele podia ser pintado a mao — a antecamara foi. Gerar por codigo, para a arena,
 * vale mais: a planta e feita de regras (o anel de agua na borda, as ilhas de
 * pedra, as canaletas que correm para o ralo) e regras se ajustam mexendo num
 * numero, enquanto um PNG pintado se ajusta pixel a pixel.
 *
 *     java tools/GenCisterna.java
 */
public class GenCisterna {

    private static final String FONTE =
            "tools/assets/fonte/puny/PUNY_DUNGEON_v1/punyworld-dungeon-tileset.png";
    private static final String TILES =
            "src/main/resources/com/retronova/resources/sprites/objects/tile/";
    private static final String MAPAS =
            "src/main/resources/com/retronova/resources/maps/";

    /** Recorte de um tile: nome de saida, coluna e linha na folha. */
    private record Peca(String saida, int col, int lin) {
    }

    private static final Peca[] PECAS = {
            // Do pacote saem apenas AGUA e PISO — as duas coisas que a cisterna
            // precisa e que o jogo nao tem. Todo o resto e repintado do proprio
            // acervo, logo abaixo.
            new Peca("cisternWater", 8, 0),
            new Peca("cisternFloor", 0, 5),
            new Peca("cisternFloorCracked", 1, 5),
            new Peca("cisternFloorWorn", 2, 5),
            new Peca("cisternChannelV", 12, 6),
            new Peca("cisternChannelH", 14, 8),
            new Peca("cisternChannelCross", 14, 6),
            new Peca("cisternSink", 12, 8),
    };

    /**
     * Tiles do PROPRIO JOGO, repintados para a cisterna.
     *
     * POR QUE REPINTAR EM VEZ DE IMPORTAR — e a licao da primeira tentativa.
     *
     * As paredes vinham do pacote de fora, e nao funcionaram por duas razoes
     * independentes. A cor era uma: alvenaria cinza-esverdeada de outro artista ao
     * lado do piso desta arena nao fecha paleta com nada. A GEOMETRIA era a outra,
     * e pior: o tile de parede daquele pacote foi desenhado com a agua encostando
     * no PE dele. Empilhado numa coluna vertical, aquele rodape de agua se repetia
     * a cada dezesseis pixels e a parede saia listrada de verde.
     *
     * Os tiles daqui nao tem esse problema porque foram feitos para este jogo: o
     * topo e a face sao pecas separadas, e a parede se monta com as duas. Repintar
     * herda a geometria certa de graca — e, de quebra, a cisterna passa a ser
     * reconhecivel como o MESMO castelo da antecamara, so que umido, em vez de um
     * lugar emprestado de outro jogo.
     */
    private record Repintura(String origem, String saida) {
    }

    private static final Repintura[] REPINTURAS = {
            new Repintura("stoneTop", "cisternStoneTop"),
            new Repintura("stoneFace", "cisternStoneFace"),
            new Repintura("stoneGrate", "cisternGrate"),
    };

    /**
     * A escada de cor da cisterna, do fundo para a luz.
     *
     * E a mesma escada de pedra da antecamara empurrada para o verde e escurecida:
     * limo sobre alvenaria molhada. Nao e uma paleta nova — e a de sempre, com
     * umidade — e por isso as duas salas continuam parecendo o mesmo castelo.
     */
    /**
     * Escada do entulho que fica no chao.
     *
     * Vai do quase preto ate o tom da propria laje, passando por um verde acinzentado
     * de limo. Ela e mais clara e mais espalhada que a da alvenaria de proposito:
     * peca no chao compete com um piso claro, e nao com a sombra de uma parede.
     */
    private static final int[] CHAO = {
            0x24261f, 0x3b3e33, 0x55584a, 0x6e7260, 0x878a76, 0x9d9f8c,
    };

    private static final int[] RAMPA = {
            0x131a16, 0x1e2a22, 0x2b3a2f, 0x3c503f, 0x52684f, 0x6d8266,
    };

    /**
     * As cores do mapa. Tem de bater EXATAMENTE com TileIDs.
     *
     * O PNG do mapa nao guarda numero de tile, guarda cor: cada pixel e um tile e
     * a cor diz qual. Errar um valor aqui nao quebra o build — carrega a sala com
     * o tile errado, ou com vazio — entao os dois lados existem em um lugar so,
     * este mapa, e o gerador reclama se algum ficar de fora.
     */
    private static final Map<String, Integer> COR = new LinkedHashMap<>();

    static {
        COR.put("cisternWater", 0xff2f6b3d);
        COR.put("cisternFloor", 0xff8f8b7a);
        COR.put("cisternFloorCracked", 0xff84806f);
        COR.put("cisternFloorWorn", 0xff7a7666);
        COR.put("cisternChannelV", 0xff565349);
        COR.put("cisternChannelH", 0xff525046);
        COR.put("cisternChannelCross", 0xff4e4c43);
        COR.put("cisternSink", 0xff4a4840);
        COR.put("cisternStoneTop", 0xff8a8f92);
        COR.put("cisternStoneFace", 0xff5c6165);
        COR.put("cisternGrate", 0xff24312b);
        COR.put("cisternSkull", 0xff6b6f5e);
        COR.put("cisternFlaskBig", 0xff5b5f4e);
        COR.put("cisternPlate", 0xff676b5a);
        COR.put("cisternFlask", 0xff5f6352);
        COR.put("void", 0xff000000);
    }

    public static void main(String[] args) throws Exception {
        File folha = new File(FONTE);
        if (!folha.isFile()) {
            System.err.println("Falta o pacote: " + folha.getPath());
            System.err.println("Ver docs/TERCEIROS.md para de onde baixar.");
            System.exit(1);
        }
        BufferedImage sheet = ImageIO.read(folha);

        System.out.println("Tiles da cisterna");
        for (Peca p : PECAS) {
            BufferedImage tile = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            tile.getGraphics().drawImage(
                    sheet.getSubimage(p.col() * 16, p.lin() * 16, 16, 16), 0, 0, null);
            ImageIO.write(tile, "png", new File(TILES + p.saida() + ".png"));
            System.out.println("  sprites/objects/tile/" + p.saida() + ".png");
        }

        repinturas();
        plantas();
        porta();
    }

    /**
     * A porta da cisterna, montada no formato que o Gate espera.
     *
     * O Gate do jogo desenha uma folha de OITO quadros de tres por tres tiles — e
     * a grade da antecamara, que sobe. A porta deste pacote e outra coisa: um
     * batente de UM tile em quatro quadros, que abre para dentro. Em vez de
     * reescrever o Gate para dois formatos, a porta e MONTADA aqui no formato dele:
     * o batente do pacote no meio, alvenaria do proprio pacote nas jambas e na
     * verga, e os quatro quadros de abertura distribuidos nos oito.
     *
     * Isto e montagem, nao desenho: cada pixel vem da folha original. E o mesmo
     * que o gerador das armas ja faz — recorta e compoe, nunca inventa.
     */
    private static void porta() throws Exception {
        BufferedImage folha = ImageIO.read(new File(FONTE));
        int t = 16;
        int quadros = 8;
        BufferedImage saida = new BufferedImage(quadros * t * 3, t * 3,
                BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics g = saida.getGraphics();

        // Os quatro estagios do batente, do fechado ao aberto.
        int[] estagio = {16, 17, 18, 19};
        for (int q = 0; q < quadros; q++) {
            int base = q * t * 3;
            // Jambas e verga: alvenaria seca do mesmo pacote, para a porta parecer
            // encaixada na parede em vez de colada nela.
            for (int cx = 0; cx < 3; cx++) {
                for (int cy = 0; cy < 3; cy++) {
                    boolean vao = cx == 1 && cy >= 1;
                    int col = vao ? estagio[Math.min(estagio.length - 1, q * estagio.length / quadros)] : 1;
                    int lin = vao ? 16 : (cy == 0 ? 0 : 1);
                    g.drawImage(folha.getSubimage(col * t, lin * t, t, t),
                            base + cx * t, cy * t, null);
                }
            }
        }
        g.dispose();
        String mobilia = "src/main/resources/com/retronova/resources/sprites/objects/furniture/";
        ImageIO.write(saida, "png", new File(mobilia + "gateCistern.png"));
        System.out.println("Porta");
        System.out.println("  sprites/objects/furniture/gateCistern.png");

        // A MESMA PORTA PARA A ANTECAMARA, na paleta fria dela.
        //
        // A grade de ferro que estava la tinha o mesmo defeito da folha girando no
        // chao: ela subia, e uma grade subindo em vista de cima le como um objeto
        // se levantando do piso. O vao escuro com luz saindo funciona nos dois
        // lugares, e usar a mesma peca nas duas salas faz "passagem aberta" ter uma
        // aparencia so no jogo inteiro — o jogador aprende uma vez.
        BufferedImage fria = new BufferedImage(saida.getWidth(), saida.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        int[] rampaFria = {0x141a20, 0x202c33, 0x2f434a, 0x3f5f64, 0x4a6b70, 0x6d8f92};
        for (int y = 0; y < saida.getHeight(); y++) {
            for (int x = 0; x < saida.getWidth(); x++) {
                int px = saida.getRGB(x, y);
                int a = px >>> 24;
                if (a == 0) {
                    continue;
                }
                int cr = (px >> 16) & 0xff, cg = (px >> 8) & 0xff, cb = px & 0xff;
                double luz = (0.299 * cr + 0.587 * cg + 0.114 * cb) / 255.0;
                int degrau = Math.max(0, Math.min(rampaFria.length - 1,
                        (int) Math.round(luz * (rampaFria.length - 1) * 1.35)));
                fria.setRGB(x, y, (a << 24) | rampaFria[degrau]);
            }
        }
        ImageIO.write(fria, "png", new File(mobilia + "gateStone.png"));
        System.out.println("  sprites/objects/furniture/gateStone.png");
    }

    /**
     * Repinta um tile do jogo na escada da cisterna.
     *
     * A conversao e por LUMINOSIDADE, e nao cor a cor: cada pixel e medido pelo
     * quanto ele e claro e recebe o degrau equivalente da escada nova. E o que
     * preserva o relevo — a ordem das sombras e o que da volume a uma parede, e
     * trocar cor por cor sem respeitar essa ordem achata o desenho. E o mesmo
     * metodo do gerador das armas elementais, pela mesma razao.
     */
    private static void repinturas() throws Exception {
        System.out.println("Repinturas do acervo");
        for (Repintura r : REPINTURAS) {
            BufferedImage origem = ImageIO.read(new File(TILES + r.origem() + ".png"));
            BufferedImage saida = new BufferedImage(origem.getWidth(), origem.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < origem.getHeight(); y++) {
                for (int x = 0; x < origem.getWidth(); x++) {
                    int p = origem.getRGB(x, y);
                    int a = p >>> 24;
                    if (a == 0) {
                        continue;
                    }
                    int cr = (p >> 16) & 0xff, cg = (p >> 8) & 0xff, cb = p & 0xff;
                    double luz = (0.299 * cr + 0.587 * cg + 0.114 * cb) / 255.0;
                    int degrau = (int) Math.round(luz * (RAMPA.length - 1) * 1.35);
                    degrau = Math.max(0, Math.min(RAMPA.length - 1, degrau));
                    saida.setRGB(x, y, (a << 24) | RAMPA[degrau]);
                }
            }
            ImageIO.write(saida, "png", new File(TILES + r.saida() + ".png"));
            System.out.println("  " + r.origem() + " -> " + r.saida());
        }
        alavanca();
    }

    /**
     * A alavanca, na mesma escada.
     *
     * Ela e mobilia da antecamara, azul-acinzentada, e do lado da porta da cisterna
     * aparecia como a unica peca fria da cena. Repintada, ela passa a pertencer ao
     * lugar sem que ninguem precise redesenhar o mecanismo.
     */
    private static void alavanca() throws Exception {
        String mobilia = "src/main/resources/com/retronova/resources/sprites/objects/furniture/";
        BufferedImage origem = ImageIO.read(new File(mobilia + "lever.png"));
        BufferedImage saida = new BufferedImage(origem.getWidth(), origem.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < origem.getHeight(); y++) {
            for (int x = 0; x < origem.getWidth(); x++) {
                int p = origem.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) {
                    continue;
                }
                int cr = (p >> 16) & 0xff, cg = (p >> 8) & 0xff, cb = p & 0xff;
                // O CABO DE MADEIRA FICA COMO ESTA. Ele e o unico ponto quente da
                // peca e e por ele que a alavanca se le como algo de puxar; virar
                // tudo verde apagaria o proprio mecanismo.
                boolean madeira = cr > cb + 20;
                if (madeira) {
                    saida.setRGB(x, y, p);
                    continue;
                }
                double luz = (0.299 * cr + 0.587 * cg + 0.114 * cb) / 255.0;
                int degrau = (int) Math.round(luz * (RAMPA.length - 1) * 1.35);
                degrau = Math.max(0, Math.min(RAMPA.length - 1, degrau));
                saida.setRGB(x, y, (a << 24) | RAMPA[degrau]);
            }
        }
        ImageIO.write(saida, "png", new File(mobilia + "leverCistern.png"));
        System.out.println("  lever -> leverCistern");
        mobiliaDaCisterna();
        portalDaCisterna();
        entulhoDoAcervo();
    }

    /**
     * ENTULHO DE VERDADE, do DungeonTileset II, repintado para a cisterna.
     *
     * A queixa era justa: a sala tinha chao, parede e tres montinhos nos mesmos
     * cantos das seis plantas. O saguao e cheio de coisa e por isso parece um
     * lugar; a arena parecia um campo.
     *
     * A ARTE VEM DO PACOTE QUE O JOGO JA USA. O DungeonTileset II do 0x72 e CC0, e
     * dele ja saem as armas antigas — entao ele ja esta baixado e ja esta creditado.
     * Tem exatamente o que faltava: caveira, caixote, coluna, buraco no chao,
     * frascos, botao de pressao, escada e degraus.
     *
     * Considerei um pacote de itch com noventa e sete pecas de masmorra, tambem CC0
     * e com mais variedade. Nao usei: o proprio autor marca como GERADO POR IA. O
     * resto do cenario deste jogo e desenhado por gente, e misturar as duas coisas
     * aparece — nao no primeiro olhar, mas na hora em que uma peca nao encaixa e
     * ninguem sabe dizer por que.
     */
    private static void entulhoDoAcervo() throws Exception {
        String frames = "tools/assets/fonte/dtii/0x72_DungeonTilesetII_v1.7/frames/";
        String[][] pecas = {
                // SO O QUE SOBREVIVE AO TRANSPLANTE.
                //
                // Buraco, escada e degraus tambem foram tentados e sairam fora: no
                // pacote de origem eles PREENCHEM o quadro de marrom escuro, porque
                // la o piso e escuro. Repintados sobre a laje clara daqui viram
                // retangulos sem leitura — o desenho deles e feito de vazio, e vazio
                // nao se repinta. Ficaram os que tem silhueta propria.
                {"skull", "cisternSkull"},
                {"button_blue_up", "cisternPlate"},
                {"flask_green", "cisternFlask"},
                {"flask_big_blue", "cisternFlaskBig"},
        };
        System.out.println("Entulho");
        for (String[] peca : pecas) {
            File origem = new File(frames + peca[0] + ".png");
            if (!origem.isFile()) {
                System.err.println("  falta " + origem.getPath());
                continue;
            }
            BufferedImage im = ImageIO.read(origem);
            BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            // O tile de chao da cisterna vai ATRAS, para a peca nao ficar recortada
            // sobre o vazio: sao tiles, e tile nao tem transparencia util aqui.
            BufferedImage chao = ImageIO.read(new File(TILES + "cisternFloor.png"));
            out.getGraphics().drawImage(chao, 0, 0, null);
            for (int y = 0; y < Math.min(16, im.getHeight()); y++) {
                for (int x = 0; x < Math.min(16, im.getWidth()); x++) {
                    int px = im.getRGB(x, y);
                    int a = px >>> 24;
                    if (a < 16) {
                        continue;
                    }
                    int cr = (px >> 16) & 0xff, cg = (px >> 8) & 0xff, cb = px & 0xff;
                    // ESCADA DE CHAO, e nao a da parede.
                    //
                    // A primeira tentativa passou o entulho pela mesma escada verde
                    // escura da alvenaria, com um empurrao para o escuro por cima.
                    // O resultado foi um borrao: buraco, escada e frasco viraram
                    // manchas iguais. Entulho fica SOBRE o piso claro, entao precisa
                    // de uma escada que va do quase preto ao tom da laje — e sem
                    // empurrao, porque o que faz uma peca ser reconhecivel e a
                    // distancia entre a sombra e a luz dela, e o empurrao come
                    // justamente a luz.
                    double luz = (0.299 * cr + 0.587 * cg + 0.114 * cb) / 255.0;
                    int degrau = Math.max(0, Math.min(CHAO.length - 1,
                            (int) Math.round(luz * (CHAO.length - 1))));
                    out.setRGB(x, y, 0xff000000 | CHAO[degrau]);
                }
            }
            ImageIO.write(out, "png", new File(TILES + peca[1] + ".png"));
            System.out.println("  " + peca[0] + " -> " + peca[1]);
        }
    }

    /**
     * O portal de chegada, na agua da sala.
     *
     * O desenho original e rosa e amarelo — a paleta de um portal magico generico,
     * que ao lado da agua esverdeada desta arena parece de outro jogo. E a mesma
     * queixa das paredes e da mobilia, e a mesma solucao.
     *
     * SO QUE COM OUTRA ESCADA. As paredes usam a pedra umida, escura; o portal
     * precisa BRILHAR — ele e a coisa mais clara da cena no instante em que abre.
     * Entao a escada dele sobe do verde da agua ate quase o branco, e o miolo
     * continua sendo o ponto de luz que o desenho original tinha.
     */
    private static void portalDaCisterna() throws Exception {
        int[] rampaDeAgua = {
                0x1d3a2a, 0x2f6b3d, 0x548257, 0x79b57a, 0xa8e0a0, 0xe8ffe4,
        };
        String mobilia = "src/main/resources/com/retronova/resources/sprites/objects/furniture/";
        BufferedImage im = ImageIO.read(new File(mobilia + "portal.png"));
        BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int px = im.getRGB(x, y);
                int a = px >>> 24;
                if (a == 0) {
                    continue;
                }
                int cr = (px >> 16) & 0xff, cg = (px >> 8) & 0xff, cb = px & 0xff;
                double luz = (0.299 * cr + 0.587 * cg + 0.114 * cb) / 255.0;
                int degrau = Math.max(0, Math.min(rampaDeAgua.length - 1,
                        (int) Math.round(luz * (rampaDeAgua.length - 1))));
                out.setRGB(x, y, (a << 24) | rampaDeAgua[degrau]);
            }
        }
        ImageIO.write(out, "png", new File(mobilia + "portalCistern.png"));
        System.out.println("  portal -> portalCistern");
    }

    /**
     * A MOBILIA DA SALA, repintada do proprio acervo.
     *
     * O pedido era "mais detalhe no cenario", e a primeira resposta foi importar
     * caixotes e cascalho de fora — que nao pertenceram ao lugar nem por cor nem por
     * desenho. A segunda resposta e a que ja tinha funcionado para as paredes:
     * pegar o que o jogo TEM e passar pela mesma escada de cor.
     *
     * E o acervo tem justamente o que uma cisterna abandonada pede — ossos,
     * correntes penduradas, entulho, barris e pilares. Nada disso precisou ser
     * desenhado nem baixado; precisava so parar de ser azul.
     *
     * O BRASEIRO E A EXCECAO PARCIAL: o fogo dele fica como esta, pela mesma razao
     * que o cabo da alavanca ficou. Repintar a chama de verde apagaria a unica
     * fonte de calor da sala, e e ela que faz o resto do verde parecer frio.
     */
    private static void mobiliaDaCisterna() throws Exception {
        String mobilia = "src/main/resources/com/retronova/resources/sprites/objects/furniture/";
        String[] pecas = {"pillar", "barrel", "bones", "chain", "rubble", "brazier"};
        for (String peca : pecas) {
            File origem = new File(mobilia + peca + ".png");
            if (!origem.isFile()) {
                continue;
            }
            BufferedImage im = ImageIO.read(origem);
            BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < im.getHeight(); y++) {
                for (int x = 0; x < im.getWidth(); x++) {
                    int px = im.getRGB(x, y);
                    int a = px >>> 24;
                    if (a == 0) {
                        continue;
                    }
                    int cr = (px >> 16) & 0xff, cg = (px >> 8) & 0xff, cb = px & 0xff;
                    // Vermelho e laranja acesos sao fogo: passam intactos.
                    if (cr > cb + 60 && cr > 120) {
                        out.setRGB(x, y, px);
                        continue;
                    }
                    double luz = (0.299 * cr + 0.587 * cg + 0.114 * cb) / 255.0;
                    int degrau = Math.max(0, Math.min(RAMPA.length - 1,
                            (int) Math.round(luz * (RAMPA.length - 1) * 1.35)));
                    out.setRGB(x, y, (a << 24) | RAMPA[degrau]);
                }
            }
            String nome = peca + "Cistern";
            ImageIO.write(out, "png", new File(mobilia + nome + ".png"));
            System.out.println("  " + peca + " -> " + nome);
        }
    }

    /** Lado da arena, em tiles. */
    private static final int LADO = 34;

    /** O centro da sala, em tiles. */
    private static final int MEIO = LADO / 2;

    /**
     * QUANTAS ARENAS, e de onde sai esse numero.
     *
     * A corrida precisa de varias salas do mesmo tema, e nao de uma repetida — foi
     * o que a primeira versao fazia: NEXT ARENA reconstruia sempre o mesmo mapa, e
     * o quinto turno era visualmente identico ao primeiro.
     *
     * O tamanho do lote vem de como o genero se organiza. Em Hades, Tartaro tem
     * quinze camaras e Asfodelo dez; em Slay the Spire sao quinze a dezessete
     * andares por ato, com a corrida inteira em torno de quarenta e cinco minutos a
     * uma hora; em Isaac, vinte e cinco minutos a uma hora. O padrao que se repete
     * e: TRES A CINCO regioes por corrida, DEZ A QUINZE salas por regiao.
     *
     * Aqui cada sala e uma arena inteira de ondas, mais longa que uma camara de
     * Hades, entao a conta e menor: SEIS layouts para a cisterna. Seis cobre um
     * bloco de turnos sem repetir de cara e ainda e um numero que da para desenhar
     * com cuidado — variedade que ninguem olha nao vale o custo de manter.
     */
    private static final int QUANTAS = 6;

    /**
     * A variante em desenho. Sai do laco de geracao e nao e sorteada: a mesma
     * chamada do gerador tem de produzir os mesmos seis mapas, sempre.
     */
    private static int variante;

    private static void plantas() throws Exception {
        System.out.println("Plantas");
        for (int v = 0; v < QUANTAS; v++) {
            variante = v;
            BufferedImage mapa = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < LADO; y++) {
                for (int x = 0; x < LADO; x++) {
                    mapa.setRGB(x, y, COR.get(tileEm(x, y)));
                }
            }
            String nome = "cistern_" + (v + 1);
            ImageIO.write(mapa, "png", new File(MAPAS + nome + ".png"));
            io(nome);
            System.out.println("  maps/" + nome + ".png  saida ao " + ladoDaSaida());
        }
    }

    /**
     * De que parede sai o corredor, por variante.
     *
     * QUALQUER PAREDE SERVE, e isso e consequencia de nao haver desenho de porta.
     * Enquanto a saida era uma folha vista de frente, so a parede de baixo
     * funcionava; um corredor e so chao cavado na alvenaria, e chao nao tem lado
     * certo. Variar a saida muda o caminho que o jogador faz depois da briga, que e
     * a diferenca mais barata e mais sentida entre uma sala e outra.
     *
     * 0 sul, 1 norte, 2 leste, 3 oeste.
     */
    private static int ladoDaSaida() {
        return variante % 4;
    }

    /** O corredor de saida, cavado da plataforma ate a borda do mapa. */
    private static boolean noCorredor(int x, int y) {
        switch (ladoDaSaida()) {
            case 1:
                return x == MEIO && y <= 3;
            case 2:
                return y == MEIO && x >= LADO - 4;
            case 3:
                return y == MEIO && x <= 3;
            default:
                return x == MEIO && y >= LADO - 4;
        }
    }

    /** A passarela que cruza a agua ate o corredor. */
    private static boolean naPonte(int x, int y) {
        switch (ladoDaSaida()) {
            case 1:
                return x == MEIO && y > 3 && y < 10;
            case 2:
                return y == MEIO && x < LADO - 4 && x > LADO - 11;
            case 3:
                return y == MEIO && x > 3 && x < 10;
            default:
                return x == MEIO && y < LADO - 4 && y > LADO - 11;
        }
    }

    private static String tileEm(int x, int y) {
        int borda = Math.min(Math.min(x, y), Math.min(LADO - 1 - x, LADO - 1 - y));

        if (noCorredor(x, y) || naPonte(x, y)) {
            return "cisternFloor";
        }
        if (borda == 0) {
            return "cisternStoneTop";
        }
        if (borda == 1) {
            // A grade so na parede de cima, onde o desenho dela — visto de frente,
            // despejando para baixo — faz sentido.
            if (y == 1 && x % 9 == 4) {
                return "cisternGrate";
            }
            return "cisternStoneFace";
        }

        // ----------------------------------------------------------- a agua
        //
        // Cada variante recebe uma FASE diferente na ondulacao da margem, e uma
        // largura media propria. E a mudanca que mais se nota entrando na sala:
        // uma cisterna quase seca e outra quase alagada sao lugares diferentes,
        // mesmo com a mesma paleta e os mesmos tiles.
        int aoLongo = (x <= y && x <= LADO - 1 - y) || (LADO - 1 - x <= y && LADO - 1 - x <= LADO - 1 - y)
                ? Math.min(y, LADO - 1 - y)
                : Math.min(x, LADO - 1 - x);
        double fase = variante * 0.9;
        int base = 2 + variante % 3;
        int margem = base + (int) (2 * Math.abs(Math.sin(aoLongo * 0.42 + fase)));
        if (borda <= margem) {
            if (pilar(x, y)) {
                return borda == margem ? "cisternStoneFace" : "cisternStoneTop";
            }
            return "cisternWater";
        }

        // ------------------------------------------------------ plataforma
        //
        // O desenho das canaletas tambem muda: umas salas tem a cruz inteira, outras
        // so um eixo, outras nenhuma. Sao o unico traco forte do piso, entao mexer
        // nelas muda a leitura do centro sem mexer em mais nada.
        boolean cruz = variante % 3 != 2;
        boolean soUmEixo = variante % 3 == 1;
        if (cruz) {
            if (x == MEIO && y == MEIO) {
                return "cisternSink";
            }
            boolean bracoH = y == MEIO && Math.abs(x - MEIO) <= 7;
            boolean bracoV = x == MEIO && Math.abs(y - MEIO) <= 7;
            if (soUmEixo) {
                bracoV = false;
            }
            if (bracoH && bracoV) {
                return "cisternChannelCross";
            }
            if (bracoH) {
                return "cisternChannelH";
            }
            if (bracoV) {
                return "cisternChannelV";
            }
        }

        // ------------------------------------------------------ o entulho
        //
        // VARIA POR PLANTA, e essa e a correcao. Antes eram tres montinhos nos
        // mesmos cantos das seis salas: variedade que se repete nao e variedade.
        // Agora a semente entra na conta, entao cada planta espalha pecas
        // diferentes em lugares diferentes.
        //
        // Continua longe do centro e do caminho — entulho e para dar vida ao chao,
        // nao para atrapalhar a briga nem tapar a saida.
        int doCentro = Math.max(Math.abs(x - MEIO), Math.abs(y - MEIO));
        if (doCentro > 7 && !noCorredor(x, y) && !naPonte(x, y)) {
            int mistura = (x * 73856093) ^ (y * 19349663) ^ ((variante + 1) * 83492791);
            mistura = (mistura >>> 7) ^ mistura;
            int sorte = Math.floorMod(mistura, 100);
            String[] entulho = {"cisternSkull", "cisternSkull", "cisternPlate",
                    "cisternFlask", "cisternFlaskBig"};
            if (sorte < 6) {
                return entulho[Math.floorMod(mistura >> 3, entulho.length)];
            }
        }

        int variacao = (x * 7 + y * 13 + variante * 3) % 10;
        if (variacao < 6) {
            return "cisternFloor";
        }
        return variacao < 8 ? "cisternFloorCracked" : "cisternFloorWorn";
    }

    /**
     * Pilares plantados na agua. A quantidade e a posicao mudam por variante.
     */
    private static boolean pilar(int x, int y) {
        int recuo = 4 + variante % 2;
        int[][] pes = {
                {recuo, recuo + 1}, {LADO - 1 - recuo, recuo + 1},
                {recuo, LADO - 2 - recuo}, {LADO - 1 - recuo, LADO - 2 - recuo},
        };
        int quantos = variante % 2 == 0 ? pes.length : 2;
        for (int i = 0; i < quantos; i++) {
            if (x == pes[i][0] && (y == pes[i][1] || y == pes[i][1] + 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * O JSON da variante: nascimento, passagem, alavanca e tochas.
     *
     * Ele acompanha o lado da saida — a passagem fica na boca do corredor e a
     * alavanca encostada na parede ao lado dela. Escrito a mao seriam seis arquivos
     * para manter em sincronia com seis plantas; gerado, os dois nunca divergem.
     */
    private static void io(String nome) throws Exception {
        int px, py, lx, ly;
        switch (ladoDaSaida()) {
            case 1 -> {
                px = MEIO; py = 0; lx = MEIO + 2; ly = 1;
            }
            case 2 -> {
                px = LADO - 1; py = MEIO; lx = LADO - 2; ly = MEIO + 2;
            }
            case 3 -> {
                px = 0; py = MEIO; lx = 1; ly = MEIO + 2;
            }
            default -> {
                px = MEIO; py = LADO - 1; lx = MEIO + 2; ly = LADO - 2;
            }
        }
        StringBuilder j = new StringBuilder();
        j.append("{\n  \"spawn\": { \"x\": ").append(MEIO).append(", \"y\": ").append(MEIO).append(" },\n\n");
        j.append("  \"entities\": [\n");
        j.append("    { \"name\": \"Gate\", \"x\": ").append(px).append(", \"y\": ").append(py)
                .append(", \"values\": [\"NEXT ARENA\", \"none\"] },\n");
        j.append("    { \"name\": \"Lever\", \"x\": ").append(lx).append(", \"y\": ").append(ly)
                .append(", \"values\": [\"leverCistern\"] }");
        // ------------------------------------------------------- os cantinhos
        //
        // "Um lugarzinho diferente": a sala precisa de pontos que nao sejam chao
        // nem parede. Sao TRES agrupamentos, e agrupados de proposito — mobilia
        // espalhada de um em um vira sujeira no chao; junta, ela conta uma cena.
        // Um canto desabado com entulho e ossos, uma pilha de barris encostada na
        // parede, e correntes penduradas do outro lado.
        //
        // Todos ficam a mais de sete tiles do centro: o meio da sala e onde a briga
        // acontece e tem de continuar limpo.
        int[][][] cantos = {
                {{7, 7}, {8, 7}, {7, 8}},
                {{LADO - 9, LADO - 8}, {LADO - 8, LADO - 8}, {LADO - 8, LADO - 9}},
                {{7, LADO - 8}, {LADO - 8, 7}},
        };
        String[][] pecas = {
                {"Rubble", "rubbleCistern"},
                {"Bones", "bonesCistern"},
                {"Barrel", "barrelCistern"},
        };
        for (int c = 0; c < cantos.length; c++) {
            for (int[] onde : cantos[c]) {
                if (noCorredor(onde[0], onde[1]) || naPonte(onde[0], onde[1])) {
                    continue;
                }
                String[] peca = pecas[(c + variante) % pecas.length];
                j.append(",\n    { \"name\": \"").append(peca[0])
                        .append("\", \"x\": ").append(onde[0])
                        .append(", \"y\": ").append(onde[1])
                        .append(", \"values\": [\"").append(peca[1]).append("\"] }");
            }
        }
        // Correntes penduradas NA PAREDE, nunca no chao: elas descem do teto.
        int[][] correntes = {{5, 1}, {LADO - 6, 1}, {MEIO - 4, 1}};
        for (int[] onde : correntes) {
            if (noCorredor(onde[0], onde[1])) {
                continue;
            }
            j.append(",\n    { \"name\": \"Chain\", \"x\": ").append(onde[0])
                    .append(", \"y\": ").append(onde[1])
                    .append(", \"values\": [\"chainCistern\"] }");
        }

        int[][] tochas = {
                {1, 8}, {1, MEIO}, {1, LADO - 9},
                {LADO - 2, 8}, {LADO - 2, MEIO}, {LADO - 2, LADO - 9},
                {8, 1}, {MEIO, 1}, {LADO - 9, 1},
                {8, LADO - 2}, {MEIO, LADO - 2}, {LADO - 9, LADO - 2},
        };
        for (int[] t : tochas) {
            // Tocha em cima do corredor tampa a saida; a boca dele fica limpa.
            if (noCorredor(t[0], t[1]) || (t[0] == lx && t[1] == ly)) {
                continue;
            }
            j.append(",\n    { \"name\": \"Torch\", \"x\": ").append(t[0])
                    .append(", \"y\": ").append(t[1]).append(", \"values\": [] }");
        }
        j.append("\n  ]\n}\n");
        java.nio.file.Files.writeString(
                java.nio.file.Path.of(MAPAS + nome + ".json"), j.toString());
    }

}
