import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Reconstroi as armas a partir do DungeonTileset II, encurtadas para o gato.
 *
 * ------------------------------------------------------------------------------
 * POR QUE AS ARMAS ANTIGAS INCOMODAVAM
 *
 * Nao era o comprimento, era o VOLUME. Elas eram diagonais gordas enchendo o
 * quadrado de 16 — 22px de diagonal contra 13px de corpo de gato, 168%. As deste
 * pack sao verticais e finas, e mesmo mais compridas leem bem mais leves.
 *
 * A medida que decidiu o alvo veio da propria arte oficial do jogo: na
 * ilustracao do titulo a espada que o Muffin carrega tem cerca de 55% da altura
 * do corpo dele. E essa a direcao de arte da casa, e e o que se persegue aqui.
 *
 * ------------------------------------------------------------------------------
 * COMO ELAS SAO ENCURTADAS, E POR QUE ASSIM
 *
 * Tres caminhos foram testados antes deste:
 *
 *  - REDUZIR A ESCALA pela metade destroi o desenho. O arco sumiu, a espada
 *    virou um risco de cinco pixels, a maca perdeu a cabeca. Reduzir joga fora
 *    metade dos pixels, e em 16px nao ha pixel sobrando.
 *  - CORTAR O CABO salva machado, maca e martelo, e arruina espada e arco: sem
 *    guarda e sem punho uma espada vira uma lasca de metal.
 *  - REMOVER AS LINHAS REDUNDANTES funciona, e e o que um pixel artist faz. A
 *    lamina de uma espada e a haste de um machado sao linhas repetidas que nao
 *    carregam informacao nenhuma; guarda, gume, ponta e punho carregam. Some a
 *    repeticao, fica o desenho.
 *
 * A escolha de qual linha remover e automatica, e ESPALHADA: a altura e dividida
 * em tantas faixas quantas forem as linhas a tirar, e de cada faixa sai a linha
 * mais parecida com a vizinha. Escolher sempre a mais barata do sprite inteiro
 * nao funcionou — lamina e cabo sao ambos linhas identicas, o algoritmo comia a
 * lamina toda e deixava a guarda, e saia um martelo. Espalhando, lamina e cabo
 * encolhem juntos. Mudanca de silhueta tem peso alto, entao ponta e gume ficam.
 *
 * ------------------------------------------------------------------------------
 * O QUADRO E 16x16 PORQUE O MOTOR EXIGE
 *
 * Item.java conta os quadros por largura/16 e pede cada um por
 * getSpriteWithIndex, que fixa 16*escala. Nao e preferencia: sprite de item que
 * nao for 16x16 e recortado errado. As armas do pack tem ate 37px de altura, e e
 * por isso que encurtar nao e opcional.
 *
 * A arma sai centrada na horizontal e assentada embaixo, que e onde os pontos de
 * rotacao dos itens ja esperam encontrar o punho.
 *
 * Uso: java tools/GenItens.java  (precisa do pack em tools/assets/fonte)
 */
public class GenItens {

    private static final String FONTE =
            "tools/assets/fonte/dtii/0x72_DungeonTilesetII_v1.7/frames/";
    private static final String DESTINO =
            "src/main/resources/com/retronova/resources/sprites/items/";

    /** Lado do quadro de item. Imposto por Item.java, ver o javadoc da classe. */
    private static final int QUADRO = 16;

    /**
     * De onde cada arma vem e que altura ela deve ter.
     *
     * O alvo varia por tipo, e nao e um numero so, porque arma de verdade tambem
     * varia: faca e curta, lanca e comprida. O que todas respeitam e caber no
     * quadro e nao passar muito da altura do gato.
     */
    record Arma(String item, String fonte, int altura, String pasta) {
        Arma(String item, String fonte, int altura) {
            this(item, fonte, altura, DESTINO);
        }
    }

    /** A flecha nao e item de inventario: vive junto com os utilitarios. */
    private static final String UTILITARIOS =
            "src/main/resources/com/retronova/resources/sprites/objects/utility/";

    /*
     * O que NAO tem equivalente no pack, e por isso continua com a arte antiga:
     * racao, bolota, erva-de-gato, melancia, seda, orbe magnetico, bola de pelo e
     * o laser. Sao comida, material e ficcao cientifica; o DungeonTileset II e um
     * pacote de masmorra medieval e simplesmente nao tem nada disso. Vale notar
     * que o incomodo original tambem nao se aplica a eles: consumivel nunca
     * aparece na mao do gato, so no slot, e no slot encher o quadro esta certo.
     */

    private static final Arma[] ARMAS = {
            // corpo a corpo, na altura do gato ou abaixo
            new Arma("kunai", "weapon_knife", 10),
            new Arma("sword", "weapon_regular_sword", 12),
            new Arma("swordfire", "weapon_lavish_sword", 13),
            new Arma("sickle", "weapon_machete", 12),
            new Arma("claw_blades", "weapon_saw_sword", 12),
            new Arma("bloody_axe", "weapon_waraxe", 12),
            // haste e arco podem passar: sao compridos por natureza
            new Arma("trident", "weapon_spear", 15),
            new Arma("bow", "weapon_bow", 14),
            new Arma("boweletric", "weapon_bow_2", 14),
            new Arma("magicstick", "weapon_red_magic_staff", 15),
            new Arma("dangerouswand", "weapon_green_magic_staff", 15),
            // arremessaveis: ja nascem pequenos, nao precisam encurtar
            new Arma("bomb", "bomb_f0", 13),
            new Arma("gasbomb", "flask_green", 11),
            // A flecha era do tamanho do quadro inteiro e saia maior que o arco
            // que a disparava. Nove pixels e o comprimento que ela tem no pack.
            new Arma("arrow", "weapon_arrow", 9, UTILITARIOS),
    };

    public static void main(String[] a) throws Exception {
        File fonte = new File(FONTE);
        if (!fonte.isDirectory()) {
            System.err.println("Pack nao encontrado em " + FONTE);
            System.err.println("Baixe o DungeonTileset II (CC0) e descompacte em tools/assets/fonte.");
            System.exit(1);
        }
        for (Arma arma : ARMAS) {
            File origem = new File(FONTE + arma.fonte() + ".png");
            if (!origem.isFile()) {
                System.err.println("  FALTA no pack: " + arma.fonte());
                continue;
            }
            BufferedImage cheia = recortar(ImageIO.read(origem));
            BufferedImage curta = encolher(cheia, arma.altura());
            BufferedImage quadro = noQuadro(curta);
            ImageIO.write(quadro, "png", new File(arma.pasta() + arma.item() + ".png"));
            System.out.printf("  %-16s %-26s %2d -> %2d px%n",
                    arma.item() + ".png", arma.fonte(), cheia.getHeight(), curta.getHeight());
        }
        System.out.println(ARMAS.length + " armas reconstruidas");
    }

    /** Tira a moldura vazia em volta do desenho. */
    static BufferedImage recortar(BufferedImage im) {
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
        return x1 < 0 ? im : im.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }

    /**
     * Encurta removendo linhas redundantes, ESPALHADAS ao longo da arma.
     *
     * Escolher sempre a linha globalmente mais barata nao funciona: a lamina de
     * uma espada e feita de linhas identicas e o cabo tambem, entao o algoritmo
     * comia a lamina inteira e deixava a guarda intacta — saia um martelo. O
     * remedio e dividir a altura em tantas faixas quantas forem as linhas a tirar
     * e remover UMA de cada faixa. Assim lamina e cabo encolhem juntos e a arma
     * so fica menor, nao deformada.
     */
    static BufferedImage encolher(BufferedImage im, int alvo) {
        int altura = im.getHeight();
        if (altura <= alvo || altura <= 3) {
            return im;
        }
        int aRemover = altura - alvo;
        boolean[] fora = new boolean[altura];

        // A primeira e as duas ultimas ficam sempre: sao a ponta e o punho.
        int primeira = 1, ultima = altura - 2;
        double faixa = (ultima - primeira) / (double) aRemover;
        for (int k = 0; k < aRemover; k++) {
            int de = primeira + (int) Math.floor(k * faixa);
            int ate = primeira + (int) Math.floor((k + 1) * faixa);
            int escolhida = -1;
            long menor = Long.MAX_VALUE;
            for (int y = de; y < Math.max(de + 1, ate) && y < ultima; y++) {
                if (fora[y]) {
                    continue;
                }
                long custo = diferenca(im, y, y + 1);
                if (custo < menor) {
                    menor = custo;
                    escolhida = y;
                }
            }
            if (escolhida >= 0) {
                fora[escolhida] = true;
            }
        }

        int sobram = 0;
        for (int y = 0; y < altura; y++) {
            if (!fora[y]) {
                sobram++;
            }
        }
        BufferedImage o = new BufferedImage(im.getWidth(), sobram, BufferedImage.TYPE_INT_ARGB);
        int destino = 0;
        for (int y = 0; y < altura; y++) {
            if (fora[y]) {
                continue;
            }
            for (int x = 0; x < im.getWidth(); x++) {
                o.setRGB(x, destino, im.getRGB(x, y));
            }
            destino++;
        }
        return o;
    }

    /** Quanto custa considerar duas linhas equivalentes. */
    static long diferenca(BufferedImage im, int a, int b) {
        long soma = 0;
        for (int x = 0; x < im.getWidth(); x++) {
            int p = im.getRGB(x, a), q = im.getRGB(x, b);
            if (p == q) {
                continue;
            }
            boolean pOpaco = (p >>> 24) >= 128, qOpaco = (q >>> 24) >= 128;
            if (pOpaco != qOpaco) {
                // A silhueta muda: e o gume, a ponta, o alargamento da guarda.
                // Peso alto para o algoritmo comer o meio da lamina primeiro.
                soma += 400;
                continue;
            }
            soma += Math.abs(((p >> 16) & 255) - ((q >> 16) & 255))
                    + Math.abs(((p >> 8) & 255) - ((q >> 8) & 255))
                    + Math.abs((p & 255) - (q & 255));
        }
        return soma;
    }

    /** Centrada na horizontal, assentada embaixo, dentro do quadro de 16. */
    static BufferedImage noQuadro(BufferedImage im) {
        BufferedImage o = new BufferedImage(QUADRO, QUADRO, BufferedImage.TYPE_INT_ARGB);
        int x = (QUADRO - im.getWidth()) / 2;
        int y = QUADRO - im.getHeight();
        for (int j = 0; j < im.getHeight(); j++) {
            for (int i = 0; i < im.getWidth(); i++) {
                int destinoX = x + i, destinoY = y + j;
                if (destinoX < 0 || destinoY < 0 || destinoX >= QUADRO || destinoY >= QUADRO) {
                    continue;
                }
                int p = im.getRGB(i, j);
                if ((p >>> 24) != 0) {
                    o.setRGB(destinoX, destinoY, p);
                }
            }
        }
        return o;
    }
}
