package com.retronova.menus.controles;

import com.retronova.engine.graphics.Acelerada;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.inputs.gamepad.Comando;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * O controle no padrão Xbox, em pixel art, para a tela de Controles.
 *
 * DESENHADO AQUI, E NÃO BAIXADO. Os pacotes livres têm o controle em 16 px (um
 * ícone, não um mapa) ou com o círculo do logotipo no meio — o único em pixel art
 * com licença livre, o de Anton Revin (CC0), tem 48x33 e o logo. Nenhum dá para
 * ligar uma linha a cada botão. Este segue o layout padrão — analógico esquerdo
 * alto e direcional baixo, botões de face altos e analógico direito baixo, View
 * e Menu no meio, bumpers e gatilhos em cima — com a paleta do jogo e as cores de
 * sempre nos botões de face. O botão do meio é um botão liso: sem logotipo, que é
 * marca da Microsoft.
 *
 * O VOLUME VEM DA FORMA, NÃO DA BORDA. A primeira versão clareava a borda de cima
 * e escurecia a de baixo de cada peça, e o controle saía chapado, uma placa com
 * botões colados. Agora cada peça é esculpida ({@link #esculpir}): a distância de
 * cada pixel até a borda vira uma altura arredondada, a inclinação dessa altura
 * vira a direção da superfície, e uma luz só, do alto à esquerda (a da interface
 * inteira), dá o tom — quantizado numa rampa curta da paleta, como um pixel
 * artista faria à mão. As empunhaduras inclinam para fora, os analógicos têm poço
 * e cabeça, os botões de face são domos com a letra, e uma sombra assenta tudo.
 *
 * DESENHADO UMA VEZ, NA RESOLUÇÃO NATIVA (1 pixel = 1 pixel de arte), e ampliado
 * por fator inteiro sem suavização. Há uma imagem com tudo apagado e uma por peça
 * acesa; o realce troca uma imagem inteira pela outra, sem pintar por quadro.
 *
 * A GEOMETRIA É UMA SÓ: {@link #PECAS} diz onde cada peça fica, de onde sai a
 * linha dela e para que lado. O desenho, o realce, o mouse e as linhas leem dali;
 * o DesenhoDoControleTest confere que nenhuma linha passa por cima de outra peça.
 */
public final class DesenhoDoControle {

    /** Tamanho do desenho, em pixels nativos: o sprite da carcaça (118x76) e a sombra embaixo. */
    public static final int LARGURA = 118, ALTURA = 80;

    /** As cores de face, puxadas para a luz fria da paleta. */
    private static final Color VERDE = new Color(0x5E9A55), VERMELHO = new Color(0xA8505A),
            AZUL = new Color(0x4F78B0), AMARELO = new Color(0xC2A04A);
    /** O tom das peças escuras (analógicos, direcional): um passo acima do fundo, para não sumirem no corpo. */
    private static final Color PECA_ESCURA = misturar(Palette.DEEP, Palette.MAIN, 0.3f);
    /** De onde vem a luz: alto à esquerda, um pouco de frente (x, y, z; y para baixo). */
    private static final double[] LUZ = normalizar(-0.55, -0.75, 0.9);
    /** O quanto a sombra projetada desce, em pixels nativos, e a força dela (0-255). */
    private static final int SOMBRA_DESCE = 3, SOMBRA_ALFA = 110;

    /** Para onde sai a linha de uma peça: uma das colunas, ou reta para cima ou para baixo. */
    public enum Lado { ESQUERDA, DIREITA, CIMA, BAIXO }

    /**
     * Uma peça: o retângulo dela, o ponto de onde a linha sai, para onde a linha vai
     * e de que lado da ponta fica o texto.
     */
    public record Peca(Rectangle area, int ancoraX, int ancoraY, Lado lado, boolean textoAEsquerda) {
        static Peca esquerda(Rectangle area, int ax, int ay) {
            return new Peca(area, ax, ay, Lado.ESQUERDA, true);
        }

        static Peca direita(Rectangle area, int ax, int ay) {
            return new Peca(area, ax, ay, Lado.DIREITA, false);
        }
    }

    /**
     * As peças com rótulo, nas posições do sprite da carcaça. L3 e R3 ficam de
     * fora: são o clique dos analógicos, sem desenho próprio, e o jogo não usa
     * nenhum dos dois.
     *
     * A âncora fica na borda da peça voltada para onde a linha vai, numa altura (ou
     * coluna de pixels) em que a reta até fora do desenho não atravessa outra peça.
     * View e Menu ficam na altura do analógico esquerdo e saem por cima; o X,
     * cercado pelos botões de face, e o analógico direito saem por baixo.
     */
    public static final Map<Comando, Peca> PECAS = new EnumMap<>(Comando.class);

    static {
        PECAS.put(Comando.LT, Peca.esquerda(new Rectangle(18, 0, 19, 9), 18, 4));
        PECAS.put(Comando.LB, Peca.esquerda(new Rectangle(6, 9, 23, 10), 6, 14));
        PECAS.put(Comando.ANALOGICO_ESQ, Peca.esquerda(new Rectangle(10, 29, 20, 20), 10, 38));
        PECAS.put(Comando.DPAD, Peca.esquerda(new Rectangle(33, 48, 15, 15), 33, 55));
        PECAS.put(Comando.VIEW, new Peca(new Rectangle(47, 36, 5, 4), 49, 36, Lado.CIMA, true));
        PECAS.put(Comando.MENU, new Peca(new Rectangle(65, 36, 5, 4), 67, 36, Lado.CIMA, false));

        PECAS.put(Comando.RT, Peca.direita(new Rectangle(81, 0, 20, 9), 100, 4));
        PECAS.put(Comando.RB, Peca.direita(new Rectangle(89, 9, 23, 10), 111, 14));
        PECAS.put(Comando.Y, Peca.direita(new Rectangle(90, 22, 8, 8), 97, 25));
        PECAS.put(Comando.B, Peca.direita(new Rectangle(99, 31, 8, 8), 106, 34));
        PECAS.put(Comando.A, Peca.direita(new Rectangle(90, 40, 8, 8), 97, 43));
        PECAS.put(Comando.X, new Peca(new Rectangle(81, 31, 8, 8), 87, 38, Lado.BAIXO, false));
        PECAS.put(Comando.ANALOGICO_DIR, new Peca(new Rectangle(68, 46, 19, 19), 77, 64, Lado.BAIXO, true));
    }

    /**
     * O botão do meio, liso, por cima do logotipo que o sprite traz: só enfeite,
     * sem ação e sem rótulo. O logotipo é marca da Microsoft e não aparece.
     */
    private static final Rectangle BOTAO_DO_MEIO = new Rectangle(51, 16, 15, 14);
    /**
     * A área dos botões de face. Os anéis que o sprite traz ali ficam em posições
     * um pouco tortas; eles são apagados, e os botões são pintados num losango
     * exato (mesmo eixo para Y e A, mesmo vão para X e B), em PECAS.
     */
    private static final Rectangle AREA_DOS_BOTOES = new Rectangle(80, 21, 29, 29);
    /** As cores do sprite da carcaça (paleta Juice 32), trocadas pelas da nossa na pintura. */
    private static final int SPRITE_CORPO = 0x384766, SPRITE_PAINEL = 0x6B7A99, SPRITE_CONTORNO = 0xFFFFF2;

    private static BufferedImage apagado;
    private static final Map<Comando, BufferedImage> ACESOS = new EnumMap<>(Comando.class);
    /** As cópias ampliadas, por escala: [0] o apagado, [1 + ordinal] cada peça acesa. */
    private static final Map<Integer, BufferedImage[]> AMPLIADOS = new java.util.HashMap<>();
    /** As peças sem ação, apagadas no desenho; a cópia é refeita se mudarem. */
    private static Set<Comando> semAcaoDoCache;

    private DesenhoDoControle() {
    }

    /**
     * O desenho na escala {@code s}, com {@code acesa} realçada (ou nenhuma, com
     * nulo). As peças em {@code semAcao} saem esmaecidas.
     */
    public static synchronized BufferedImage imagem(int s, Comando acesa, Set<Comando> semAcao) {
        if (!semAcao.equals(semAcaoDoCache)) {
            semAcaoDoCache = Set.copyOf(semAcao);
            apagado = null;
            ACESOS.clear();
            AMPLIADOS.clear();
        }
        BufferedImage[] porPeca = AMPLIADOS.computeIfAbsent(s, k -> new BufferedImage[Comando.values().length + 1]);
        int i = acesa == null || !PECAS.containsKey(acesa) ? 0 : 1 + acesa.ordinal();
        if (porPeca[i] == null) {
            BufferedImage nativa = i == 0 ? apagado() : ACESOS.computeIfAbsent(acesa, c -> pintar(c, semAcaoDoCache));
            porPeca[i] = Acelerada.de(ampliar(nativa, s));
        }
        return porPeca[i];
    }

    private static BufferedImage apagado() {
        if (apagado == null) {
            apagado = pintar(null, semAcaoDoCache);
        }
        return apagado;
    }

    private static BufferedImage ampliar(BufferedImage img, int s) {
        BufferedImage o = new BufferedImage(img.getWidth() * s, img.getHeight() * s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(img, 0, 0, o.getWidth(), o.getHeight(), null);
        g.dispose();
        return o;
    }

    // ------------------------------------------------------------------ pintura

    /** Pinta o controle inteiro, com uma peça acesa (ou nenhuma). */
    static BufferedImage pintar(Comando acesa, Set<Comando> semAcao) {
        int[] px = new int[LARGURA * ALTURA];

        // A CARCAÇA É A DO SPRITE (lightUP, ver TERCEIROS): a silhueta, o painel
        // claro de cima e os ombros com gatilhos e bumpers. Ela vem chapada, em outra
        // paleta; aqui ganha o volume e as cores do jogo. O corpo e o painel são
        // esculpidos juntos, para a luz correr pelos dois sem emenda.
        int[][] sprite = sprite();
        boolean[][] corpo = new boolean[ALTURA][LARGURA];
        boolean[][] painel = new boolean[ALTURA][LARGURA];
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                int p = sprite[y][x];
                corpo[y][x] = p >= 0 && p != SPRITE_CONTORNO;
                // O traço claro do sprite (anéis, cruz, logotipo) some: as peças são
                // pintadas por cima, e no lugar do logotipo vale o painel em volta.
                painel[y][x] = (p == SPRITE_PAINEL && !AREA_DOS_BOTOES.contains(x, y))
                        || (corpo[y][x] && p != SPRITE_CORPO && BOTAO_DO_MEIO.contains(x, y));
            }
        }
        esculpir(px, corpo, rampaDoCorpo(), 6, false, null);
        esculpir(px, corpo, rampaDe(misturar(Palette.MAIN, Palette.LIGHT, 0.45f)), 6, false, painel);
        for (Comando c : new Comando[] {Comando.LT, Comando.RT, Comando.LB, Comando.RB}) {
            if (c == acesa || semAcao.contains(c)) {
                boolean[][] daPeca = new boolean[ALTURA][LARGURA];
                Rectangle r = PECAS.get(c).area();
                for (int y = r.y; y < r.y + r.height; y++) {
                    for (int x = r.x; x < r.x + r.width; x++) {
                        daPeca[y][x] = painel[y][x];
                    }
                }
                esculpir(px, corpo, rampa(misturar(Palette.MAIN, Palette.LIGHT, 0.45f), c, acesa, semAcao),
                        6, false, daPeca);
            }
        }

        analogico(px, Comando.ANALOGICO_ESQ, acesa, semAcao);
        analogico(px, Comando.ANALOGICO_DIR, acesa, semAcao);
        dpad(px, acesa, semAcao);
        for (Comando c : new Comando[] {Comando.VIEW, Comando.MENU}) {
            Rectangle r = PECAS.get(c).area();
            poco(px, new RoundRectangle2D.Double(r.x - 1, r.y - 1, r.width + 2, r.height + 2, 5, 5));
            esculpir(px, mascara(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 4, 4)),
                    rampa(Palette.DEEP, c, acesa, semAcao), 2, false, null);
        }
        Rectangle m = BOTAO_DO_MEIO;
        poco(px, new Ellipse2D.Double(m.x - 1, m.y - 1, m.width + 2, m.height + 2));
        esculpir(px, mascara(new Ellipse2D.Double(m.x, m.y, m.width, m.height)),
                rampaDe(misturar(Palette.MAIN, Palette.LIGHT, 0.3f)), 4, false, null);

        face(px, Comando.Y, AMARELO, LETRA_Y, acesa, semAcao);
        face(px, Comando.X, AZUL, LETRA_X, acesa, semAcao);
        face(px, Comando.B, VERMELHO, LETRA_B, acesa, semAcao);
        face(px, Comando.A, VERDE, LETRA_A, acesa, semAcao);

        BufferedImage img = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_ARGB);
        sombra(img, px);
        img.setRGB(0, 0, LARGURA, ALTURA, mesclar(img.getRGB(0, 0, LARGURA, ALTURA, null, 0, LARGURA), px), 0, LARGURA);
        return img;
    }

    private static int[][] spriteCarregado;

    /** O sprite da carcaça, cor a cor (RGB sem alfa), com -1 onde é transparente. */
    private static int[][] sprite() {
        if (spriteCarregado == null) {
            BufferedImage img = SpriteHandler.getSprite("controles", "lightup_xbox_one", 1);
            int[][] cores = new int[ALTURA][LARGURA];
            for (int y = 0; y < ALTURA; y++) {
                for (int x = 0; x < LARGURA; x++) {
                    int p = x < img.getWidth() && y < img.getHeight() ? img.getRGB(x, y) : 0;
                    cores[y][x] = (p >>> 24) == 0 ? -1 : p & 0xFFFFFF;
                }
            }
            // FUROS VIRAM CORPO. A letra do B no sprite é recortada (o fundo aparece
            // por ela), e o furo virava contorno e sombra em cima do botão. Só é
            // transparente o que se alcança por fora do desenho.
            boolean[][] fora = new boolean[ALTURA][LARGURA];
            java.util.ArrayDeque<int[]> fila = new java.util.ArrayDeque<>();
            for (int x = 0; x < LARGURA; x++) {
                fila.add(new int[] {x, 0});
                fila.add(new int[] {x, ALTURA - 1});
            }
            for (int y = 0; y < ALTURA; y++) {
                fila.add(new int[] {0, y});
                fila.add(new int[] {LARGURA - 1, y});
            }
            while (!fila.isEmpty()) {
                int[] p = fila.poll();
                int x = p[0], y = p[1];
                if (x < 0 || y < 0 || x >= LARGURA || y >= ALTURA || fora[y][x] || cores[y][x] != -1) {
                    continue;
                }
                fora[y][x] = true;
                fila.add(new int[] {x + 1, y});
                fila.add(new int[] {x - 1, y});
                fila.add(new int[] {x, y + 1});
                fila.add(new int[] {x, y - 1});
            }
            for (int y = 0; y < ALTURA; y++) {
                for (int x = 0; x < LARGURA; x++) {
                    if (cores[y][x] == -1 && !fora[y][x]) {
                        cores[y][x] = SPRITE_CORPO;
                    }
                }
            }
            spriteCarregado = cores;
        }
        return spriteCarregado;
    }

    /** O analógico: o poço escuro afundado na carcaça e a cabeça em domo, com a borda que pega luz. */
    private static void analogico(int[] px, Comando c, Comando acesa, Set<Comando> semAcao) {
        Rectangle r = PECAS.get(c).area();
        poco(px, new Ellipse2D.Double(r.x, r.y, r.width, r.height));
        Color[] cabeca = rampa(PECA_ESCURA, c, acesa, semAcao);
        esculpir(px, mascara(new Ellipse2D.Double(r.x + 2, r.y + 1, r.width - 4, r.height - 4)), cabeca, 4, false, null);
        // O miolo côncavo da cabeça: um anel de luz embaixo, porque a borda de baixo
        // do miolo é a que fica de frente para a luz.
        esculpir(px, mascara(new Ellipse2D.Double(r.x + 5, r.y + 4, r.width - 10, r.height - 10)), cabeca, 2, true, null);
    }

    /** O direcional em cruz, saltado de um poço redondo. */
    private static void dpad(int[] px, Comando acesa, Set<Comando> semAcao) {
        Rectangle r = PECAS.get(Comando.DPAD).area();
        int braco = 5, meio = (r.width - braco) / 2;
        poco(px, new Ellipse2D.Double(r.x - 1, r.y - 1, r.width + 2, r.height + 2));
        java.awt.geom.Area cruz = new java.awt.geom.Area(new RoundRectangle2D.Double(r.x + meio, r.y, braco, r.height, 3, 3));
        cruz.add(new java.awt.geom.Area(new RoundRectangle2D.Double(r.x, r.y + meio, r.width, braco, 3, 3)));
        Color[] cor = rampa(PECA_ESCURA, Comando.DPAD, acesa, semAcao);
        esculpir(px, mascara(cruz), cor, 2, false, null);
        int cx = r.x + r.width / 2, cy = r.y + r.height / 2;
        px[cy * LARGURA + cx] = cor[0].getRGB();
    }

    /** Um botão de face: domo na cor dele, com a letra clara por cima. */
    private static void face(int[] px, Comando c, Color cor, String[] letra, Comando acesa, Set<Comando> semAcao) {
        Rectangle r = PECAS.get(c).area();
        poco(px, new Ellipse2D.Double(r.x - 1, r.y - 1, r.width + 2, r.height + 2));
        Color base = c == acesa ? misturar(cor, Palette.TEXT, 0.35f)
                : semAcao.contains(c) ? misturar(cor, Palette.OUTLINE, 0.6f) : cor;
        esculpir(px, mascara(new Ellipse2D.Double(r.x, r.y, r.width, r.height)), rampaDe(base), 3, false, null);
        int tinta = misturar(base, Palette.TEXT, semAcao.contains(c) ? 0.25f : 0.75f).getRGB();
        int x0 = r.x + (r.width - 3) / 2, y0 = r.y + (r.height - 5) / 2;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 3; x++) {
                if (letra[y].charAt(x) == '#') {
                    px[(y0 + y) * LARGURA + x0 + x] = tinta;
                }
            }
        }
    }

    private static final String[] LETRA_A = {".#.", "#.#", "###", "#.#", "#.#"};
    private static final String[] LETRA_B = {"##.", "#.#", "##.", "#.#", "##."};
    private static final String[] LETRA_X = {"#.#", "#.#", ".#.", "#.#", "#.#"};
    private static final String[] LETRA_Y = {"#.#", "#.#", ".#.", ".#.", ".#."};

    /** Um poço na carcaça: o buraco escuro em volta de uma peça que sai dela. */
    private static void poco(int[] px, Shape forma) {
        esculpir(px, mascara(forma), new Color[] {Palette.OUTLINE, Palette.OUTLINE,
                misturar(Palette.DEEP, Palette.OUTLINE, 0.5f), Palette.DEEP, Palette.DEEP}, 2, true, null);
    }

    // ------------------------------------------------------------------ rampas

    /** A rampa do corpo: do contorno à luz, passando pelos tons da paleta. */
    private static Color[] rampaDoCorpo() {
        return new Color[] {Palette.OUTLINE, Palette.DEEP, misturar(Palette.DEEP, Palette.MAIN, 0.55f),
                Palette.MAIN, misturar(Palette.MAIN, Palette.LIGHT, 0.6f)};
    }

    /** A rampa de uma peça: a base dela, ou a acesa (clara), ou a esmaecida (sem ação). */
    private static Color[] rampa(Color base, Comando c, Comando acesa, Set<Comando> semAcao) {
        if (c == acesa) {
            return rampaDe(misturar(Palette.LIGHT, Palette.ACCENT, 0.6f));
        }
        if (semAcao.contains(c)) {
            return rampaDe(misturar(base, Palette.OUTLINE, 0.55f));
        }
        return rampaDe(base);
    }

    /** Cinco tons em volta de uma cor: dois para a sombra, ela mesma e dois para a luz. */
    private static Color[] rampaDe(Color base) {
        return new Color[] {misturar(base, Palette.OUTLINE, 0.7f), misturar(base, Palette.OUTLINE, 0.35f), base,
                misturar(base, Palette.TEXT, 0.18f), misturar(base, Palette.TEXT, 0.38f)};
    }

    // ------------------------------------------------------------------ escultura

    /**
     * Pinta uma forma com volume: a distância de cada pixel até a borda (até
     * {@code raio}) vira uma altura arredondada, o declive dela vira a direção da
     * superfície, e o ângulo com a luz escolhe o tom da rampa. Contorno de um
     * pixel por fora. Com {@code afundada}, a forma é um buraco: a luz bate na
     * parede de baixo.
     */
    private static void esculpir(int[] px, boolean[][] dentro, Color[] rampa, int raio, boolean afundada,
                                 boolean[][] so) {
        double[][] h = alturas(dentro, raio);
        int contorno = Palette.OUTLINE.getRGB();
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                if (so != null && !so[y][x]) {
                    continue;       // só repinta onde pediram: o painel em cima do corpo, a peça acesa
                }
                if (!dentro[y][x]) {
                    if (!afundada && vizinho(dentro, x, y)) {
                        px[y * LARGURA + x] = contorno;
                    }
                    continue;
                }
                double dx = altura(h, x + 1, y) - altura(h, x - 1, y);
                double dy = altura(h, x, y + 1) - altura(h, x, y - 1);
                if (afundada) {
                    dx = -dx;
                    dy = -dy;
                }
                double[] n = normalizar(-dx * 1.6, -dy * 1.6, 1);
                double luz = n[0] * LUZ[0] + n[1] * LUZ[1] + n[2] * LUZ[2];
                // Tampo plano = o tom do meio; quem encara a luz sobe, quem dá as costas desce.
                double base = LUZ[2];
                int i;
                if (luz > base + 0.22) {
                    i = 4;
                } else if (luz > base + 0.06) {
                    i = 3;
                } else if (luz > base - 0.12) {
                    i = 2;
                } else if (luz > base - 0.38) {
                    i = 1;
                } else {
                    i = 0;
                }
                px[y * LARGURA + x] = rampa[Math.min(rampa.length - 1, i)].getRGB();
            }
        }
    }

    /** A altura de cada pixel: a distância até fora, arredondada como um travesseiro de borda curva. */
    private static double[][] alturas(boolean[][] dentro, int raio) {
        int[][] d = new int[ALTURA][LARGURA];
        int infinito = 1 << 20;
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                d[y][x] = dentro[y][x] ? infinito : 0;
            }
        }
        // Distância chanfrada 3-4, em duas passadas.
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                if (d[y][x] == 0) {
                    continue;
                }
                int m = d[y][x];
                m = Math.min(m, dist(d, x - 1, y) + 3);
                m = Math.min(m, dist(d, x, y - 1) + 3);
                m = Math.min(m, dist(d, x - 1, y - 1) + 4);
                m = Math.min(m, dist(d, x + 1, y - 1) + 4);
                d[y][x] = m;
            }
        }
        for (int y = ALTURA - 1; y >= 0; y--) {
            for (int x = LARGURA - 1; x >= 0; x--) {
                if (d[y][x] == 0) {
                    continue;
                }
                int m = d[y][x];
                m = Math.min(m, dist(d, x + 1, y) + 3);
                m = Math.min(m, dist(d, x, y + 1) + 3);
                m = Math.min(m, dist(d, x + 1, y + 1) + 4);
                m = Math.min(m, dist(d, x - 1, y + 1) + 4);
                d[y][x] = m;
            }
        }
        double[][] h = new double[ALTURA][LARGURA];
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                if (d[y][x] > 0) {
                    double t = Math.min(1.0, d[y][x] / 3.0 / raio);
                    h[y][x] = Math.sqrt(1 - (1 - t) * (1 - t)) * raio;
                }
            }
        }
        // A DISTÂNCIA CHANFRADA ANDA EM DEGRAUS, e nas junções côncavas (corpo com
        // empunhadura) os degraus viravam um serrilhado escuro. Duas passadas de
        // média 3x3 dentro da forma alisam o declive sem mexer na silhueta.
        for (int passada = 0; passada < 2; passada++) {
            double[][] liso = new double[ALTURA][LARGURA];
            for (int y = 0; y < ALTURA; y++) {
                for (int x = 0; x < LARGURA; x++) {
                    if (!dentro[y][x]) {
                        continue;
                    }
                    double soma = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            soma += altura(h, x + dx, y + dy);
                        }
                    }
                    liso[y][x] = soma / 9;
                }
            }
            h = liso;
        }
        return h;
    }

    private static int dist(int[][] d, int x, int y) {
        return x < 0 || y < 0 || x >= LARGURA || y >= ALTURA ? 0 : d[y][x];
    }

    private static double altura(double[][] h, int x, int y) {
        return x < 0 || y < 0 || x >= LARGURA || y >= ALTURA ? 0 : h[y][x];
    }

    /** A sombra projetada: a silhueta, um pouco abaixo, no tom mais fundo da paleta. */
    private static void sombra(BufferedImage img, int[] px) {
        int cor = new Color(Palette.DARKEST.getRed(), Palette.DARKEST.getGreen(), Palette.DARKEST.getBlue(),
                SOMBRA_ALFA).getRGB();
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                int origem = y - SOMBRA_DESCE;
                if (origem >= 0 && (px[origem * LARGURA + x] >>> 24) != 0) {
                    img.setRGB(x, y, cor);
                }
            }
        }
    }

    /** O desenho por cima da sombra. */
    private static int[] mesclar(int[] fundo, int[] frente) {
        int[] saida = fundo.clone();
        for (int i = 0; i < frente.length; i++) {
            if ((frente[i] >>> 24) != 0) {
                saida[i] = frente[i];
            }
        }
        return saida;
    }

    private static boolean[][] mascara(Shape forma) {
        BufferedImage m = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = m.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fill(forma);
        g.dispose();
        boolean[][] dentro = new boolean[ALTURA][LARGURA];
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                dentro[y][x] = (m.getRGB(x, y) >>> 24) != 0;
            }
        }
        return dentro;
    }

    private static boolean vizinho(boolean[][] dentro, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < LARGURA && ny < ALTURA && dentro[ny][nx]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double[] normalizar(double x, double y, double z) {
        double n = Math.sqrt(x * x + y * y + z * z);
        return new double[] {x / n, y / n, z / n};
    }

    private static Color misturar(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
