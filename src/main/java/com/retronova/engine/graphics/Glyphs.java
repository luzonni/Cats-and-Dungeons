package com.retronova.engine.graphics;

import com.retronova.engine.inputs.Acao;
import com.retronova.engine.inputs.gamepad.Comando;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Os desenhos de tecla, mouse e botão de controle: a tela de Controles e o rodapé.
 *
 * DE ONDE VÊM: "Input Prompts Pixel 16×", da Kenney (CC0), a folha empacotada
 * inteira em {@code controles/kenney_input_prompts.png}, com a licença ao lado.
 * Escolhida entre os pacotes livres por ter, num estilo só e em 16 px, teclado,
 * mouse e os botões do Xbox nas cores de sempre (A verde, B vermelho, X azul,
 * Y amarelo) — sem o logotipo da Microsoft. Ver docs/TERCEIROS.md.
 *
 * O ACABAMENTO É DAQUI, não da folha. Cada tecla vem com uma auréola escura de
 * dois pixels, feita para fundo claro; sobre os painéis do jogo ela engordava a
 * tecla e a encostava na vizinha. Na carga a auréola vira um contorno de um pixel
 * na cor de contorno da paleta (o mesmo dos sprites e do texto), e o desenho é
 * recortado no que sobra: uma tecla fica com 11x12, a altura do rodapé.
 *
 * Tudo em escala inteira e sem suavização: a cópia ampliada de cada escala é
 * feita uma vez e guardada ({@link #imagem}).
 */
public final class Glyphs {

    /** Colunas da folha empacotada e o lado de cada tile, em pixels. */
    private static final int COLUNAS = 34, TILE = 16;
    /** A auréola da folha, que vira contorno de um pixel. */
    private static final int AUREOLA = 0x434A5F;

    /** Os desenhos usados, pelo índice na folha (linha * 34 + coluna). */
    public enum Glyph {
        ESC(17), F3(20), F4(21), F5(22), F6(23), F7(24), F11(28),
        N1(51), N2(52), N3(53),
        Q(85), W(86), E(87), A(120), S(121), D(122), F(123),
        ESPACO(153), CIMA(166), DIREITA(167), BAIXO(168), ESQUERDA(169),
        ENTER(134, 135), SHIFT(255, 256),
        MOUSE_ESQ(77), MOUSE_DIR(78), MOUSE_RODA(81),
        PAD_A(4), PAD_B(5), PAD_X(6), PAD_Y(7),
        PAD_LB(553), PAD_RB(554), PAD_LT(551), PAD_RT(552),
        PAD_ANALOGICO_ESQ(416), PAD_ANALOGICO_DIR(484), PAD_L3(424), PAD_R3(492),
        PAD_DPAD(34), PAD_VIEW(616), PAD_MENU(617);

        final int[] tiles;

        Glyph(int... tiles) {
            this.tiles = tiles;
        }
    }

    private static BufferedImage folha;
    private static final Map<Glyph, BufferedImage> TRATADOS = new EnumMap<>(Glyph.class);
    private static final Map<Glyph, BufferedImage[]> AMPLIADOS = new EnumMap<>(Glyph.class);

    private Glyphs() {
    }

    // ------------------------------------------------------------- consulta

    /** O desenho de uma tecla física (KeyEvent.VK_*), ou nulo se a folha não tem. */
    public static Glyph daTecla(int codigo) {
        return switch (codigo) {
            case KeyEvent.VK_ESCAPE -> Glyph.ESC;
            case KeyEvent.VK_F3 -> Glyph.F3;
            case KeyEvent.VK_F4 -> Glyph.F4;
            case KeyEvent.VK_F5 -> Glyph.F5;
            case KeyEvent.VK_F6 -> Glyph.F6;
            case KeyEvent.VK_F7 -> Glyph.F7;
            case KeyEvent.VK_F11 -> Glyph.F11;
            case KeyEvent.VK_1 -> Glyph.N1;
            case KeyEvent.VK_2 -> Glyph.N2;
            case KeyEvent.VK_3 -> Glyph.N3;
            case KeyEvent.VK_Q -> Glyph.Q;
            case KeyEvent.VK_W -> Glyph.W;
            case KeyEvent.VK_E -> Glyph.E;
            case KeyEvent.VK_A -> Glyph.A;
            case KeyEvent.VK_S -> Glyph.S;
            case KeyEvent.VK_D -> Glyph.D;
            case KeyEvent.VK_F -> Glyph.F;
            case KeyEvent.VK_SPACE -> Glyph.ESPACO;
            case KeyEvent.VK_UP -> Glyph.CIMA;
            case KeyEvent.VK_DOWN -> Glyph.BAIXO;
            case KeyEvent.VK_LEFT -> Glyph.ESQUERDA;
            case KeyEvent.VK_RIGHT -> Glyph.DIREITA;
            case KeyEvent.VK_ENTER -> Glyph.ENTER;
            case KeyEvent.VK_SHIFT -> Glyph.SHIFT;
            default -> null;
        };
    }

    public static Glyph doMouse(Acao.Mouse m) {
        return switch (m) {
            case ESQUERDO -> Glyph.MOUSE_ESQ;
            case DIREITO -> Glyph.MOUSE_DIR;
            case RODA -> Glyph.MOUSE_RODA;
        };
    }

    public static Glyph doComando(Comando c) {
        return switch (c) {
            case LT -> Glyph.PAD_LT;
            case LB -> Glyph.PAD_LB;
            case RT -> Glyph.PAD_RT;
            case RB -> Glyph.PAD_RB;
            case ANALOGICO_ESQ -> Glyph.PAD_ANALOGICO_ESQ;
            case ANALOGICO_DIR -> Glyph.PAD_ANALOGICO_DIR;
            case L3 -> Glyph.PAD_L3;
            case R3 -> Glyph.PAD_R3;
            case DPAD -> Glyph.PAD_DPAD;
            case VIEW -> Glyph.PAD_VIEW;
            case MENU -> Glyph.PAD_MENU;
            case A -> Glyph.PAD_A;
            case B -> Glyph.PAD_B;
            case X -> Glyph.PAD_X;
            case Y -> Glyph.PAD_Y;
        };
    }

    /** Largura do desenho, em pixels nativos. */
    public static int largura(Glyph glyph) {
        return tratado(glyph).getWidth();
    }

    /** Altura do desenho, em pixels nativos. */
    public static int altura(Glyph glyph) {
        return tratado(glyph).getHeight();
    }

    // ------------------------------------------------------------- desenho

    /** Desenha com o canto de cima à esquerda em (x, y), na escala {@code s}. */
    public static void desenhar(Graphics2D g, Glyph glyph, int x, int y, int s) {
        g.drawImage(imagem(glyph, s), x, y, null);
    }

    /** O desenho ampliado por {@code s}, sem suavização, guardado para os próximos quadros. */
    public static synchronized BufferedImage imagem(Glyph glyph, int s) {
        int escala = Math.max(1, s);
        BufferedImage[] porEscala = AMPLIADOS.computeIfAbsent(glyph, k -> new BufferedImage[17]);
        if (escala >= porEscala.length) {
            return Acelerada.de(ampliar(tratado(glyph), escala));
        }
        if (porEscala[escala] == null) {
            porEscala[escala] = Acelerada.de(ampliar(tratado(glyph), escala));
        }
        return porEscala[escala];
    }

    private static BufferedImage ampliar(BufferedImage img, int s) {
        BufferedImage o = new BufferedImage(img.getWidth() * s, img.getHeight() * s, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) == 0) {
                    continue;
                }
                for (int dy = 0; dy < s; dy++) {
                    for (int dx = 0; dx < s; dx++) {
                        o.setRGB(x * s + dx, y * s + dy, p);
                    }
                }
            }
        }
        return o;
    }

    // ------------------------------------------------------------- tratamento

    private static synchronized BufferedImage tratado(Glyph glyph) {
        return TRATADOS.computeIfAbsent(glyph, Glyphs::tratar);
    }

    private static BufferedImage tratar(Glyph glyph) {
        if (folha == null) {
            folha = SpriteHandler.getSprite("controles", "kenney_input_prompts", 1);
        }
        int w = glyph.tiles.length * TILE;
        BufferedImage bruto = new BufferedImage(w, TILE, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < glyph.tiles.length; i++) {
            int t = glyph.tiles[i];
            int ox = (t % COLUNAS) * TILE, oy = (t / COLUNAS) * TILE;
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    if (ox + x < folha.getWidth() && oy + y < folha.getHeight()) {
                        bruto.setRGB(i * TILE + x, y, folha.getRGB(ox + x, oy + y));
                    }
                }
            }
        }
        return recortar(afinarAureola(bruto, Palette.OUTLINE.getRGB()));
    }

    /**
     * A auréola de dois pixels vira um contorno de um: fica só o pixel dela que
     * encosta (inclusive na diagonal) no desenho de verdade, já na cor pedida.
     */
    static BufferedImage afinarAureola(BufferedImage f, int contorno) {
        int w = f.getWidth(), h = f.getHeight();
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = f.getRGB(x, y);
                if ((p >>> 24) == 0) {
                    continue;
                }
                if ((p & 0xFFFFFF) != AUREOLA) {
                    o.setRGB(x, y, p);
                } else if (encostaNoDesenho(f, x, y)) {
                    o.setRGB(x, y, contorno);
                }
            }
        }
        return o;
    }

    private static boolean encostaNoDesenho(BufferedImage f, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= f.getWidth() || ny >= f.getHeight()) {
                    continue;
                }
                int p = f.getRGB(nx, ny);
                if ((p >>> 24) != 0 && (p & 0xFFFFFF) != AUREOLA) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Corta as bordas transparentes. */
    static BufferedImage recortar(BufferedImage f) {
        int x0 = f.getWidth(), y0 = f.getHeight(), x1 = -1, y1 = -1;
        for (int y = 0; y < f.getHeight(); y++) {
            for (int x = 0; x < f.getWidth(); x++) {
                if ((f.getRGB(x, y) >>> 24) != 0) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        if (x1 < 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage o = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(f, -x0, -y0, null);
        g.dispose();
        return o;
    }
}
