package com.retronova.engine.graphics;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O cache do Alpha.getImage: a mesma imagem por degrau, e a opacidade certa nos pixels. */
class AlphaTest {

    private static BufferedImage opaca() {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                img.setRGB(x, y, 0xFF336699);
            }
        }
        return img;
    }

    @Test
    void mesmaOpacidadeDevolveAMesmaImagem() {
        BufferedImage s = opaca();
        assertSame(Alpha.getImage(s, 0.5f), Alpha.getImage(s, 0.5f), "sem imagem nova a cada quadro");
        assertSame(Alpha.getImage(s, 0.50f), Alpha.getImage(s, 0.51f), "mesmo degrau, mesma imagem");
    }

    @Test
    void opacidadeDiferenteDevolveOutraImagem() {
        BufferedImage s = opaca();
        assertNotSame(Alpha.getImage(s, 0.25f), Alpha.getImage(s, 0.75f));
    }

    @Test
    void opacidadeCheiaDevolveOProprioSprite() {
        BufferedImage s = opaca();
        assertSame(s, Alpha.getImage(s, 1f));
    }

    @Test
    void osPixelsSaemComAOpacidadeDoDegrau() {
        BufferedImage s = opaca();
        int a = Alpha.getImage(s, 0.5f).getRGB(1, 1) >>> 24;
        assertTrue(Math.abs(a - 128) <= 2, "alfa obtido " + a);
        assertEquals(0, Alpha.getImage(s, 0f).getRGB(1, 1) >>> 24);
    }

    @Test
    void degrausFicamNoIntervalo() {
        assertEquals(0, Alpha.degrau(-1f));
        assertEquals(16, Alpha.degrau(2f));
        assertEquals(8, Alpha.degrau(0.5f));
    }
}
