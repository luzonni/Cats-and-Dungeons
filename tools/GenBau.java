import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/**
 * Gera os quadros do baú abrindo, para a tela de abertura (AberturaDoBau).
 *
 * ---------------------------------------------------------------------------
 * DE ONDE VEM
 *
 * Os três quadros "chest_full_open_anim" do 16x16 DungeonTileset II, de 0x72
 * (CC0 1.0, ver docs/TERCEIROS.md), que estão em tools/assets/fonte/dtii. O
 * baú fechado, a tampa entreaberta e a tampa aberta com o brilho de dentro.
 *
 * REMAPEADOS PARA A PALETA DO BAÚ DO JOGO, e não copiados como estão: o baú do
 * pacote é laranja, e o que cai no chão (furniture/chest.png) é madeira escura
 * com ferragens de aço frio. Cada cor do pacote tem um papel (contorno, três
 * tons de madeira, metal, brilho) e vai para a cor do mesmo papel no baú do
 * jogo — assim o baú que abre na tela é o mesmo que o jogador pegou no chão.
 *
 * Os dois pontos de cor dentro do baú aberto (as joias do pacote) viram brilho
 * branco: o prêmio é revelado pela tela, e uma joia verde dentro do baú
 * prometeria um prêmio verde.
 *
 * Saída: sprites/objects/furniture/bau_abrindo.png, uma tira de 48x16 com os
 * três quadros lado a lado.
 *
 *     java tools/GenBau.java
 */
public class GenBau {

    private static final String FONTE = "tools/assets/fonte/dtii/0x72_DungeonTilesetII_v1.7/frames/";
    private static final String SAIDA =
            "src/main/resources/com/retronova/resources/sprites/objects/furniture/bau_abrindo.png";

    /** Cor do pacote -> cor do baú do jogo, pelo papel de cada uma. */
    private static final Map<Integer, Integer> CORES = Map.ofEntries(
            Map.entry(0x222222, 0x1C130F),   // contorno
            Map.entry(0x62232F, 0x33211A),   // madeira na sombra
            Map.entry(0x8F4029, 0x63432D),   // madeira
            Map.entry(0xC56025, 0x7D5738),   // madeira na luz
            Map.entry(0xEE8E2E, 0x4A6B70),   // ferragem
            Map.entry(0xFACB3E, 0x6D8F92),   // ferragem na luz
            Map.entry(0xFFAD3B, 0x6D8F92),   // ferragem na luz, dentro da tampa
            Map.entry(0xFDF7ED, 0xABB1C1),   // brilho (Palette.ACCENT)
            Map.entry(0x72D6CE, 0xFCFCFC),   // joia -> brilho (Palette.TEXT)
            Map.entry(0x97DA3F, 0xFCFCFC));  // joia -> brilho

    public static void main(String[] args) throws Exception {
        BufferedImage saida = new BufferedImage(48, 16, BufferedImage.TYPE_INT_ARGB);
        for (int q = 0; q < 3; q++) {
            BufferedImage quadro = ImageIO.read(new File(FONTE + "chest_full_open_anim_f" + q + ".png"));
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int p = quadro.getRGB(x, y);
                    if ((p >>> 24) == 0) {
                        continue;
                    }
                    Integer nova = CORES.get(p & 0xFFFFFF);
                    if (nova == null) {
                        throw new IllegalStateException(String.format(
                                "cor #%06X do quadro %d sem papel no mapa", p & 0xFFFFFF, q));
                    }
                    saida.setRGB(q * 16 + x, y, (p & 0xFF000000) | nova);
                }
            }
        }
        ImageIO.write(saida, "png", new File(SAIDA));
        System.out.println("gravado " + SAIDA);
    }
}
