import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Gera os poucos sprites de cenario que nenhum pacote base cobre.
 *
 *     java tools/GenCenario.java
 *
 * Utilitario de partida, fora do build. Os PNGs sao conteudo e podem ser
 * repintados a mao depois - rodar de novo sobrescreve.
 *
 * ESCOPO. Quase tudo passou para tools/GenKenney.java, que deriva os sprites de
 * pacotes CC0 com qualidade que este gerador nao alcanca — parede, piso, pilar,
 * portao, entulho, barril, e agora tambem o fogo, que ganhou chama animada.
 * Sobraram aqui so a ossada e a corrente, que nenhum dos pacotes tem. Os dois
 * geradores nao podem escrever o mesmo arquivo, ou rodar um desfaz o outro em
 * silencio.
 *
 * As tres regras que este gerador implementa:
 *
 *  1. PERSPECTIVA 3/4. Todo volume tem face de TOPO e face FRONTAL. A face de
 *     topo e sensivelmente mais clara. E o contraste entre as duas que cria a
 *     profundidade - sem face de topo o objeto fica chapado.
 *  2. ANCORAGEM. Todo prop projeta uma sombra curta no chao, sempre para o mesmo
 *     lado. Sem sombra o cerebro le o objeto como deitado.
 *  3. PALETA DE CENARIO. Nenhum carmesim de UI entra aqui.
 */
public class GenCenario {

    // Rampa de pedra fria.
    static final int CONTORNO= 0xff161d24;
    static final int ESCURO  = 0xff243840;
    static final int MEIO    = 0xff355157;
    static final int CLARO   = 0xff4a6b70;
    static final int SOMBRA  = 0x552a2233;   // sombra projetada, semitransparente

    public static void main(String[] args) throws Exception {
        File furniture = new File("src/main/resources/com/retronova/resources/sprites/objects/furniture");
        if (!furniture.isDirectory()) {
            System.err.println("Rode a partir da raiz do repositorio.");
            System.exit(1);
        }
        ImageIO.write(ossos(), "png", new File(furniture, "bones.png"));
        ImageIO.write(corrente(), "png", new File(furniture, "chain.png"));
        System.out.println("ok: bones, chain");
    }

    // ------------------------------------------------------------- utilidades

    static void rect(BufferedImage img, int x, int y, int w, int h, int cor) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                if (i >= 0 && j >= 0 && i < img.getWidth() && j < img.getHeight()) {
                    img.setRGB(i, j, cor);
                }
            }
        }
    }


    /** Sombra curta no chão, sempre deslocada para a direita e para baixo. */
    static void sombra(BufferedImage img, int x, int y, int w) {
        rect(img, x + 2, y, w, 2, SOMBRA);
        rect(img, x + 4, y + 2, w - 4, 1, SOMBRA);
    }

    // ----------------------------------------------------------------- props

    /**
     * Pilar 32x32. A base fica na linha 31 e o desenho é ancorado por ela, então
     * o pilar sobe um tile acima da célula. Capitel e base transbordam a célula
     * de propósito: é o que dá presença arquitetônica.
     */
    // ----------------------------------------------------------------- props


    /** Entulho 16x16. Set dressing barato: sugere ruína e quebra a repetição. */
    /** Ossos no chão. Conta que alguém não voltou. */
    static BufferedImage ossos() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        final int OSSO = 0xffc9c4b0, OSSO_SOMBRA = 0xff8e8a79;
        sombra(img, 3, 12, 9);
        rect(img, 3, 10, 8, 2, OSSO);
        rect(img, 3, 12, 8, 1, OSSO_SOMBRA);
        rect(img, 2, 9, 2, 4, OSSO);
        rect(img, 10, 9, 2, 4, OSSO);
        rect(img, 6, 6, 4, 4, OSSO);        // crânio
        rect(img, 7, 8, 1, 1, CONTORNO);
        rect(img, 9, 8, 1, 1, CONTORNO);
        return img;
    }

    /** Corrente pendurada na parede. Sugere que algo foi contido aqui. */
    static BufferedImage corrente() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 13; y += 3) {
            rect(img, 7, y, 3, 2, CLARO);
            rect(img, 7, y + 2, 3, 1, ESCURO);
        }
        rect(img, 6, 12, 5, 3, MEIO);       // argola no fim
        rect(img, 7, 13, 3, 1, CONTORNO);
        return img;
    }

}
