import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Retinge os sprites de interface com as cores da arte oficial.
 *
 * O jogo nasceu com uma paleta vermelha e azul-marinho, escolhida antes de a arte
 * oficial existir. Ela nao conversava com a ilustracao do titulo — que e fria,
 * escura e acinzentada — nem com os personagens, e por isso a interface sempre
 * pareceu colada por cima do jogo em vez de fazer parte dele.
 *
 * DE ONDE VEM CADA COR. Nenhuma foi escolhida a olho, e nenhuma e a cor mais
 * frequente da arte — esse foi o erro da primeira versao. O que ocupa area em
 * icons/Gato.png e o fundo noturno, entao pegar as familias dominantes jogava a
 * interface inteira no porao da imagem.
 *
 * A medida certa e por FAIXA DE LUMINOSIDADE. A arte poe cerca de 20% dos pixels
 * em cada faixa ate 45% de valor, mais 11% entre 45 e 55%, e ainda 12% acima
 * disso. Cada cor abaixo e a mais comum DENTRO da sua faixa:
 *
 *   #211A2B   faixa V 15-25%  — contorno
 *   #3E4160   faixa V 35-45%  — sombra de painel
 *   #525779   faixa V 45-55%  — corpo de painel; e o tom dos gatos iluminados
 *   #7E849C   faixa V 55-65%  — luz de painel
 *   #ABB1C1   faixa V 65-80%  — realce; a luz da lua
 *   #FCFCFC   faixa V 80-95%  — o branco do titulo
 *
 * O REALCE E FRIO, E NAO DOURADO. A primeira versao usava o ouro da coroa, que e
 * o unico calor da arte. Isolado num anel de foco ele nao le como parte da
 * ilustracao — le como amarelo de alerta sobre ardosia. O tom da lua faz o mesmo
 * trabalho de destacar e continua dentro da mesma familia fria do resto.
 *
 * COMO A TROCA E FEITA. Os sprites usam exatamente as cinco cores da paleta
 * antiga, entao e substituicao um para um: forma, contorno e sombreamento
 * continuam sendo os que o artista desenhou, so o pigmento muda. Os originais
 * ficam preservados em tools/assets/ui_original, porque o gerador e destrutivo —
 * rodar duas vezes sobre o proprio resultado nao acharia mais cor nenhuma para
 * trocar, mas rodar sobre um retoque futuro apagaria o retoque.
 *
 * Uso: java tools/GenTema.java
 */
public class GenTema {

    private static final String UI = "src/main/resources/com/retronova/resources/ui/";
    private static final String ORIGINAIS = "tools/assets/ui_original/";

    /**
     * Sprites em que o vermelho NAO era cromo de interface, e sim significado.
     *
     * A troca em bloco leva junto o que era so pintura de painel — e certo — mas
     * tambem levaria o que o vermelho estava dizendo. O frasco de vida e o caso:
     * liquido vermelho e a convencao universal de "isto cura", e um frasco de
     * ardosia nao diz nada. Aqui ele fica na familia das bandeiras da arte, que e
     * o vermelho que aquela ilustracao de fato tem.
     */
    private static final Map<String, Map<Integer, Integer>> EXCECOES = new LinkedHashMap<>();
    static {
        Map<Integer, Integer> frasco = new LinkedHashMap<>();
        frasco.put(0xBE3144, 0x7F4750);   // liquido: o vermelho das bandeiras
        frasco.put(0x872341, 0x511F27);   // fundo do liquido: a sombra delas
        EXCECOES.put("lifebottle.png", frasco);
    }

    /**
     * Sprites que NAO sao deste gerador: quem os faz e tools/GenUiAssets.java.
     *
     * Um arquivo, um dono. Estes tres sao desenhados por codigo, e a CI confere
     * que o que esta versionado bate com o que aquele gerador produz — retingir
     * por cima aqui quebrava a conferencia toda vez. Quem muda a cor deles e a
     * paleta de la, que e uma copia da do jogo.
     */
    private static final java.util.Set<String> DE_OUTRO_DONO =
            java.util.Set.of("button.png", "button_dark.png", "icon.png");

    /** Da paleta vermelha/azul para a paleta da arte. */
    private static final Map<Integer, Integer> DE_PARA = new LinkedHashMap<>();
    static {
        DE_PARA.put(0x09122C, 0x211A2B);   // contorno: azul-marinho -> ardosia funda
        DE_PARA.put(0x872341, 0x3E4160);   // sombra:   vinho        -> ardosia escura
        DE_PARA.put(0xBE3144, 0x525779);   // corpo:    vermelho     -> ardosia media
        DE_PARA.put(0xE17564, 0x7E849C);   // luz:      salmao       -> ardosia clara
        DE_PARA.put(0xFFF7D1, 0xFCFCFC);   // texto:    creme        -> branco do titulo
    }

    public static void main(String[] a) throws Exception {
        new File(ORIGINAIS).mkdirs();
        List<File> tocados = new ArrayList<>();
        File[] arquivos = new File(UI).listFiles((d, n) -> n.endsWith(".png"));
        if (arquivos == null) {
            System.err.println("Rode a partir da raiz do repositorio.");
            System.exit(1);
        }
        for (File f : arquivos) {
            if (DE_OUTRO_DONO.contains(f.getName())) {
                continue;
            }
            BufferedImage src = original(f);
            Map<Integer, Integer> mapa = new LinkedHashMap<>(DE_PARA);
            mapa.putAll(EXCECOES.getOrDefault(f.getName(), Map.of()));
            int trocados = 0;
            BufferedImage o = new BufferedImage(src.getWidth(), src.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int p = src.getRGB(x, y);
                    Integer nova = mapa.get(p & 0xFFFFFF);
                    if (nova != null && (p >>> 24) != 0) {
                        o.setRGB(x, y, (p & 0xFF000000) | nova);
                        trocados++;
                    } else {
                        o.setRGB(x, y, p);
                    }
                }
            }
            if (trocados == 0) {
                continue;                  // sprite que nunca usou a paleta antiga
            }
            ImageIO.write(o, "png", f);
            tocados.add(f);
            System.out.printf("  %-18s %d pixels%s%n", f.getName(), trocados,
                    EXCECOES.containsKey(f.getName()) ? "  (excecao: mantem vermelho)" : "");
        }
        System.out.println(tocados.size() + " sprites retingidos");
    }

    /** Le a arte limpa, preservando uma copia na primeira vez. */
    static BufferedImage original(File atual) throws Exception {
        File guardado = new File(ORIGINAIS + atual.getName());
        if (!guardado.exists()) {
            ImageIO.write(ImageIO.read(atual), "png", guardado);
        }
        return ImageIO.read(guardado);
    }
}
