package com.retronova.engine.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Texto de interface com acabamento de pixel art.
 *
 * Antes cada tela desenhava a própria sombra de um passo só, com o preenchimento
 * chapado. Sobre os painéis cinza-azulados o branco ficava estourado e a letra
 * parecia colada por cima da arte, sem o contorno que os sprites têm.
 *
 * O acabamento aqui imita o dos sprites: contorno fechado de um pixel da fonte,
 * sombra projetada para baixo e o preenchimento em duas faixas — a de cima na
 * cor pedida e a de baixo puxada para a sombra, como a luz da lua vinda de cima.
 * As faixas são um corte seco e não um degradê, para não borrar o pixel.
 */
public final class TextFx {

    /** Quanto a faixa de baixo puxa para {@link Palette#DEEP}. */
    private static final float SOMBREADO = 0.30f;

    /** O branco puro estoura sobre os painéis; o texto claro usa este. */
    public static final Color CLARO = new Color(0xE6E8F0);

    private TextFx() { }

    /**
     * @param y linha de base, como em {@code drawString}
     * @param s tamanho de um pixel da fonte na tela (a escala da UI)
     */
    public static void draw(Graphics2D g, String texto, int x, int y, Color cor, int s) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        int p = Math.max(1, s);
        Color original = g.getColor();

        // Sombra projetada: o contorno repetido um pixel abaixo.
        g.setColor(comAlfa(Palette.DARKEST, cor.getAlpha()));
        for (int dx = -p; dx <= p; dx += p) {
            g.drawString(texto, x + dx, y + 2 * p);
        }

        g.setColor(comAlfa(Palette.OUTLINE, cor.getAlpha()));
        for (int dx = -p; dx <= p; dx += p) {
            for (int dy = -p; dy <= p; dy += p) {
                if (dx != 0 || dy != 0) {
                    g.drawString(texto, x + dx, y + dy);
                }
            }
        }

        Color topo = cor.equals(Palette.TEXT) ? CLARO : cor;
        g.setColor(topo);
        g.drawString(texto, x, y);

        // Faixa de baixo: a metade inferior do corpo da letra, mais escura.
        FontMetrics fm = g.getFontMetrics();
        int ascent = fm.getAscent();
        int corte = y - ascent / 3;
        Shape clip = g.getClip();
        g.clipRect(x - p, corte, fm.stringWidth(texto) + 2 * p, fm.getDescent() + ascent);
        g.setColor(misturar(topo, Palette.DEEP, SOMBREADO));
        g.drawString(texto, x, y);
        g.setClip(clip);

        g.setColor(original);
    }

    public static void draw(Graphics2D g, String texto, Font fonte, int x, int y, Color cor, int s) {
        g.setFont(fonte);
        draw(g, texto, x, y, cor, s);
    }

    private static Color comAlfa(Color c, int alfa) {
        return alfa >= 255 ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }

    private static Color misturar(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                a.getAlpha());
    }
}
