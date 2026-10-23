import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Extrai o letreiro "Cats & Dungeons" da ilustração do título, numa camada própria
 * para a abertura (menus/Abertura) animar.
 *
 * ---------------------------------------------------------------------------
 * POR QUE EXTRAIR, E NÃO DESENHAR OUTRO
 *
 * O letreiro só existe pintado dentro de icons/Gato.png. A abertura mostra a logo
 * sozinha no escuro e depois faz a cena surgir em volta dela; se a logo da
 * abertura fosse outro desenho, a troca para a ilustração mostraria duas logos
 * diferentes. Extraída da própria arte, ela coincide pixel a pixel com o
 * letreiro pintado, e a passagem não tem emenda.
 *
 * COMO SE SEPARA. As letras são quase brancas; a lua atrás delas é clara, mas
 * azulada — o menor dos três canais dela fica abaixo de 220, e o das letras acima
 * de 240. A transparência sai desse canal, numa rampa entre os dois limites, o que
 * preserva a borda suave da pintura. Só a faixa do letreiro é lida.
 *
 * Saída: icons/logo.png, recortada no menor retângulo que contém as letras. O
 * canto do recorte na ilustração é impresso e precisa bater com
 * Abertura.LOGO_X / LOGO_Y (a abertura desenha a logo onde ela está na arte).
 *
 *     java tools/GenLogo.java
 */
public class GenLogo {

    private static final String ARTE = "src/main/resources/com/retronova/resources/icons/Gato.png";
    private static final String SAIDA = "src/main/resources/com/retronova/resources/icons/logo.png";

    /** A faixa do letreiro na ilustração (3260x1620), com folga. */
    private static final int FAIXA_X = 300, FAIXA_Y = 0, FAIXA_W = 2700, FAIXA_H = 720;
    /** Menor canal abaixo disto é cena (transparente); acima de CHEIO, letra inteira. */
    private static final int CENA = 222, CHEIO = 246;

    public static void main(String[] args) throws Exception {
        BufferedImage arte = ImageIO.read(new File(ARTE));
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        int[][] alfa = new int[FAIXA_H][FAIXA_W];
        for (int y = 0; y < FAIXA_H; y++) {
            for (int x = 0; x < FAIXA_W; x++) {
                int p = arte.getRGB(FAIXA_X + x, FAIXA_Y + y);
                int menor = Math.min((p >> 16) & 255, Math.min((p >> 8) & 255, p & 255));
                int a = (int) Math.round(255.0 * Math.max(0, Math.min(1, (menor - CENA) / (double) (CHEIO - CENA))));
                alfa[y][x] = a;
                if (a > 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        int w = maxX - minX + 1, h = maxY - minY + 1;
        BufferedImage logo = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = alfa[minY + y][minX + x];
                if (a > 0) {
                    int p = arte.getRGB(FAIXA_X + minX + x, FAIXA_Y + minY + y);
                    logo.setRGB(x, y, (a << 24) | (p & 0xFFFFFF));
                }
            }
        }
        ImageIO.write(logo, "png", new File(SAIDA));
        System.out.printf("gravado %s (%dx%d), canto na arte: x=%d y=%d%n",
                SAIDA, w, h, FAIXA_X + minX, FAIXA_Y + minY);
    }
}
