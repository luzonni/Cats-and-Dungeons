import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Os itens que DESENHAMOS, porque nenhum pacote de fora tinha um que servisse.
 *
 * Todo o resto do arsenal veio do DungeonTileset II (CC0). Estes dois nao:
 *
 *   ESCUDO   nenhum dos pacotes tem um. O unico candidato do Tiny Dungeon do
 *            Kenney (tile_0102) e um quadrado com um chanfro de dois pixels
 *            embaixo — recolorido para a paleta do jogo, lia como caixa.
 *
 * O TRIDENTE SAIU DAQUI. Ele foi desenhado aqui por um tempo porque a pesquisa
 * dizia que o pacote do Shade nao tinha nenhum. Tinha trinta, num grupo inteiro
 * que ninguem abriu — agora vem de la, por tools/GenElementais.java. O desenho a
 * mao continua no historico, se um dia fizer falta.
 *
 * A paleta e a mesma medida dos PNGs ja versionados, para os dois nao destoarem
 * do resto. A silhueta e nossa; o contorno e o aro sao CALCULADOS a partir dela,
 * e nao escritos a mao, senao qualquer ajuste na forma os deixa tortos.
 *
 * Uso: java tools/GenDesenhados.java
 */
public class GenDesenhados {

    private static final String DESTINO =
            "src/main/resources/com/retronova/resources/sprites/items/";

    private static final int QUADRO = 16;

    // A paleta e a mesma do resto do arsenal, medida dos PNGs ja versionados.
    private static final int PRETO  = 0xFF222222;   // contorno, igual a toda arma
    private static final int ACO    = 0xFFB6CBCF;   // metal claro, como na foice
    private static final int ACO_E  = 0xFF414859;   // sombra do metal
    private static final int LUZ    = 0xFFFDF7ED;   // brilho, o mesmo da espada
    private static final int MAD_C  = 0xFFC56025;   // madeira na luz, como no tridente
    private static final int MAD_E  = 0xFF8F4029;   // madeira na sombra, como no arco
    private static final int FITA   = 0xFF9F294E;   // couro do punho, o mesmo da lanca
    private static final int FITA_C = 0xFFDA4E38;

    /**
     * O tridente sai em DOIS lugares.
     *
     * Alem do icone do item, ele precisa existir como folha de utilitario: a arma
     * e arremessada, e o que voa e uma entidade (ThrownTrident) que carrega o
     * proprio sprite. Sao o mesmo desenho, e por isso saem do mesmo gerador — dois
     * arquivos mantidos a mao acabariam divergindo no primeiro ajuste.
     */
    private static final String UTILITARIOS =
            "src/main/resources/com/retronova/resources/sprites/objects/utility/";

    public static void main(String[] args) throws Exception {
        gravar(DESTINO, "shield", escudo());
    }

    private static void gravar(String pasta, String nome, BufferedImage im) throws Exception {
        new File(pasta).mkdirs();
        ImageIO.write(assentar(im), "png", new File(pasta + nome + ".png"));
        System.out.println("  " + pasta.replace("src/main/resources/com/retronova/resources/", "")
                + nome + ".png 16x16");
    }

    // ================================================================= escudo

    /**
     * Silhueta de escudo heater: topo reto, ombros retos, ponta embaixo.
     *
     * O que faz um escudo LER a 16 pixels sao tres coisas, nesta ordem: essa
     * silhueta, o aro claro contornando a face — que separa o objeto do fundo
     * escuro da dungeon — e o umbo, o disco de metal no meio. Sem o umbo o
     * desenho vira uma tabua. A luz vem de cima e da esquerda, igual ao resto dos
     * itens, entao a metade direita da madeira e um tom mais escura: face chapada
     * parece adesivo.
     */
    private static BufferedImage escudo() {
        Map<Integer, int[][]> forma = new LinkedHashMap<>();
        forma.put(2,  new int[][]{{4, 11}});
        forma.put(3,  new int[][]{{3, 12}});
        forma.put(4,  new int[][]{{3, 12}});
        forma.put(5,  new int[][]{{3, 12}});
        forma.put(6,  new int[][]{{3, 12}});
        forma.put(7,  new int[][]{{3, 12}});
        forma.put(8,  new int[][]{{3, 12}});
        forma.put(9,  new int[][]{{4, 11}});
        forma.put(10, new int[][]{{4, 11}});
        forma.put(11, new int[][]{{5, 10}});
        forma.put(12, new int[][]{{6,  9}});
        forma.put(13, new int[][]{{7,  8}});

        BufferedImage im = contornar(forma, MAD_C);

        // O aro: a primeira casca por dentro do contorno.
        BufferedImage base = copiar(im);
        for (int y = 0; y < QUADRO; y++) {
            for (int x = 0; x < QUADRO; x++) {
                if (base.getRGB(x, y) == MAD_C && encosta(base, x, y, PRETO)) {
                    im.setRGB(x, y, ACO);
                }
            }
        }
        for (int y = 0; y < QUADRO; y++) {
            for (int x = 8; x < QUADRO; x++) {
                if (im.getRGB(x, y) == MAD_C) {
                    im.setRGB(x, y, MAD_E);
                }
            }
        }
        int[][] umbo = {{7, 5}, {8, 5},
                        {6, 6}, {7, 6}, {8, 6}, {9, 6},
                        {6, 7}, {7, 7}, {8, 7}, {9, 7},
                        {7, 8}, {8, 8}};
        for (int[] p : umbo) {
            im.setRGB(p[0], p[1], ACO_E);
        }
        im.setRGB(7, 6, ACO);
        im.setRGB(7, 5, LUZ);
        return im;
    }

    // =============================================================== tridente

    /**
     * Tres dentes, o do meio mais alto — e o que faz ler tridente e nao garfo.
     *
     * Cada dente tem tres colunas: contorno, metal, contorno. Sobra um pixel de
     * metal por dente, que a essa resolucao e o maximo possivel se o desenho tiver
     * de manter o contorno preto que todas as outras armas tem. Os vaos entre os
     * dentes sao TRANSPARENTES, e nao pretos: e o vazio que conta os dentes.
     */
    private static BufferedImage tridente() {
        Map<Integer, int[][]> forma = new LinkedHashMap<>();
        int[][] tresDentes = {{2, 4}, {6, 8}, {10, 12}};
        int[][] soOMeio = {{6, 8}};
        forma.put(0,  soOMeio);
        forma.put(1,  soOMeio);
        forma.put(2,  tresDentes);
        forma.put(3,  tresDentes);
        forma.put(4,  tresDentes);
        forma.put(5,  new int[][]{{2, 12}});
        forma.put(6,  new int[][]{{2, 12}});
        forma.put(7,  new int[][]{{5, 9}});
        for (int y = 8; y < QUADRO; y++) {
            forma.put(y, soOMeio);
        }
        final int cabeca = 7;

        BufferedImage im = contornar(forma, ACO);
        for (int y = cabeca + 1; y < QUADRO; y++) {
            for (int x = 0; x < QUADRO; x++) {
                if (im.getRGB(x, y) == ACO) {
                    im.setRGB(x, y, x >= 8 ? MAD_E : MAD_C);
                }
            }
        }
        // Brilho na aresta esquerda do metal: a luz vem da esquerda.
        for (int y = 0; y <= cabeca; y++) {
            for (int x = 1; x < QUADRO; x++) {
                if (im.getRGB(x, y) == ACO && im.getRGB(x - 1, y) == PRETO) {
                    im.setRGB(x, y, LUZ);
                    break;
                }
            }
        }
        // PONTAS. O contorno automatico poe preto em cima de cada dente, e tres
        // tampas pretas fazem o desenho ler como forcado. A ponta e o unico
        // lugar onde o contorno cede: o pixel mais alto de cada dente vira brilho.
        im.setRGB(7, 0, LUZ);
        im.setRGB(3, 2, LUZ);
        im.setRGB(11, 2, LUZ);
        // Fita de couro no punho, a mesma da lanca do pacote.
        for (int y : new int[]{10, 11}) {
            for (int x = 0; x < QUADRO; x++) {
                int c = im.getRGB(x, y);
                if ((c >>> 24) > 16 && c != PRETO) {
                    im.setRGB(x, y, FITA);
                }
            }
            im.setRGB(7, y, FITA_C);
        }
        return im;
    }

    // ================================================================ comuns

    /** Pinta a forma de {@code miolo} e poe contorno preto na casca. */
    private static BufferedImage contornar(Map<Integer, int[][]> forma, int miolo) {
        BufferedImage im = new BufferedImage(QUADRO, QUADRO, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < QUADRO; y++) {
            for (int x = 0; x < QUADRO; x++) {
                if (!dentro(forma, x, y)) {
                    continue;
                }
                boolean borda = !(dentro(forma, x - 1, y) && dentro(forma, x + 1, y)
                        && dentro(forma, x, y - 1) && dentro(forma, x, y + 1));
                im.setRGB(x, y, borda ? PRETO : miolo);
            }
        }
        return im;
    }

    private static boolean dentro(Map<Integer, int[][]> forma, int x, int y) {
        int[][] faixas = forma.get(y);
        if (faixas == null) {
            return false;
        }
        for (int[] f : faixas) {
            if (x >= f[0] && x <= f[1]) {
                return true;
            }
        }
        return false;
    }

    private static boolean encosta(BufferedImage im, int x, int y, int cor) {
        return pixel(im, x - 1, y) == cor || pixel(im, x + 1, y) == cor
                || pixel(im, x, y - 1) == cor || pixel(im, x, y + 1) == cor;
    }

    private static int pixel(BufferedImage im, int x, int y) {
        return x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight() ? 0 : im.getRGB(x, y);
    }

    private static BufferedImage copiar(BufferedImage im) {
        BufferedImage o = new BufferedImage(im.getWidth(), im.getHeight(), im.getType());
        o.getGraphics().drawImage(im, 0, 0, null);
        return o;
    }

    /**
     * Desce o desenho ate o fundo do quadro.
     *
     * O jogo poe o punho no pe do quadro de 16 — ver Item.empunhadura —, entao
     * arma que flutua no meio do arquivo aparece flutuando acima da mao do gato.
     */
    private static BufferedImage assentar(BufferedImage im) {
        int ultima = -1;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                if ((im.getRGB(x, y) >>> 24) > 16) {
                    ultima = y;
                }
            }
        }
        BufferedImage o = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB);
        o.getGraphics().drawImage(im, 0, im.getHeight() - 1 - ultima, null);
        return o;
    }
}
