package com.retronova.game.map.arena;

import com.retronova.engine.graphics.Familia;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** A folha e a troca de cores precisam preservar as cinco cores da identidade. */
class PortalTest {
    @Test
    void folhaNasFamiliasComCacheETransparencia() throws Exception {
        var folha = ImageIO.read(new File("src/main/resources/com/retronova/resources/sprites/objects/furniture/portalRunforce.png"));
        assertEquals(64, folha.getWidth());
        assertEquals(16, folha.getHeight());
        for (Chegada.Tom tom : Chegada.Tom.values()) {
            Familia familia = tom == Chegada.Tom.PERIGO ? Familia.VIDA : Familia.ACO;
            Set<Integer> cores = new HashSet<>();
            for (int i = 0; i < 5; i++) cores.add(familia.tom(i).getRGB());
            var colorida = tom.tingir(folha);
            assertSame(colorida, tom.tingir(folha));
            for (int y = 0; y < folha.getHeight(); y++) {
                for (int x = 0; x < folha.getWidth(); x++) {
                    int pixel = colorida.getRGB(x, y);
                    assertEquals(folha.getRGB(x, y) >>> 24, pixel >>> 24);
                    if ((pixel >>> 24) != 0) assertTrue(cores.contains(pixel));
                }
            }
        }
    }
}
