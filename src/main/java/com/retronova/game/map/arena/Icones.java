package com.retronova.game.map.arena;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Icones de pixel em texto, 9x9, para os simbolos do caca-niquel.
 *
 * '#' e a cor do premio, '+' e realce branco, '.' e vazio. Desenhados em
 * codigo porque nao ha arte para eles — e um simbolo de rolo precisa ser lido
 * de relance, girando, coisa que o nome escrito nao e.
 */
public final class Icones {

    private Icones() {
    }

    private static final Map<String, String[]> DESENHOS = Map.ofEntries(
            Map.entry("VIGOR", new String[]{
                    ".##...##.",
                    "####.####",
                    "#+#######",
                    "#########",
                    ".#######.",
                    "..#####..",
                    "...###...",
                    "....#....",
                    "........."}),
            Map.entry("FURIA", new String[]{
                    "......##.",
                    ".....#+#.",
                    "....#+#..",
                    "...#+#...",
                    "#.#+#....",
                    ".###.....",
                    "..#......",
                    ".#.#.....",
                    "#........"}),
            Map.entry("COURO", new String[]{
                    ".#######.",
                    "#########",
                    "#+#######",
                    "#+#######",
                    "#########",
                    ".#######.",
                    "..#####..",
                    "...###...",
                    "....#...."}),
            Map.entry("PATAS", new String[]{
                    "..##.##..",
                    "..##.##..",
                    "##.....##",
                    "##.###.##",
                    "..#####..",
                    ".##+####.",
                    ".#######.",
                    "..#####..",
                    "........."}),
            // Velocidade de ataque: espada com riscos de movimento atras.
            Map.entry("FRENESI", new String[]{
                    "......##.",
                    ".....#+#.",
                    "##..#+#..",
                    "...#+#...",
                    "###.#....",
                    "..###....",
                    "...#.....",
                    "..#.#....",
                    ".#......."}),
            // Sorte: trevo de quatro folhas, com o cabinho embaixo.
            Map.entry("INSTINTO", new String[]{
                    ".##...##.",
                    "#+##.####",
                    "####.####",
                    ".###.###.",
                    "....#....",
                    ".###.###.",
                    "####.####",
                    ".##.#.##.",
                    "....#...."}),
            Map.entry("PRESAGIO", new String[]{
                    ".........",
                    "..#####..",
                    ".#.....#.",
                    "#..###..#",
                    "#.##+##.#",
                    "#..###..#",
                    ".#.....#.",
                    "..#####..",
                    "........."}),
            Map.entry("NOVELO", new String[]{
                    "..#####..",
                    ".##+##.#.",
                    "#.##.##.#",
                    "##.##.###",
                    "#.##.##.#",
                    "##.##.###",
                    ".#.##.##.",
                    "..#####..",
                    "......##."}),
            Map.entry("GRIMORIO", new String[]{
                    "#######..",
                    "#+#####+.",
                    "#######+.",
                    "###+###+.",
                    "##+++##+.",
                    "###+###+.",
                    "#######+.",
                    "#######..",
                    "........."}),
            Map.entry("IMA", new String[]{
                    "##.....##",
                    "#+.....+#",
                    "##.....##",
                    "##.....##",
                    "##.....##",
                    ".##...##.",
                    ".#######.",
                    "..#####..",
                    "........."}),
            Map.entry("ARMADURA", new String[]{
                    ".#######.",
                    "#########",
                    "#+##.####",
                    "#+#...###",
                    "###...###",
                    ".#######.",
                    "..#####..",
                    "...###...",
                    "....#...."}),
            Map.entry("REGENERACAO", new String[]{
                    ".##...##.",
                    "####.####",
                    "###+++###",
                    "####+####",
                    ".###+###.",
                    "..#####..",
                    "...###...",
                    "....#....",
                    "........."}),
            // Critico: o mesmo estouro da particula de dano critico, para o
            // jogador ligar a carta ao numero que ve na tela.
            Map.entry("CRITICO", new String[]{
                    "....#....",
                    ".#..#..#.",
                    "..#.#.#..",
                    "...###...",
                    "####+####",
                    "...###...",
                    "..#.#.#..",
                    ".#..#..#.",
                    "....#...."}),
            // Area: ondas concentricas saindo do centro.
            Map.entry("AREA", new String[]{
                    "..#####..",
                    ".#.....#.",
                    "#..###..#",
                    "#.#...#.#",
                    "#.#.+.#.#",
                    "#.#...#.#",
                    "#..###..#",
                    ".#.....#.",
                    "..#####.."}),
            Map.entry("PRESSA", new String[]{
                    "#########",
                    ".#+++++#.",
                    "..#+++#..",
                    "...#+#...",
                    "....#....",
                    "...#.#...",
                    "..#.#.#..",
                    ".#.###.#.",
                    "#########"}),
            Map.entry("CRESCIMENTO", new String[]{
                    "....#....",
                    "...###...",
                    "..#+###..",
                    ".#######.",
                    "....#....",
                    ".##.#....",
                    "#+###....",
                    ".##.#....",
                    "....#...."}),
            // Troféu das conquistas: taça com alças, haste e base.
            Map.entry("TROFEU", new String[]{
                    ".#######.",
                    "#.#+###.#",
                    "#.#####.#",
                    "##.###.##",
                    ".#.###.#.",
                    "...###...",
                    "....#....",
                    "..#####..",
                    ".#######."}),
            // Retaliacao (Muffin): escudo com a onda saindo pelos cantos.
            Map.entry("RETALIACAO", new String[]{
                    "#.......#",
                    ".#.###.#.",
                    "..#####..",
                    ".##+####.",
                    ".#######.",
                    ".#######.",
                    "..#####..",
                    ".#.###.#.",
                    "#...#...#"}),
            // Explosao arcana (Azrael): o orbe no meio, as faiscas saindo.
            Map.entry("EXPLOSAO_ARCANA", new String[]{
                    "#...#...#",
                    ".#.....#.",
                    "...###...",
                    "..#+###..",
                    "#.#####.#",
                    "..#####..",
                    "...###...",
                    ".#.....#.",
                    "#...#...#"}),
            Map.entry("VOLEIO", new String[]{
                    "#...#...#",
                    "+#..+..#+",
                    ".#..#..#.",
                    "..#.#.#..",
                    "..#.#.#..",
                    "...###...",
                    "...###...",
                    "..#.#.#..",
                    ".#..#..#."}),
            Map.entry("RAIO", new String[]{
                    ".....###.",
                    "....###..",
                    "...###...",
                    "..#+####.",
                    ".....##..",
                    "....##...",
                    "...##....",
                    "..#......",
                    ".#......."}),
            Map.entry("TREMOR", new String[]{
                    "....#....",
                    "...###...",
                    "..#+###..",
                    "..#####..",
                    ".........",
                    "#.#...#.#",
                    ".#.#.#.#.",
                    "#########",
                    "........."}),
            Map.entry("METEORO", new String[]{
                    "#........",
                    ".#.......",
                    "..#.###..",
                    "...#####.",
                    "...##+##.",
                    "...#####.",
                    "....###..",
                    ".........",
                    "........."}),
            Map.entry("AURA", new String[]{
                    "..#####..",
                    ".#.....#.",
                    "#..+.+..#",
                    "#.......#",
                    "#...+...#",
                    "#.......#",
                    "#..+.+..#",
                    ".#.....#.",
                    "..#####.."})
    );

    private static final String[] DESCONHECIDO = {
            "..#####..",
            ".##...##.",
            ".....##..",
            "....##...",
            "...##....",
            "...##....",
            ".........",
            "...##....",
            "........."};

    /**
     * Desenha o icone com cada celula de 'celula' pixels, a partir de (x, y).
     *
     * COM RELEVO. O icone era chapado: uma cor so e uma sombra deslocada, e
     * parecia um adesivo colado no rolo. Agora ele tem quatro camadas, como um
     * sprite: sombra projetada, contorno escuro fino em volta da silhueta, o
     * preenchimento, e por cima de cada celula um brilho na borda de cima (onde
     * nao ha celula acima) e uma sombra na de baixo (onde nao ha celula abaixo).
     * A luz vem de cima, entao o icone le como uma peca em alto-relevo.
     */
    public static void desenhar(Graphics2D g, String chave, Color cor, int x, int y, int celula) {
        // DESENHADO UMA VEZ, DEPOIS SO COPIADO. Celula por celula, com as quatro
        // camadas, um icone sao uns trezentos retangulos — e a roleta mostra uns
        // quinze por quadro, mais tres fantasmas de borrao cada: dezoito mil
        // retangulos a cada quadro, o que derrubava o giro para uns 30 FPS. Uma
        // imagem pronta e um drawImage so, e o pipeline acelera.
        int fio = Math.max(1, celula / 3);
        String k = chave + "|" + cor.getRGB() + "|" + celula;
        java.awt.image.BufferedImage img = CACHE.get(k);
        if (img == null) {
            int margem = fio;
            int lado = 9 * celula + 2 * margem + celula;   // + espaco da sombra
            img = new java.awt.image.BufferedImage(lado, lado, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gi = img.createGraphics();
            desenharDireto(gi, chave, cor, margem, margem, celula);
            gi.dispose();
            CACHE.put(k, img);
        }
        g.drawImage(img, x - fio, y - fio, null);
    }

    private static final Map<String, java.awt.image.BufferedImage> CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Faixas de velocidade do borrao. Mais que isso nao se distingue girando. */
    static final int NIVEIS_DE_RASTRO = 6;

    /**
     * O icone com o RASTRO de movimento ja embutido: ele e tres copias cada vez
     * mais fracas acima, numa imagem so.
     *
     * O borrao era desenhado na hora, com tres copias transparentes por icone, e
     * cada uma trocava o modo de transparencia do desenho. Com uns quinze icones
     * no giro sao quarenta e cinco trocas por quadro, e cada troca obriga o
     * pipeline de video a esvaziar a fila — o giro caia para 60 FPS com
     * engasgos, enquanto a roleta parada ficava em 165. Pronto aqui, por faixa de
     * velocidade, cada simbolo volta a ser um desenho so.
     *
     * @param nivel   de 1 a {@link #NIVEIS_DE_RASTRO}: quanto mais, mais longo e forte
     * @param passoPx distancia entre duas copias, em pixels de tela
     */
    static void desenharComRastro(Graphics2D g, String chave, Color cor, int x, int y, int celula,
                                  int nivel, int passoPx) {
        int fio = Math.max(1, celula / 3);
        String k = chave + "|" + cor.getRGB() + "|" + celula + "|r" + nivel + "|" + passoPx;
        java.awt.image.BufferedImage img = CACHE.get(k);
        if (img == null) {
            desenhar(g, chave, cor, -10000, -10000, celula);      // garante a base no cache
            java.awt.image.BufferedImage base = CACHE.get(chave + "|" + cor.getRGB() + "|" + celula);
            int fantasmas = 3;
            img = new java.awt.image.BufferedImage(base.getWidth(), base.getHeight() + fantasmas * passoPx,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gi = img.createGraphics();
            int topoDaBase = fantasmas * passoPx;
            float forca = Math.min(0.35f, nivel * 0.07f);
            for (int f = fantasmas; f >= 1; f--) {
                gi.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, forca / f));
                gi.drawImage(base, 0, topoDaBase - f * passoPx, null);
            }
            gi.setComposite(java.awt.AlphaComposite.SrcOver);
            gi.drawImage(base, 0, topoDaBase, null);
            gi.dispose();
            CACHE.put(k, img);
        }
        g.drawImage(img, x - fio, y - fio - 3 * passoPx, null);
    }

    private static void desenharDireto(Graphics2D g, String chave, Color cor, int x, int y, int celula) {
        String[] d = DESENHOS.getOrDefault(chave, DESCONHECIDO);
        int fio = Math.max(1, celula / 3);
        Color sombra = new Color(0, 0, 0, 95);
        Color contorno = misturar(cor, new Color(0x120A18), 0.75f);
        Color claro = misturar(cor, Color.WHITE, 0.45f);
        Color escuro = misturar(cor, Color.BLACK, 0.40f);

        // 1. Sombra projetada, para baixo e para a direita.
        int off = celula / 2 + fio;
        g.setColor(sombra);
        for (int ly = 0; ly < d.length; ly++) {
            for (int lx = 0; lx < d[ly].length(); lx++) {
                if (cheia(d, lx, ly)) {
                    g.fillRect(x + lx * celula + off, y + ly * celula + off, celula, celula);
                }
            }
        }
        // 2. Contorno: cada celula engordada um fio, na cor escura.
        g.setColor(contorno);
        for (int ly = 0; ly < d.length; ly++) {
            for (int lx = 0; lx < d[ly].length(); lx++) {
                if (cheia(d, lx, ly)) {
                    g.fillRect(x + lx * celula - fio, y + ly * celula - fio, celula + 2 * fio, celula + 2 * fio);
                }
            }
        }
        // 3. Preenchimento e 4. relevo.
        for (int ly = 0; ly < d.length; ly++) {
            for (int lx = 0; lx < d[ly].length(); lx++) {
                if (!cheia(d, lx, ly)) {
                    continue;
                }
                boolean branco = d[ly].charAt(lx) == '+';
                int px = x + lx * celula, py = y + ly * celula;
                g.setColor(branco ? Color.WHITE : cor);
                g.fillRect(px, py, celula, celula);
                if (branco || celula < 2) {
                    continue;
                }
                if (!cheia(d, lx, ly - 1)) {
                    g.setColor(claro);
                    g.fillRect(px, py, celula, fio);
                }
                if (!cheia(d, lx, ly + 1)) {
                    g.setColor(escuro);
                    g.fillRect(px, py + celula - fio, celula, fio);
                }
                if (!cheia(d, lx + 1, ly)) {
                    g.setColor(escuro);
                    g.fillRect(px + celula - fio, py, fio, celula);
                }
            }
        }
    }

    private static boolean cheia(String[] d, int lx, int ly) {
        return ly >= 0 && ly < d.length && lx >= 0 && lx < d[ly].length() && d[ly].charAt(lx) != '.';
    }

    private static Color misturar(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static int lado(int celula) {
        return 9 * celula;
    }
}
