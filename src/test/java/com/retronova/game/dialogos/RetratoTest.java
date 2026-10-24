package com.retronova.game.dialogos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os retratos: toda emoção tem cara própria, e a que falta vira a neutra.
 */
class RetratoTest {

    /** Pixels diferentes entre dois quadros. */
    private static int diferenca(BufferedImage a, BufferedImage b) {
        int n = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    @DisplayName("Cada emoção do vendedor é uma cara diferente da neutra, e a piscada e a fala mudam o quadro")
    void vendedor() {
        Retrato r = Retrato.de("vendedor");
        assertNotNull(r, "falta sprites/retratos/vendedor.png");
        BufferedImage neutro = r.cru(Emocao.NEUTRO, Retrato.ABERTO);
        for (Emocao e : Emocao.values()) {
            assertEquals(e.ordinal(), r.coluna(e), e + " sem coluna propria");
            if (e != Emocao.NEUTRO) {
                assertTrue(diferenca(neutro, r.cru(e, Retrato.ABERTO)) >= 6, e + " igual a neutra");
            }
            if (e != Emocao.FELIZ) {   // a feliz ja tem os olhos fechados
                assertTrue(diferenca(r.cru(e, Retrato.ABERTO), r.cru(e, Retrato.PISCANDO)) > 0, e + " nao pisca");
            }
            assertTrue(diferenca(r.cru(e, Retrato.ABERTO), r.cru(e, Retrato.FALANDO)) > 0
                    || e == Emocao.SURPRESO, e + " nao abre a boca");
        }
    }

    @Test
    @DisplayName("Folha só com a neutra: toda emoção cai nela, e a linha que falta também")
    void soNeutra() {
        BufferedImage folha = new BufferedImage(Retrato.LADO, Retrato.LADO, BufferedImage.TYPE_INT_ARGB);
        folha.setRGB(5, 5, 0xFFFF0000);
        Retrato r = Retrato.daFolha(folha);
        for (Emocao e : Emocao.values()) {
            assertEquals(0, r.coluna(e));
        }
        assertEquals(0xFFFF0000, r.cru(Emocao.BRAVO, Retrato.FALANDO).getRGB(5, 5));
    }
}
