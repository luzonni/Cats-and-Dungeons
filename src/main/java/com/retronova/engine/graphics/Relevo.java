package com.retronova.engine.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Acabamento de pixel art para ícones pequenos: contorno, relevo e sombra.
 *
 * Os selos de elemento e de raridade são PNGs de 11x11 desenhados chapados e
 * esticados para o tamanho da carta. Esticar por um fator quebrado (11 para 30
 * é 2,7x) com suavização era o que os deixava borrados, e sem contorno eles se
 * perdiam no painel. Aqui o ícone é tratado NA RESOLUÇÃO NATIVA, pixel a pixel,
 * e só depois ampliado — por fator inteiro e sem suavização.
 *
 * O tratamento, de baixo para cima:
 *  - sombra projetada um pixel abaixo e à direita;
 *  - contorno escuro de um pixel em volta da silhueta;
 *  - relevo: o pixel com vazio acima clareia, o com vazio abaixo escurece.
 *    A luz vem de cima, como no resto da interface.
 */
public final class Relevo {

    private static final Map<BufferedImage, BufferedImage> CACHE = new IdentityHashMap<>();

    private Relevo() {
    }

    /** O ícone tratado, com 2 pixels de folga de cada lado (para o contorno e a sombra). */
    public static synchronized BufferedImage de(BufferedImage fonte) {
        return CACHE.computeIfAbsent(fonte, Relevo::tratar);
    }

    /**
     * Desenha o ícone tratado centrado em (cx, cy), no maior fator inteiro que
     * caiba em {@code lado} pixels.
     */
    public static void desenhar(Graphics2D g, BufferedImage fonte, int cx, int cy, int lado) {
        if (fonte == null) {
            return;
        }
        BufferedImage img = de(fonte);
        int fator = Math.max(1, lado / img.getWidth());
        int w = img.getWidth() * fator, h = img.getHeight() * fator;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(img, cx - w / 2, cy - h / 2, w, h, null);
        g2.dispose();
    }

    private static BufferedImage tratar(BufferedImage f) {
        int w = f.getWidth(), h = f.getHeight();
        int pad = 2;
        BufferedImage o = new BufferedImage(w + 2 * pad, h + 2 * pad, BufferedImage.TYPE_INT_ARGB);
        int sombra = new Color(0, 0, 0, 110).getRGB();
        int contorno = Palette.OUTLINE.getRGB();

        // Sombra: a silhueta deslocada (1, 1).
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (cheio(f, x, y)) {
                    o.setRGB(x + pad + 1, y + pad + 2, sombra);
                }
            }
        }
        // Contorno: os vizinhos (8 direções) de cada pixel cheio.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!cheio(f, x, y)) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        o.setRGB(x + pad + dx, y + pad + dy, contorno);
                    }
                }
            }
        }
        // Preenchimento com relevo.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!cheio(f, x, y)) {
                    continue;
                }
                Color c = new Color(f.getRGB(x, y), true);
                if (!cheio(f, x, y - 1)) {
                    c = misturar(c, Color.WHITE, 0.35f);
                } else if (!cheio(f, x, y + 1) || !cheio(f, x + 1, y)) {
                    c = misturar(c, Color.BLACK, 0.30f);
                }
                o.setRGB(x + pad, y + pad, c.getRGB() | 0xFF000000);
            }
        }
        return o;
    }

    private static boolean cheio(BufferedImage f, int x, int y) {
        return x >= 0 && y >= 0 && x < f.getWidth() && y < f.getHeight() && (f.getRGB(x, y) >>> 24) > 100;
    }

    private static Color misturar(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
