import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Os retratos de diálogo, desenhados em pixel art a partir do sprite do mundo.
 *
 * UMA FOLHA POR PERSONAGEM, em sprites/retratos/<nome>.png:
 *   colunas: as emoções, na ordem de game/dialogos/Emocao.java
 *            (neutro, feliz, triste, bravo, confuso, surpreso, malicioso);
 *   linhas:  0 olhos abertos, 1 piscada, 2 boca aberta (falando).
 * Cada quadro tem LADO x LADO pixels. Emoção sem coluna (folha mais estreita)
 * cai na neutra, sem quebrar — ver game/dialogos/Retrato.java.
 *
 * O VENDEDOR. As cores são as do sprite dele (sprites/objects/npc/seller.png):
 * pelo caramelo #A78563 com listras #A37444, focinho creme #EDDFD2, orelha
 * rosa #D393BF, olhos e nariz em marrom escuro, colete carmim #770C0C e o botão
 * dourado #FBF236. O contorno é o da interface (Palette.OUTLINE), porque o
 * retrato mora na caixa de diálogo, e não no mundo.
 *
 * O ROSTO É FIXO E SÓ OLHOS, SOBRANCELHAS E BOCA MUDAM. É como o Stardew e o
 * Celeste fazem: a cabeça igual em todos os quadros deixa a troca de emoção
 * legível como expressão, e não como outro desenho. Em 32 pixels, cada emoção
 * se resolve em três peças de poucos pixels; o teste RetratoTest confere que
 * todas diferem da neutra.
 *
 * Uso: java tools/GenRetratos.java
 */
public class GenRetratos {

    static final int LADO = 32;
    static final int EMOCOES = 7, LINHAS = 3;

    static final String SAIDA = "src/main/resources/com/retronova/resources/sprites/retratos/";

    // Paleta do vendedor (sprite) e o contorno da interface.
    static final int CONTORNO = 0xFF211A2B;
    static final int PELO = 0xFFA78563, PELO_SOMBRA = 0xFF7E5E40, PELO_LUZ = 0xFFC4A27C, LISTRA = 0xFF8C6239;
    static final int CREME = 0xFFEDDFD2, CREME_SOMBRA = 0xFFC9B6A4;
    static final int ROSA = 0xFFD393BF, ROSA_ESCURO = 0xFFA8608F;
    static final int ESCURO = 0xFF351F0A, NARIZ = 0xFF8A4F5E, BRILHO = 0xFFFCFCFC;
    static final int COLETE = 0xFF770C0C, COLETE_SOMBRA = 0xFF4E0808, COLETE_LUZ = 0xFFA0201C;
    static final int OURO = 0xFFFBF236, OURO_SOMBRA = 0xFFC29A20;

    // Onde ficam as peças do rosto.
    static final int OLHO_E = 11, OLHO_D = 20, OLHO_Y = 14, BOCA_X = 16, BOCA_Y = 19;

    public static void main(String[] args) throws Exception {
        new File(SAIDA).mkdirs();
        BufferedImage folha = new BufferedImage(LADO * EMOCOES, LADO * LINHAS, BufferedImage.TYPE_INT_ARGB);
        for (int e = 0; e < EMOCOES; e++) {
            for (int l = 0; l < LINHAS; l++) {
                int[][] q = vendedor(e, l);
                for (int y = 0; y < LADO; y++) {
                    for (int x = 0; x < LADO; x++) {
                        folha.setRGB(e * LADO + x, l * LADO + y, q[y][x]);
                    }
                }
            }
        }
        ImageIO.write(folha, "png", new File(SAIDA + "vendedor.png"));
        System.out.println("  sprites/retratos/vendedor.png (" + EMOCOES + " emocoes x " + LINHAS + " linhas)");
    }

    // ------------------------------------------------------------- o vendedor

    static int[][] vendedor(int emocao, int linha) {
        int[][] p = new int[LADO][LADO];
        corpo(p);
        cabeca(p);
        contornar(p);
        rosto(p, emocao, linha);
        return p;
    }

    /** Ombros com o colete, o peito creme e o botão dourado na gola. */
    static void corpo(int[][] p) {
        for (int y = 22; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                double dx = (x + 0.5 - 16) / 13.0, dy = (y + 0.5 - 34) / 11.0;
                if (dx * dx + dy * dy > 1) {
                    continue;
                }
                int largura = (y - 22) / 2 + 2;
                if (Math.abs(x + 0.5 - 16) <= largura) {
                    p[y][x] = (x < 16 && y > 26) ? CREME_SOMBRA : CREME;
                } else {
                    p[y][x] = x < 9 ? COLETE_LUZ : x > 23 ? COLETE_SOMBRA : COLETE;
                }
            }
        }
        // A gola do colete, uma faixa clara de cada lado do peito.
        for (int y = 23; y < LADO; y++) {
            int largura = (y - 22) / 2 + 2;
            int esq = (int) Math.floor(16 - largura - 1), dir = (int) Math.ceil(16 + largura);
            if (esq >= 0 && p[y][esq] != 0) {
                p[y][esq] = COLETE_LUZ;
            }
            if (dir < LADO && p[y][dir] != 0) {
                p[y][dir] = COLETE_SOMBRA;
            }
        }
        p[25][15] = OURO;
        p[25][16] = OURO;
        p[26][15] = OURO_SOMBRA;
        p[26][16] = OURO_SOMBRA;
    }

    /** Orelhas, cabeça redonda e larga como a do sprite, focinho creme e listras na testa. */
    static void cabeca(int[][] p) {
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                double cx = x + 0.5, cy = y + 0.5;
                boolean orelha = triangulo(cx, cy, 6, 1, 4, 11, 13, 7)
                        || triangulo(cx, cy, 26, 1, 28, 11, 19, 7);
                double dx = (cx - 16) / 11.8, dy = (cy - 14.5) / 9.6;
                boolean cabeca = dx * dx + dy * dy <= 1;
                if (!orelha && !cabeca) {
                    continue;
                }
                if (orelha && !cabeca) {
                    boolean dentro = triangulo(cx, cy, 6.6, 3.2, 5.6, 9.5, 11, 7.2)
                            || triangulo(cx, cy, 25.4, 3.2, 26.4, 9.5, 21, 7.2);
                    p[y][x] = dentro ? (cy < 5.5 ? ROSA : ROSA_ESCURO) : PELO;
                    continue;
                }
                // Sombra embaixo e à direita, luz em cima à esquerda: a luz da arte do jogo.
                double luz = dx * 0.55 + dy * 0.8;
                p[y][x] = luz > 0.62 ? PELO_SOMBRA : luz < -0.62 ? PELO_LUZ : PELO;
                // O focinho e as bochechas creme, na metade de baixo.
                double fx = (cx - 16) / 8.6, fy = (cy - 20.2) / 4.8;
                if (fx * fx + fy * fy <= 1) {
                    p[y][x] = cy > 22.5 && cx > 16 ? CREME_SOMBRA : CREME;
                }
            }
        }
        // As listras da testa, como as três do sprite.
        for (int y = 6; y <= 8; y++) {
            p[y][13] = LISTRA;
            p[y][16] = LISTRA;
            p[y][19] = LISTRA;
        }
        p[9][16] = LISTRA;
    }

    static boolean triangulo(double px, double py, double ax, double ay, double bx, double by,
                             double cx, double cy) {
        double d1 = sinal(px, py, ax, ay, bx, by), d2 = sinal(px, py, bx, by, cx, cy),
                d3 = sinal(px, py, cx, cy, ax, ay);
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0, pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    static double sinal(double px, double py, double ax, double ay, double bx, double by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }

    /** Pixel pintado encostando no vazio vira contorno. */
    static void contornar(int[][] p) {
        int[][] antes = new int[LADO][];
        for (int y = 0; y < LADO; y++) {
            antes[y] = p[y].clone();
        }
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                if (antes[y][x] != 0) {
                    continue;
                }
                if (pintado(antes, x + 1, y) || pintado(antes, x - 1, y)
                        || pintado(antes, x, y + 1) || pintado(antes, x, y - 1)) {
                    p[y][x] = CONTORNO;
                }
            }
        }
    }

    static boolean pintado(int[][] p, int x, int y) {
        return x >= 0 && y >= 0 && x < LADO && y < LADO && p[y][x] != 0;
    }

    // ----------------------------------------------------------- expressões

    /** Olhos, sobrancelhas e boca de cada emoção. Linha 1 pisca, linha 2 abre a boca. */
    static void rosto(int[][] p, int emocao, int linha) {
        boolean pisca = linha == 1, fala = linha == 2;
        switch (emocao) {
            case 0 -> { // neutro
                olhos(p, pisca, 3);
                if (fala) bocaAberta(p, 0); else bocaGato(p);
            }
            case 1 -> { // feliz: olhos em arco fechado, sorriso largo
                for (int c : new int[]{OLHO_E, OLHO_D}) {
                    p[OLHO_Y][c - 1] = ESCURO;
                    p[OLHO_Y - 1][c] = ESCURO;
                    p[OLHO_Y - 1][c + 1] = ESCURO;
                    p[OLHO_Y][c + 2] = ESCURO;
                }
                nariz(p);
                if (fala) {
                    bocaAberta(p, 1);
                } else {
                    p[BOCA_Y][BOCA_X - 3] = ESCURO;
                    for (int x = BOCA_X - 2; x <= BOCA_X + 1; x++) {
                        p[BOCA_Y + 1][x] = ESCURO;
                    }
                    p[BOCA_Y][BOCA_X + 2] = ESCURO;
                    p[BOCA_Y + 2][BOCA_X - 1] = ROSA;
                    p[BOCA_Y + 2][BOCA_X] = ROSA;
                }
                // Bochechas coradas.
                p[OLHO_Y + 3][OLHO_E - 2] = ROSA;
                p[OLHO_Y + 3][OLHO_D + 3] = ROSA;
            }
            case 2 -> { // triste: sobrancelhas subindo para o meio, olhar baixo, boca caída
                for (int i = 0; i < 3; i++) {
                    p[OLHO_Y - 3 + (i == 2 ? -1 : 0)][OLHO_E - 1 + i] = ESCURO;
                    p[OLHO_Y - 3 + (i == 0 ? -1 : 0)][OLHO_D + i] = ESCURO;
                }
                olhosBaixos(p, pisca);
                nariz(p);
                if (fala) {
                    bocaAberta(p, 0);
                } else {
                    p[BOCA_Y + 2][BOCA_X - 2] = ESCURO;
                    p[BOCA_Y + 1][BOCA_X - 1] = ESCURO;
                    p[BOCA_Y + 1][BOCA_X] = ESCURO;
                    p[BOCA_Y + 2][BOCA_X + 1] = ESCURO;
                }
                p[OLHO_Y + 2][OLHO_D + 1] = 0xFF7FB7E0;   // uma lágrima
            }
            case 3 -> { // bravo: sobrancelhas descendo para o meio, olhos estreitos, boca reta
                for (int i = 0; i < 3; i++) {
                    p[OLHO_Y - 4 + i][OLHO_E - 1 + i] = ESCURO;
                    p[OLHO_Y - 2 - i][OLHO_D + i] = ESCURO;
                }
                olhosEstreitos(p, pisca);
                nariz(p);
                if (fala) {
                    bocaAberta(p, 2);
                } else {
                    for (int x = BOCA_X - 2; x <= BOCA_X + 1; x++) {
                        p[BOCA_Y + 1][x] = ESCURO;
                    }
                }
            }
            case 4 -> { // confuso: uma sobrancelha alta, olhos de tamanhos diferentes, boca torta
                p[OLHO_Y - 4][OLHO_E - 1] = ESCURO;
                p[OLHO_Y - 5][OLHO_E] = ESCURO;
                p[OLHO_Y - 4][OLHO_E + 1] = ESCURO;
                p[OLHO_Y - 3][OLHO_D] = ESCURO;
                p[OLHO_Y - 3][OLHO_D + 1] = ESCURO;
                olho(p, OLHO_E, pisca, 3);
                olho(p, OLHO_D, pisca, 2);
                nariz(p);
                if (fala) {
                    bocaAberta(p, 0);
                } else {
                    p[BOCA_Y + 1][BOCA_X - 2] = ESCURO;
                    p[BOCA_Y + 2][BOCA_X - 1] = ESCURO;
                    p[BOCA_Y + 1][BOCA_X] = ESCURO;
                    p[BOCA_Y + 2][BOCA_X + 1] = ESCURO;
                }
            }
            case 5 -> { // surpreso: sobrancelhas altas, olhos grandes, boca em O
                for (int c : new int[]{OLHO_E, OLHO_D}) {
                    p[OLHO_Y - 5][c] = ESCURO;
                    p[OLHO_Y - 5][c + 1] = ESCURO;
                }
                olhosGrandes(p, pisca);
                nariz(p);
                p[BOCA_Y + 1][BOCA_X - 1] = ESCURO;
                p[BOCA_Y + 1][BOCA_X] = ESCURO;
                p[BOCA_Y + 2][BOCA_X - 2] = ESCURO;
                p[BOCA_Y + 2][BOCA_X + 1] = ESCURO;
                p[BOCA_Y + 3][BOCA_X - 1] = ESCURO;
                p[BOCA_Y + 3][BOCA_X] = ESCURO;
                p[BOCA_Y + 2][BOCA_X - 1] = fala ? ROSA : NARIZ;
                p[BOCA_Y + 2][BOCA_X] = fala ? ROSA : NARIZ;
            }
            case 6 -> { // malicioso: pálpebra caída, uma sobrancelha erguida, sorriso de canto
                p[OLHO_Y - 3][OLHO_E - 1] = ESCURO;
                p[OLHO_Y - 3][OLHO_E] = ESCURO;
                p[OLHO_Y - 3][OLHO_E + 1] = ESCURO;
                p[OLHO_Y - 4][OLHO_D] = ESCURO;
                p[OLHO_Y - 5][OLHO_D + 1] = ESCURO;
                p[OLHO_Y - 4][OLHO_D + 2] = ESCURO;
                olhosMeioFechados(p, pisca);
                nariz(p);
                if (fala) {
                    bocaAberta(p, 1);
                } else {
                    for (int x = BOCA_X - 2; x <= BOCA_X; x++) {
                        p[BOCA_Y + 1][x] = ESCURO;
                    }
                    p[BOCA_Y][BOCA_X + 1] = ESCURO;
                    p[BOCA_Y - 1][BOCA_X + 2] = ESCURO;
                }
            }
            default -> olhos(p, pisca, 3);
        }
    }

    static void olhos(int[][] p, boolean pisca, int altura) {
        olho(p, OLHO_E, pisca, altura);
        olho(p, OLHO_D, pisca, altura);
    }

    /** Um olho de dois pixels de largura, com o brilho em cima à esquerda. */
    static void olho(int[][] p, int x, boolean pisca, int altura) {
        if (pisca) {
            p[OLHO_Y + 1][x - 1] = ESCURO;
            p[OLHO_Y + 1][x] = ESCURO;
            p[OLHO_Y + 1][x + 1] = ESCURO;
            p[OLHO_Y + 1][x + 2] = ESCURO;
            return;
        }
        int topo = OLHO_Y + (3 - altura);
        for (int y = topo; y < OLHO_Y + 3; y++) {
            p[y][x] = ESCURO;
            p[y][x + 1] = ESCURO;
        }
        p[topo][x] = BRILHO;
    }

    static void olhosBaixos(int[][] p, boolean pisca) {
        for (int c : new int[]{OLHO_E, OLHO_D}) {
            if (pisca) {
                olho(p, c, true, 2);
                continue;
            }
            p[OLHO_Y + 1][c] = ESCURO;
            p[OLHO_Y + 1][c + 1] = ESCURO;
            p[OLHO_Y + 2][c] = ESCURO;
            p[OLHO_Y + 2][c + 1] = ESCURO;
            p[OLHO_Y + 1][c] = BRILHO;
        }
    }

    static void olhosEstreitos(int[][] p, boolean pisca) {
        for (int c : new int[]{OLHO_E, OLHO_D}) {
            if (pisca) {
                olho(p, c, true, 2);
                continue;
            }
            for (int x = c - 1; x <= c + 2; x++) {
                p[OLHO_Y + 1][x] = ESCURO;
            }
            p[OLHO_Y + 2][c] = ESCURO;
            p[OLHO_Y + 2][c + 1] = ESCURO;
        }
    }

    static void olhosGrandes(int[][] p, boolean pisca) {
        for (int c : new int[]{OLHO_E, OLHO_D}) {
            if (pisca) {
                olho(p, c, true, 3);
                continue;
            }
            for (int y = OLHO_Y - 1; y < OLHO_Y + 3; y++) {
                for (int x = c - 1; x <= c + 2; x++) {
                    boolean canto = (y == OLHO_Y - 1 || y == OLHO_Y + 2) && (x == c - 1 || x == c + 2);
                    if (!canto) {
                        p[y][x] = ESCURO;
                    }
                }
            }
            p[OLHO_Y][c] = BRILHO;
            p[OLHO_Y - 1][c] = BRILHO;
        }
    }

    static void olhosMeioFechados(int[][] p, boolean pisca) {
        for (int c : new int[]{OLHO_E, OLHO_D}) {
            if (pisca) {
                olho(p, c, true, 2);
                continue;
            }
            for (int x = c - 1; x <= c + 2; x++) {
                p[OLHO_Y + 1][x] = ESCURO;
            }
            p[OLHO_Y + 2][c] = ESCURO;
            p[OLHO_Y + 2][c + 1] = ESCURO;
            p[OLHO_Y + 2][c] = BRILHO;
        }
    }

    static void nariz(int[][] p) {
        p[BOCA_Y - 2][BOCA_X - 1] = NARIZ;
        p[BOCA_Y - 2][BOCA_X] = NARIZ;
        p[BOCA_Y - 1][BOCA_X - 1] = ESCURO;
        p[BOCA_Y - 1][BOCA_X] = ESCURO;
    }

    /** O "w" do gato, debaixo do nariz. */
    static void bocaGato(int[][] p) {
        nariz(p);
        p[BOCA_Y][BOCA_X - 1] = ESCURO;
        p[BOCA_Y][BOCA_X] = ESCURO;
        p[BOCA_Y + 1][BOCA_X - 2] = ESCURO;
        p[BOCA_Y + 1][BOCA_X + 1] = ESCURO;
    }

    /** Boca aberta para a fala: 0 normal, 1 sorrindo, 2 de dentes cerrados. */
    static void bocaAberta(int[][] p, int jeito) {
        nariz(p);
        int largura = jeito == 1 ? 3 : 2;
        for (int y = BOCA_Y; y <= BOCA_Y + 2; y++) {
            for (int x = BOCA_X - largura; x < BOCA_X + largura; x++) {
                boolean borda = y == BOCA_Y || y == BOCA_Y + 2 || x == BOCA_X - largura || x == BOCA_X + largura - 1;
                p[y][x] = borda ? ESCURO : (jeito == 2 ? BRILHO : ROSA);
            }
        }
    }
}
