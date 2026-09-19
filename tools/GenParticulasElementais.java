import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * As duas partículas elementais que faltavam: terra e ar.
 *
 * ---------------------------------------------------------------------------
 * QUATRO DAS SEIS JÁ ESTAVAM NO REPOSITÓRIO, E DUAS DELAS SEM USO.
 *
 * Antes de sair procurando arte fora, vale olhar a pasta: `particle/` já tinha
 * `ice.png` — um floco que estilhaça em cruzinhas — e `water.png` — uma gota que
 * estoura em respingos. Nenhum dos dois era referenciado por uma linha de código
 * sequer. São impactos elementais prontos, no traço da casa, órfãos exatamente
 * como os WAV elementais estavam antes de virarem som de golpe.
 *
 * `fire.png` também já existia (a chama da queimadura) e `damagemobs.png` é o
 * brilho dourado que serve à lendária. Sobravam TERRA e AR.
 *
 * Importar um pacote inteiro de fora para depois descobrir que o jogo já tinha
 * a arte, melhor casada com o próprio traço, seria trocar coisa boa por coisa
 * parecida. Este gerador faz só o que falta.
 *
 * ---------------------------------------------------------------------------
 * AR — importado
 *
 * Vem de "Pixelart Spells" (DevWizard, CC0 1.0). O pacote é de PROJÉTEIS, não de
 * impactos: quase tudo ali voa para a direita, o que não serve para um estouro
 * no lugar onde a arma acertou. A exceção é o `Wind Bolt`, uma lufada em meia-lua
 * com fagulhas soltas, que lida como rajada em qualquer direção — e cuja paleta
 * (#94FFF9) já é praticamente a cor do elemento AR (#D7FFE4).
 *
 * A ÚNICA MUDANÇA É A MORTE. Os seis quadros do original são um CICLO: todos têm
 * mais ou menos o mesmo peso, porque o projétil precisa poder repetir enquanto
 * voa. Um impacto não repete — ele nasce, se espalha e some. Sem a rampa de
 * dissipação o efeito terminaria cheio, sumindo de um quadro para o outro, que é
 * o defeito que todas as folhas próprias do jogo já evitam.
 *
 * ---------------------------------------------------------------------------
 * TERRA — recolorido do que já havia
 *
 * Sai de `dust.png`, a nuvem de poeira da máquina de chiclete: um branco puro a
 * 70 de alfa, isto é, uma FORMA sem cor própria. É a base ideal para pintar.
 *
 * Duas escolhas, e as duas contra copiar a poeira tal como está:
 *
 *   A OPACIDADE SOBE. Setenta de alfa é fumaça — coisa que se atravessa. Terra
 *   é torrão, e torrão tapa o que está atrás. Sem isso o impacto de terra seria
 *   o único dos seis que mal se vê contra o chão claro da cisterna.
 *
 *   SÃO DOIS MARRONS, E NÃO UM. O miolo leva a cor do elemento e a borda leva um
 *   tom escuro. Uma silhueta chapada numa cor só lê como mancha; a borda mais
 *   escura é o que faz o olho ler volume, e é como o resto da arte do jogo é
 *   feita. A separação é por VIZINHANÇA (pixel cercado de pixel é miolo), e não
 *   por posição, que mudaria de quadro para quadro.
 *
 * Uso: java tools/GenParticulasElementais.java
 */
public class GenParticulasElementais {

    private static final String PARTICULAS =
            "src/main/resources/com/retronova/resources/sprites/objects/particle/";

    private static final String PACOTE = "tools/assets/fonte/pixelart_spells.zip";

    private static final String WIND_BOLT = "Pixelart Spells/PNG Files/Wind Bolt.png";

    /**
     * Quanto de cada quadro sobrevive.
     *
     * Os dois primeiros inteiros — o estouro precisa de peso no começo, senão o
     * golpe não parece ter conectado. Daí em diante despenca. A curva é a mesma
     * para as duas folhas de propósito: o que define o elemento é a cor e a
     * forma, e não o tempo; seis impactos morrendo em ritmos diferentes fariam
     * uns parecerem mais fortes que outros sem que o dano mudasse.
     */
    private static final float[] DISSIPACAO = {1f, 1f, 0.85f, 0.62f, 0.38f, 0.16f};

    /** A cor do elemento TERRA, igual à de items/Elemento.java. */
    private static final int MIOLO = 0xB07E41;

    /** O tom de sombra do torrão. */
    private static final int BORDA = 0x6E4C28;

    /** Poeira é fumaça a 70; torrão tapa o que está atrás. */
    private static final int OPACIDADE = 215;

    public static void main(String[] args) throws Exception {
        System.out.println("Particulas elementais");
        ar();
        terra();
        System.out.println("  (fogo, gelo, agua e lendaria ja existiam em particle/)");
    }

    private static void ar() throws Exception {
        File pacote = new File(PACOTE);
        if (!pacote.isFile()) {
            System.err.println("  falta " + PACOTE + " — baixe de "
                    + "https://opengameart.org/sites/default/files/pixelart_spells_1.zip");
            return;
        }
        BufferedImage folha;
        try (ZipFile zip = new ZipFile(pacote)) {
            ZipEntry entrada = zip.getEntry(WIND_BOLT);
            if (entrada == null) {
                System.err.println("  o pacote nao tem " + WIND_BOLT);
                return;
            }
            try (InputStream in = zip.getInputStream(entrada)) {
                folha = ImageIO.read(in);
            }
        }
        dissipar(folha);
        gravar(folha, "air");
    }

    private static void terra() throws Exception {
        BufferedImage poeira = ImageIO.read(new File(PARTICULAS + "dust.png"));
        int l = poeira.getWidth();
        int a = poeira.getHeight();
        BufferedImage terra = new BufferedImage(l, a, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < a; y++) {
            for (int x = 0; x < l; x++) {
                if (!opaco(poeira, x, y)) {
                    continue;
                }
                // CERCADO DE POEIRA POR TODOS OS LADOS é miolo; o resto é contorno.
                boolean dentro = opaco(poeira, x - 1, y) && opaco(poeira, x + 1, y)
                        && opaco(poeira, x, y - 1) && opaco(poeira, x, y + 1);
                terra.setRGB(x, y, (OPACIDADE << 24) | (dentro ? MIOLO : BORDA));
            }
        }
        dissipar(terra);
        gravar(terra, "earth");
    }

    private static boolean opaco(BufferedImage im, int x, int y) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) {
            return false;
        }
        return (im.getRGB(x, y) >>> 24) > 0;
    }

    /**
     * Aplica a rampa de morte quadro a quadro.
     *
     * Os quadros são quadrados do tamanho da altura, que é como toda folha de
     * partícula do jogo é montada.
     */
    private static void dissipar(BufferedImage folha) {
        int lado = folha.getHeight();
        int quadros = folha.getWidth() / lado;
        for (int q = 0; q < quadros; q++) {
            float resto = DISSIPACAO[Math.min(q, DISSIPACAO.length - 1)];
            if (resto >= 1f) {
                continue;
            }
            for (int y = 0; y < lado; y++) {
                for (int x = q * lado; x < (q + 1) * lado; x++) {
                    int argb = folha.getRGB(x, y);
                    int alfa = argb >>> 24;
                    if (alfa == 0) {
                        continue;
                    }
                    folha.setRGB(x, y,
                            (Math.round(alfa * resto) << 24) | (argb & 0xFFFFFF));
                }
            }
        }
    }

    private static void gravar(BufferedImage folha, String nome) throws Exception {
        File destino = new File(PARTICULAS + nome + ".png");
        ImageIO.write(folha, "png", destino);
        System.out.printf("  %-6s %dx%d (%d quadros)%n", nome + ".png",
                folha.getWidth(), folha.getHeight(),
                folha.getWidth() / folha.getHeight());
    }
}
