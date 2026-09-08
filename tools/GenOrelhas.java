import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Orelhas de gato para os paineis: inventario, status e hotbar.
 *
 * Os botoes e os cards de personagem ja tinham orelha — e era a unica coisa em
 * toda a interface que dizia que o jogo e sobre gato. Mochila, ficha e hotbar
 * continuavam sendo caixas de RPG generico, e do lado dos botoes isso lia como
 * duas interfaces diferentes no mesmo jogo.
 *
 * TIRA SEPARADA, E NAO ASSADA NO PAINEL. As orelhas saem em PNGs proprios,
 * desenhados por cima na hora de renderizar. Assar no painel obrigaria a crescer
 * a tela dos arquivos, e toda posicao de slot no codigo e contada a partir do
 * canto do painel: inventory.png, status.png e a folha da hotbar mudariam de
 * tamanho e as tres telas sairiam do lugar de uma vez. A tira nao mexe em
 * dimensao nenhuma, entao nao ha o que recalcular.
 *
 * A geometria e a MESMA dos botoes (tools/GenUiAssets.java): cinco linhas de
 * altura, base de sete pixels afinando para dois. Orelha de tamanho diferente
 * em cada tela seria pior do que nao ter orelha.
 *
 * As cores ja saem na paleta final, entao estes arquivos ficam de fora do
 * retintor — ver DE_OUTRO_DONO em tools/GenTema.java.
 *
 * Uso: java tools/GenOrelhas.java
 */
public class GenOrelhas {

    private static final String UI = "src/main/resources/com/retronova/resources/ui/";

    /**
     * A orelha cresce com o painel.
     *
     * A geometria dos botoes (5 de altura por 7 de base) foi desenhada para uma
     * celula de 24 pixels. Repetida numa moldura de 92 ou 140, a mesma orelha
     * vira um espeto no canto — nao le como orelha, le como enfeite. A forma e a
     * mesma, so a escala acompanha a largura de cada painel.
     */
    private static final int PONTA = 3;

    /**
     * Uma linha a mais, por baixo, encostando no painel.
     *
     * Sem ela sobra uma costura de um pixel entre a base da orelha e a borda do
     * painel, porque a tira e desenhada como imagem separada e o antialias do
     * escalonamento nao junta as duas.
     */
    private static final int COSTURA = 1;

    // Paleta do jogo, a mesma de engine/graphics/Palette.java.
    private static final int OUTLINE = 0xFF211A2B;
    private static final int MAIN    = 0xFF525779;
    private static final int LIGHT   = 0xFF7E849C;

    /** Cada painel: largura, distancia da borda, e o tamanho da orelha. */
    private record Painel(String nome, int largura, int margem, int altura, int base) { }

    private static final Painel[] PAINEIS = {
            new Painel("inventory", 92, 6, 8, 12),
            new Painel("status", 140, 8, 9, 14),
            // A hotbar do HUD e uma barra fina; orelha grande demais ali competiria
            // com os proprios itens, que sao o que o jogador precisa ver.
            new Painel("hotbar", 80, 4, 6, 9),
    };

    public static void main(String[] args) throws Exception {
        for (Painel p : PAINEIS) {
            BufferedImage tira = tira(p);
            ImageIO.write(tira, "png", new File(UI + "ears_" + p.nome() + ".png"));
            System.out.printf("  ui/ears_%s.png %dx%d%n", p.nome(), tira.getWidth(), tira.getHeight());
        }
    }

    private static BufferedImage tira(Painel p) {
        int altura = p.altura() + COSTURA;
        BufferedImage im = new BufferedImage(p.largura(), altura, BufferedImage.TYPE_INT_ARGB);
        boolean[][] sil = new boolean[p.largura()][altura];

        orelha(sil, p, p.margem(), altura - COSTURA, false);
        orelha(sil, p, p.largura() - 1 - p.margem(), altura - COSTURA, true);

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < p.largura(); x++) {
                if (!sil[x][y]) {
                    continue;
                }
                im.setRGB(x, y, contorno(sil, x, y, p.largura(), altura) ? OUTLINE : MAIN);
            }
        }
        // Miolo claro, o mesmo detalhe do botao: sem ele a orelha vira um
        // triangulo chapado e some contra a borda do painel.
        int meio = p.base() / 2;
        clarear(im, sil, p, p.margem() + meio, altura - COSTURA, p.largura(), altura);
        clarear(im, sil, p, p.largura() - 1 - p.margem() - meio, altura - COSTURA, p.largura(), altura);
        return im;
    }

    /**
     * Triangulo de orelha. {@code xBorda} e a coluna do cateto vertical, na borda
     * externa; a hipotenusa desce em direcao ao centro.
     */
    private static void orelha(boolean[][] sil, Painel p, int xBorda, int baseY, boolean direita) {
        for (int i = 0; i < p.altura(); i++) {
            int y = baseY - 1 - i;
            if (y < 0) {
                continue;
            }
            int larg = Math.round(p.base() + (PONTA - p.base()) * (i / (float) Math.max(1, p.altura() - 1)));
            for (int k = 0; k < larg; k++) {
                int x = direita ? xBorda - k : xBorda + k;
                if (x >= 0 && x < sil.length) {
                    sil[x][y] = true;
                }
            }
        }
        // A costura: a base da orelha continua uma linha para dentro do painel.
        for (int k = 0; k < p.base(); k++) {
            int x = direita ? xBorda - k : xBorda + k;
            if (x >= 0 && x < sil.length && baseY < sil[0].length) {
                sil[x][baseY] = true;
            }
        }
    }

    private static void clarear(BufferedImage im, boolean[][] sil, Painel p, int cx, int baseY, int w, int h) {
        for (int i = 1; i <= p.altura() - 3; i++) {
            int y = baseY - 1 - i;
            if (y < 0 || cx < 0 || cx >= w) {
                continue;
            }
            if (sil[cx][y] && !contorno(sil, cx, y, w, h)) {
                im.setRGB(cx, y, LIGHT);
            }
        }
    }

    /** Pixel da silhueta encostando no vazio: vira contorno. */
    private static boolean contorno(boolean[][] m, int x, int y, int w, int h) {
        int[][] d = {{1, 0}, {-1, 0}, {0, -1}};   // embaixo encosta no painel, nao e borda
        for (int[] v : d) {
            int nx = x + v[0], ny = y + v[1];
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                return true;
            }
            if (!m[nx][ny]) {
                return true;
            }
        }
        return false;
    }
}
