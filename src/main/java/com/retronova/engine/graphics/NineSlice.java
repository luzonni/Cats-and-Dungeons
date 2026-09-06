package com.retronova.engine.graphics;

import com.retronova.engine.Engine;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Desenha uma moldura 9-slice: os quatro cantos são preservados em escala
 * inteira e as cinco regiões restantes são esticadas. É o que permite um mesmo
 * sprite servir a botões de qualquer largura sem deformar a arte.
 *
 * A folha guarda os estados lado a lado no eixo X, cada um com {@code cellW} de
 * largura.
 */
public class NineSlice {

    private final BufferedImage sheet;
    private final int cellW, cellH;
    private final int left, right, top, bottom;
    private final int states;

    /**
     * @param resource nome do arquivo em resources/ui, sem extensão
     * @param cellW    largura de um estado, em pixels nativos
     * @param left     recortes 9-slice, em pixels nativos
     */
    public NineSlice(String resource, int cellW, int left, int right, int top, int bottom) {
        BufferedImage img;
        try {
            String path = Engine.resPath + "ui/" + resource + ".png";
            img = ImageIO.read(Objects.requireNonNull(NineSlice.class.getResource(path)));
        } catch (Exception e) {
            System.err.println("NineSlice: falha ao carregar '" + resource + "': " + e);
            img = new BufferedImage(cellW, cellW, BufferedImage.TYPE_INT_ARGB);
        }
        this.sheet = img;
        this.cellW = cellW;
        this.cellH = img.getHeight();
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
        this.states = Math.max(1, img.getWidth() / cellW);
    }

    public int getCellHeight() {
        return this.cellH;
    }

    /** Menor altura que preserva os cantos, em pixels nativos. */
    public int getMinHeight() {
        return this.top + this.bottom;
    }

    /** Menor largura que preserva os cantos, em pixels nativos. */
    public int getMinWidth() {
        return this.left + this.right;
    }

    /**
     * @param state índice do estado; valores fora da folha são fixados no limite
     * @param scale fator inteiro; usar valor inteiro evita borrar a pixel art
     */
    public void draw(Graphics2D g2, int state, int x, int y, int width, int height, int scale) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int st = Math.max(0, Math.min(state, states - 1));
        int sx = st * cellW;

        int l = left * scale;
        int r = right * scale;
        int t = top * scale;
        int b = bottom * scale;

        // Em tamanhos pequenos os cantos não cabem inteiros; encolhe-os na proporção
        // em vez de deixá-los invadir o lado oposto.
        if (l + r > width) {
            double f = width / (double) (l + r);
            l = (int) (l * f);
            r = width - l;
        }
        if (t + b > height) {
            double f = height / (double) (t + b);
            t = (int) (t * f);
            b = height - t;
        }

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        int srcCw = cellW - left - right;      // faixa central da folha
        int srcCh = cellH - top - bottom;
        int dstCw = width - l - r;             // faixa central no destino
        int dstCh = height - t - b;

        // cantos
        blit(g, sx, 0, left, top, x, y, l, t);
        blit(g, sx + cellW - right, 0, right, top, x + width - r, y, r, t);
        blit(g, sx, cellH - bottom, left, bottom, x, y + height - b, l, b);
        blit(g, sx + cellW - right, cellH - bottom, right, bottom, x + width - r, y + height - b, r, b);

        // bordas horizontais
        if (dstCw > 0) {
            blit(g, sx + left, 0, srcCw, top, x + l, y, dstCw, t);
            blit(g, sx + left, cellH - bottom, srcCw, bottom, x + l, y + height - b, dstCw, b);
        }
        // bordas verticais
        if (dstCh > 0) {
            blit(g, sx, top, left, srcCh, x, y + t, l, dstCh);
            blit(g, sx + cellW - right, top, right, srcCh, x + width - r, y + t, r, dstCh);
        }
        // miolo
        if (dstCw > 0 && dstCh > 0) {
            blit(g, sx + left, top, srcCw, srcCh, x + l, y + t, dstCw, dstCh);
        }
        g.dispose();
    }

    private void blit(Graphics2D g, int sx, int sy, int sw, int sh,
                      int dx, int dy, int dw, int dh) {
        if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) {
            return;
        }
        g.drawImage(sheet, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
    }
}
