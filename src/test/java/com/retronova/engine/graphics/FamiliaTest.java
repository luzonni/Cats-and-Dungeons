package com.retronova.engine.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** As famílias do jogo contra as cores que o gerador de ícones gravou: as duas não podem divergir. */
class FamiliaTest {

    @Test
    @DisplayName("toda cor dos ícones está numa família do jogo (ou é o contorno)")
    void iconesNasFamilias() throws Exception {
        Set<Integer> conhecidas = new HashSet<>();
        conhecidas.add(Palette.OUTLINE.getRGB() & 0xFFFFFF);
        for (Familia f : Familia.values()) {
            for (int i = 0; i < 5; i++) {
                conhecidas.add(f.tom(i).getRGB() & 0xFFFFFF);
            }
        }
        File[] pngs = new File("src/main/resources/com/retronova/resources/icones").listFiles((d, n) -> n.endsWith(".png"));
        assertTrue(pngs != null && pngs.length > 0, "sem ícones para conferir");
        for (File png : pngs) {
            BufferedImage img = ImageIO.read(png);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int p = img.getRGB(x, y);
                    if ((p >>> 24) != 0) {
                        assertTrue(conhecidas.contains(p & 0xFFFFFF), png.getName() + " tem a cor "
                                + Integer.toHexString(p & 0xFFFFFF) + " fora das famílias, em " + x + "," + y);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("a versão transparente não cria cor nova a cada pedido")
    void transparentesEmCache() {
        assertTrue(Familia.FOGO.tom(Familia.BASE, 100) == Familia.FOGO.tom(Familia.BASE, 101));
    }
}
