import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Estica o painel da loja para caber mais fileiras.
 *
 * A loja tinha tres fileiras de sete, e o pedido e uma fileira por FAMILIA de
 * arma — espadas, machados, arcos, varinhas e diversos — para dar de relance o
 * que ja existe e o que falta. Cinco familias nao cabiam.
 *
 * O painel e esticado, e nao redesenhado: a faixa de uma fileira e copiada duas
 * vezes e inserida no meio, com o cabecalho e o rodape intactos. E o mesmo
 * raciocinio do 9-slice — so a parte repetivel se repete —, e mantem cada pixel
 * do desenho original.
 *
 * ORDEM NO ENCANAMENTO. Este gerador mexe na FORMA e escreve em ui_original;
 * quem pinta e o GenTema, que le de la e retinge para a paleta da arte. Rodar na
 * ordem inversa devolvia o painel na paleta vermelha antiga, porque o retintor
 * so conhece o arquivo que este aqui produz.
 *
 * A copia intocada de tres fileiras fica guardada a parte, senao a segunda
 * execucao esticaria o proprio resultado.
 *
 * Uso: java tools/GenLoja.java
 */
public class GenLoja {

    private static final String UI = "src/main/resources/com/retronova/resources/ui/";
    private static final String ORIGINAIS = "tools/assets/ui_original/";

    /** Onde comeca a grade de slots e qual a altura de uma fileira, no PNG. */
    private static final int GRADE_Y = 26, FILEIRA = 16;

    /** Quantas fileiras acrescentar. Tres viram seis. */
    private static final int A_MAIS = 3;

    public static void main(String[] a) throws Exception {
        new File(ORIGINAIS).mkdirs();
        File forma = new File(ORIGINAIS + "store.png");
        File intocado = new File(ORIGINAIS + "store_3x7.png");
        if (!intocado.exists()) {
            ImageIO.write(ImageIO.read(forma), "png", intocado);
        }
        BufferedImage origem = ImageIO.read(intocado);

        int altura = origem.getHeight() + FILEIRA * A_MAIS;
        BufferedImage o = new BufferedImage(origem.getWidth(), altura,
                BufferedImage.TYPE_INT_ARGB);

        // Cabecalho e a primeira fileira, intactos.
        int corte = GRADE_Y + FILEIRA;
        copiar(origem, o, 0, corte, 0);
        // A faixa de uma fileira, repetida.
        for (int i = 0; i < A_MAIS; i++) {
            copiar(origem, o, GRADE_Y, GRADE_Y + FILEIRA, corte + i * FILEIRA);
        }
        // O resto: as duas fileiras que sobraram, o divisor e o rodape.
        copiar(origem, o, corte, origem.getHeight(), corte + A_MAIS * FILEIRA);

        // Escreve na FORMA. O pigmento vem depois:  java tools/GenTema.java
        ImageIO.write(o, "png", forma);
        System.out.printf("  ui_original/store.png %dx%d -> %dx%d (%d fileiras)%n",
                origem.getWidth(), origem.getHeight(), o.getWidth(), altura,
                3 + A_MAIS);
    }

    /** Copia as linhas [de, ate) da origem para a linha destino da saida. */
    static void copiar(BufferedImage origem, BufferedImage saida, int de, int ate, int destino) {
        for (int y = de; y < ate && y < origem.getHeight(); y++) {
            int alvo = destino + (y - de);
            if (alvo < 0 || alvo >= saida.getHeight()) {
                continue;
            }
            for (int x = 0; x < origem.getWidth(); x++) {
                saida.setRGB(x, alvo, origem.getRGB(x, y));
            }
        }
    }
}
