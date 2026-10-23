package com.retronova.game.items;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A corda de um arco, separada do desenho para poder ser PUXADA.
 *
 * Os arcos dos pacotes trazem a corda pintada: uma linha reta de um pixel, de
 * uma cor so, na diagonal entre as duas pontas. Pintada, ela nao estica — e um
 * arco cuja corda nao se mexe nao puxa, so aponta. Aqui a corda e achada no
 * PNG, apagada de uma copia do sprite, e o arco a desenha de novo em tempo de
 * jogo, como dois segmentos das pontas ate o encaixe da flecha.
 *
 * ACHADA, E NAO MARCADA A MAO. As sete variantes de arco tem a corda em lugares
 * diferentes; a regra que serve para todas e "a maior sequencia de pixels da
 * mesma cor numa anti-diagonal" (x + y constante), porque e assim que os
 * pacotes desenham arco na diagonal. Arco em que isso nao aparece (nenhuma
 * sequencia de {@link #MINIMO} pixels) fica sem corda solta e e desenhado como
 * sempre foi.
 *
 * Tudo e feito uma vez por sprite e guardado: nada disso roda por quadro.
 */
final class CordaDoArco {

    /** Menor corda aceita, em pixels de arte. Menos que isso e enfeite, nao corda. */
    static final int MINIMO = 7;

    /** O sprite sem a corda, na mesma escala do original. */
    final BufferedImage semCorda;
    /** As pontas da corda, em pixels do sprite escalado (centro do pixel). */
    final double ax, ay, bx, by;
    final Color cor;

    private CordaDoArco(BufferedImage semCorda, double ax, double ay, double bx, double by, Color cor) {
        this.semCorda = semCorda;
        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
        this.cor = cor;
    }

    private static final Map<BufferedImage, CordaDoArco> PRONTAS =
            Collections.synchronizedMap(new WeakHashMap<>());
    /** Marca "este sprite nao tem corda achavel", para nao procurar de novo. */
    private static final CordaDoArco NENHUMA = new CordaDoArco(null, 0, 0, 0, 0, null);

    /** A corda do sprite (ja escalado por {@code px}), ou null se nao der para achar. */
    static CordaDoArco de(BufferedImage sprite, int px) {
        if (sprite == null) {
            return null;
        }
        CordaDoArco pronta = PRONTAS.get(sprite);
        if (pronta == null) {
            pronta = montar(sprite, px);
            PRONTAS.put(sprite, pronta);
        }
        return pronta == NENHUMA ? null : pronta;
    }

    private static CordaDoArco montar(BufferedImage sprite, int px) {
        int w = sprite.getWidth() / px, h = sprite.getHeight() / px;
        int[][] arte = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                arte[y][x] = sprite.getRGB(x * px + px / 2, y * px + px / 2);
            }
        }
        int[] c = acharCorda(arte);
        if (c == null) {
            return NENHUMA;
        }
        BufferedImage copia = new BufferedImage(sprite.getWidth(), sprite.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        copia.getGraphics().drawImage(sprite, 0, 0, null);
        // A corda vai de (x0, y0) descendo para a esquerda: x diminui, y aumenta.
        for (int i = 0; i < c[2]; i++) {
            int x = c[0] - i, y = c[1] + i;
            for (int yy = 0; yy < px; yy++) {
                for (int xx = 0; xx < px; xx++) {
                    copia.setRGB(x * px + xx, y * px + yy, 0);
                }
            }
        }
        double meio = px / 2d;
        int fim = c[2] - 1;
        return new CordaDoArco(com.retronova.engine.graphics.Acelerada.de(copia),
                c[0] * px + meio, c[1] * px + meio,
                (c[0] - fim) * px + meio, (c[1] + fim) * px + meio,
                new Color(arte[c[1]][c[0]] | 0xFF000000, true));
    }

    /**
     * A corda na grade de arte: {x do comeco, y do comeco, comprimento}, com o
     * comeco no pixel de cima e a corda descendo para a esquerda. Null se nao
     * houver sequencia de {@link #MINIMO} pixels opacos da mesma cor numa
     * anti-diagonal.
     */
    static int[] acharCorda(int[][] arte) {
        int h = arte.length, w = h == 0 ? 0 : arte[0].length;
        int[] melhor = null;
        for (int soma = 0; soma <= w + h - 2; soma++) {
            int corrida = 0, cor = 0, x0 = 0, y0 = 0;
            for (int y = Math.max(0, soma - (w - 1)); y <= Math.min(h - 1, soma); y++) {
                int x = soma - y;
                int p = arte[y][x];
                boolean opaco = (p >>> 24) > 16;
                if (opaco && corrida > 0 && (p & 0xFFFFFF) == cor) {
                    corrida++;
                } else if (opaco) {
                    corrida = 1;
                    cor = p & 0xFFFFFF;
                    x0 = x;
                    y0 = y;
                } else {
                    corrida = 0;
                }
                if (corrida >= MINIMO && (melhor == null || corrida > melhor[2])) {
                    melhor = new int[]{x0, y0, corrida};
                }
            }
        }
        return melhor;
    }
}
