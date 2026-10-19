package com.retronova.engine.graphics;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Copias de sprite que a placa de video consegue guardar.
 *
 * O Sheeter recorta cada quadro da folha com getSubimage. Uma imagem recortada
 * divide a memoria com a folha inteira, e o Java2D NUNCA a leva para a GPU:
 * todo drawImage dela e feito pixel a pixel na CPU, quadro apos quadro. Medido
 * na arena: so o chao custava 6,8 ms por quadro, a maior parte disso aqui.
 *
 * A copia "compativel" (no formato da tela, memoria propria) e levada para a
 * GPU pelo proprio Java depois dos primeiros desenhos, e dali em diante custa
 * quase nada. Uma por quadro de folha, guardada com chave fraca.
 */
public final class Acelerada {

    private Acelerada() {
    }

    private static final Map<BufferedImage, BufferedImage> COPIAS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static GraphicsConfiguration gc;

    public static BufferedImage de(BufferedImage origem) {
        if (origem == null) {
            return null;
        }
        return COPIAS.computeIfAbsent(origem, Acelerada::copiar);
    }

    private static BufferedImage copiar(BufferedImage origem) {
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
        }
        BufferedImage copia = gc.createCompatibleImage(origem.getWidth(), origem.getHeight(),
                Transparency.TRANSLUCENT);
        Graphics2D g = copia.createGraphics();
        g.drawImage(origem, 0, 0, null);
        g.dispose();
        return copia;
    }
}
