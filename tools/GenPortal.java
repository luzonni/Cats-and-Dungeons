import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Portal em pixel art: aro interrompido, vazio central e faíscas orbitais.
 * A folha guarda cinco níveis de cinza; Chegada.Tom aplica a família de cor.
 * Reproduzir: java tools/GenPortal.java
 */
public class GenPortal {
    private static final int LADO = 16;
    private static final int QUADROS = 4;
    private static final int[] NIVEIS = {0, 64, 128, 192, 255};

    public static void main(String[] args) throws Exception {
        BufferedImage folha = new BufferedImage(LADO * QUADROS, LADO, BufferedImage.TYPE_INT_ARGB);
        for (int q = 0; q < QUADROS; q++) {
            for (int y = 0; y < LADO; y++) {
                for (int x = 0; x < LADO; x++) {
                    double dx = (x - 7.5) / 6.5, dy = (y - 8.5) / 4.5;
                    double raio = Math.sqrt(dx * dx + dy * dy);
                    if (raio > 1.15) continue;
                    // Três trechos de luz circulam pelo aro, sem clarear o centro.
                    double arco = Math.sin(3 * Math.atan2(dy, dx) - q * Math.PI / 2);
                    int nivel;
                    if (raio < 0.60) nivel = 0;
                    else if (raio < 0.78) nivel = arco > 0.25 ? 3 : 1;
                    else if (raio < 1.0) nivel = arco > 0.65 ? 4 : arco > -0.4 ? 2 : 1;
                    else nivel = 0;
                    pixel(folha, q, x, y, nivel);
                }
            }
            // Fragmentos curtos: luz em poucos pixels, sem névoa suavizada.
            int[][] faiscas = {{3, 3}, {12, 4}, {13, 13}, {2, 12}};
            for (int i = 0; i < faiscas.length; i++) {
                int x = faiscas[i][0], y = faiscas[i][1];
                pixel(folha, q, x, y, i == q ? 4 : 2);
                if (i == q) pixel(folha, q, x, y - 1, 3);
            }
        }
        File destino = new File("src/main/resources/com/retronova/resources/sprites/objects/furniture/portalRunforce.png");
        ImageIO.write(folha, "png", destino);
        System.out.println(destino);
    }

    private static void pixel(BufferedImage folha, int quadro, int x, int y, int nivel) {
        int v = NIVEIS[nivel];
        folha.setRGB(quadro * LADO + x, y, 0xff000000 | v << 16 | v << 8 | v);
    }
}
