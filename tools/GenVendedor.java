import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Conserta a piscada do vendedor.
 *
 * O QUE ESTAVA ERRADO. A folha dele tem cinco quadros, e nos tres do meio os olhos
 * simplesmente NAO EXISTEM: os dois pixels foram pintados com a cor do pelo. Isso
 * nao le como olho fechado, le como olho apagado — o gato fica de cara lisa por um
 * instante, e o efeito e de erro de desenho, nao de piscada.
 *
 * O QUE PASSA A SER. A mesma regra que os gatos jogaveis usam, medida no sprite
 * deles quando dormem: olho fechado e o pixel de baixo sobrando, mais um segundo
 * pixel para o lado do meio da cara. Dois pixels na horizontal, onde antes havia
 * dois na vertical — e a troca de orientacao que le como palpebra baixada.
 *
 * A COR VEM DO QUADRO ABERTO, e nao de um valor escolhido aqui: o vendedor tem
 * olho castanho escuro, e amanha ele pode ter outro. Lendo do proprio desenho, o
 * conserto continua certo se a arte dele mudar.
 *
 *     java tools/GenVendedor.java
 */
public class GenVendedor {

    private static final String ARQUIVO =
            "src/main/resources/com/retronova/resources/sprites/objects/npc/seller.png";

    /** Onde fica o rosto. Igual ao dos gatos jogaveis — a cabeca e a mesma. */
    private static final int OLHO_ESQ = 4;
    private static final int OLHO_DIR = 10;
    private static final int OLHO_TOPO = 5;
    private static final int OLHO_BASE = 6;

    private static final int LADO = 16;

    /** O focinho no quadro de referencia: e por ele que se acha a cabeca. */
    private static final int FOCINHO_X = 7;
    private static final int FOCINHO_Y = 7;

    public static void main(String[] args) throws Exception {
        BufferedImage folha = ImageIO.read(new File(ARQUIVO));
        int quadros = folha.getWidth() / LADO;

        int corDoOlho = folha.getRGB(OLHO_ESQ, OLHO_BASE);

        BufferedImage saida = new BufferedImage(folha.getWidth(), folha.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        saida.getGraphics().drawImage(folha, 0, 0, null);

        int consertados = 0;
        for (int q = 0; q < quadros; q++) {
            int base = q * LADO;
            // A CABECA BALANCA, e o olho tem de acompanhar.
            //
            // Esta foi a lição da primeira tentativa: eu pintei o olho fechado
            // sempre nas mesmas linhas, e nos quadros do meio ele saiu flutuando
            // acima do rosto. O vendedor respira — o focinho dele desce ate dois
            // pixels ao longo da animacao — entao a altura do olho e RELATIVA a
            // cabeca, e nao ao quadro.
            //
            // O focinho e a referencia porque e o unico ponto do rosto com cor
            // exclusiva: achando ele, sabe-se onde a cabeca esta neste quadro.
            int desloca = alturaDaCabeca(saida, base);
            if (desloca < 0) {
                continue;
            }
            int topo = OLHO_TOPO + desloca;
            int baixo = OLHO_BASE + desloca;

            // So conserta o quadro que perdeu o olho por inteiro. Os intermediarios
            // ja tem meio olho desenhado a mao, e sao melhores do que qualquer
            // coisa que este programa colocaria no lugar.
            if (temOlho(saida, base, topo) || temOlho(saida, base, baixo)) {
                continue;
            }
            int pelo = saida.getRGB(base + OLHO_ESQ, topo - 1);
            fechar(saida, base, OLHO_ESQ, baixo, corDoOlho, pelo);
            fechar(saida, base, OLHO_DIR, baixo, corDoOlho, pelo);
            consertados++;
        }

        ImageIO.write(saida, "png", new File(ARQUIVO));
        System.out.println("Vendedor");
        System.out.println("  " + consertados + " de " + quadros
                + " quadros ganharam olho fechado");
    }

    /** Cor exclusiva do focinho, usada so para localizar a cabeca. */
    private static final int FOCINHO = 0xffd393bf;

    /** Quantas linhas a cabeca desceu neste quadro, ou -1 se nao achou o focinho. */
    private static int alturaDaCabeca(BufferedImage im, int base) {
        for (int y = 0; y < im.getHeight(); y++) {
            if (im.getRGB(base + FOCINHO_X, y) == FOCINHO) {
                return y - FOCINHO_Y;
            }
        }
        return -1;
    }

    private static boolean temOlho(BufferedImage im, int base, int y) {
        if (y < 0 || y >= im.getHeight()) {
            return false;
        }
        return im.getRGB(base + OLHO_ESQ, y) == im.getRGB(OLHO_ESQ, OLHO_BASE);
    }

    /** Pelo em cima, traco de dois pixels embaixo, crescendo para o meio da cara. */
    private static void fechar(BufferedImage im, int base, int ax, int y,
                               int corDoOlho, int pelo) {
        int paraDentro = ax < LADO / 2 ? ax + 1 : ax - 1;
        if (y - 1 >= 0) {
            im.setRGB(base + ax, y - 1, pelo);
        }
        im.setRGB(base + ax, y, corDoOlho);
        im.setRGB(base + paraDentro, y, corDoOlho);
    }
}
