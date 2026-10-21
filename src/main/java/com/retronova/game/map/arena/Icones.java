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
final class Icones {

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
            Map.entry("FRENESI", new String[]{
                    "....####.",
                    "...#+##..",
                    "..####...",
                    ".#######.",
                    "...####..",
                    "..####...",
                    ".####....",
                    ".##......",
                    "#........"}),
            Map.entry("INSTINTO", new String[]{
                    ".##...##.",
                    "#+##.####",
                    "####.####",
                    ".##.#.##.",
                    "...#+#...",
                    ".##.#.##.",
                    "####.####",
                    "####.####",
                    ".##...##."}),
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
            Map.entry("CRITICO", new String[]{
                    "...###...",
                    ".##...##.",
                    ".#..#..#.",
                    "#..###..#",
                    "#.##+##.#",
                    "#..###..#",
                    ".#..#..#.",
                    ".##...##.",
                    "...###..."}),
            Map.entry("AREA", new String[]{
                    "#.......#",
                    ".#.....#.",
                    "..#...#..",
                    "....+....",
                    "...#+#...",
                    "....+....",
                    "..#...#..",
                    ".#.....#.",
                    "#.......#"}),
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
     * Icones ja pintados, por chave, cor e tamanho de celula.
     *
     * PINTAR PIXEL A PIXEL A CADA QUADRO ERA O QUE TRAVAVA A ROLETA. Cada icone
     * custava uns oitenta fillRect (sombra e cor), e o rolo girando desenha cada
     * simbolo mais tres vezes de borrao — perto de setenta icones por quadro, uns
     * seis mil fillRect, trocando de composicao alfa no meio. O FPS caia justo no
     * momento de mais suspense. Pintado uma vez numa imagem, cada icone vira um
     * drawImage so, e o pipeline acelerado guarda a imagem na placa.
     */
    private static final Map<String, BufferedImage> PRONTOS = new ConcurrentHashMap<>();

    /**
     * Desenha o icone com cada celula de 'celula' pixels, a partir de (x, y),
     * com uma sombra escura deslocada para ele saltar do fundo.
     */
    static void desenhar(Graphics2D g, String chave, Color cor, int x, int y, int celula) {
        String id = chave + '|' + cor.getRGB() + '|' + celula;
        BufferedImage img = PRONTOS.computeIfAbsent(id, k -> pintar(chave, cor, celula));
        g.drawImage(img, x, y, null);
    }

    private static BufferedImage pintar(String chave, Color cor, int celula) {
        String[] d = DESENHOS.getOrDefault(chave, DESCONHECIDO);
        int deslocamento = celula / 2 + 1;           // a sombra sai para baixo e a direita
        int lado = lado(celula) + deslocamento;
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color sombra = new Color(0, 0, 0, 90);
        for (int passe = 0; passe < 2; passe++) {
            int dx = passe == 0 ? deslocamento : 0;
            for (int ly = 0; ly < d.length; ly++) {
                for (int lx = 0; lx < d[ly].length(); lx++) {
                    char ch = d[ly].charAt(lx);
                    if (ch == '.') {
                        continue;
                    }
                    g.setColor(passe == 0 ? sombra : (ch == '+' ? Color.WHITE : cor));
                    g.fillRect(lx * celula + dx, ly * celula + dx, celula, celula);
                }
            }
        }
        g.dispose();
        return img;
    }

    static int lado(int celula) {
        return 9 * celula;
    }
}
