package com.retronova.game.items;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A corda achada nos arcos.
 *
 * O arco tira a corda pintada do PNG e a desenha de novo, puxada. Se a busca
 * deixar de achar a corda num arco, ele volta ao desenho parado sem erro
 * nenhum — so perde a animacao. Por isso todo arco do jogo e conferido aqui.
 */
class CordaDoArcoTest {

    private static final String[] ARCOS = {"bow", "bow_fire", "bow_ice", "bow_water",
            "bow_earth", "bow_air", "bow_legend"};

    @Test
    @DisplayName("A corda e a maior sequencia de uma cor numa anti-diagonal")
    void sintetica() {
        int[][] arte = new int[16][16];
        for (int i = 0; i < 9; i++) {
            arte[3 + i][12 - i] = 0xFFFFFFFF;
        }
        arte[5][5] = 0xFF222222;
        assertArrayEquals(new int[]{12, 3, 9}, CordaDoArco.acharCorda(arte));
    }

    @Test
    @DisplayName("Sem sequencia longa, nao ha corda")
    void semCorda() {
        int[][] arte = new int[16][16];
        for (int i = 0; i < CordaDoArco.MINIMO - 1; i++) {
            arte[i][10 - i] = 0xFFFFFFFF;
        }
        assertNull(CordaDoArco.acharCorda(arte));
    }

    @Test
    @DisplayName("A corda solta puxada, passa do repouso e assenta")
    void cordaVoltando() {
        assertEquals(1.0, Bow.cordaVoltando(0), 1e-9);
        assertEquals(0.0, Bow.cordaVoltando(1), 1e-9);
        boolean passou = false;
        for (double k = 0; k < 1; k += 0.01) {
            passou |= Bow.cordaVoltando(k) < 0;
        }
        assertTrue(passou, "a corda tem de passar do ponto de repouso para a frente");
        assertEquals(1.0, Bow.coice(0), 1e-9);
        assertEquals(0.0, Bow.coice(1), 1e-9);
    }

    @Test
    @DisplayName("Todo arco do jogo tem corda achavel")
    void todosOsArcos() throws Exception {
        for (String nome : ARCOS) {
            try (InputStream in = getClass().getResourceAsStream(
                    "/com/retronova/resources/sprites/items/" + nome + ".png")) {
                assertNotNull(in, nome);
                BufferedImage im = ImageIO.read(in);
                int[][] arte = new int[16][16];
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        arte[y][x] = im.getRGB(x, y);
                    }
                }
                int[] corda = CordaDoArco.acharCorda(arte);
                assertNotNull(corda, nome + " sem corda achavel");
                assertTrue(corda[2] >= CordaDoArco.MINIMO, nome);
            }
        }
    }
}
