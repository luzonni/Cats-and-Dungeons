package com.retronova.game.items;

import com.retronova.engine.graphics.Palette;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O registro de ícones contra os PNG e contra a regra de estilo. */
class IconeTest {

    /** Quantos pixels de silhueta dois ícones da mesma família precisam ter de diferença. */
    private static final int DIFERENCA_MINIMA = 20;

    private static BufferedImage ler(Icone i) throws Exception {
        try (InputStream in = Icone.class.getResourceAsStream(
                "/com/retronova/resources/icones/" + i.name().toLowerCase() + ".png")) {
            assertNotNull(in, "falta o PNG de " + i);
            return ImageIO.read(in);
        }
    }

    @Test
    @DisplayName("todo ícone existe, tem 16x16, sem meio-transparente, com o contorno da paleta e sem preto ou branco puros")
    void estilo() throws Exception {
        int contorno = Palette.OUTLINE.getRGB() & 0xFFFFFF;
        for (Icone i : Icone.values()) {
            BufferedImage img = ler(i);
            assertEquals(16, img.getWidth(), i + " fora de 16x16");
            assertEquals(16, img.getHeight(), i + " fora de 16x16");
            boolean temContorno = false;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int p = img.getRGB(x, y);
                    int a = p >>> 24;
                    assertTrue(a == 0 || a == 255, i + " tem pixel meio transparente (suavização) em " + x + "," + y);
                    if (a == 0) {
                        continue;
                    }
                    int rgb = p & 0xFFFFFF;
                    temContorno |= rgb == contorno;
                    assertTrue(rgb != 0 && rgb != 0xFFFFFF, i + " tem preto ou branco puro, fora da paleta");
                }
            }
            assertTrue(temContorno, i + " sem o contorno da paleta");
        }
    }

    @Test
    @DisplayName("melhorias, passivas, armas, elementos e raridades têm ícone")
    void cobertura() {
        for (Melhoria m : Melhoria.values()) {
            assertNotNull(Icone.de(m), m.name());
        }
        for (Passiva p : Passiva.values()) {
            assertNotNull(Icone.de(p), p.name());
        }
        for (ArmaExtra a : ArmaExtra.values()) {
            assertNotNull(Icone.de(a), a.name());
        }
        for (Raridade r : Raridade.values()) {
            assertNotNull(Icone.de(r), r.name());
        }
        for (Elemento e : Elemento.values()) {
            assertEquals(e.simbolo() == null, Icone.de(e) == null, "o elemento " + e + " e o símbolo dele discordam");
        }
    }

    @Test
    @DisplayName("cada ícone da build se distingue pela forma, sem depender da cor")
    void formasDistintas() throws Exception {
        List<Icone> build = new ArrayList<>();
        for (Melhoria m : Melhoria.values()) {
            build.add(Icone.de(m));
        }
        for (Passiva p : Passiva.values()) {
            build.add(Icone.de(p));
        }
        for (ArmaExtra a : ArmaExtra.values()) {
            build.add(Icone.de(a));
        }
        comparar(build);
        comparar(List.of(Icone.FOGO, Icone.GELO, Icone.AGUA, Icone.TERRA, Icone.AR));
    }

    private static void comparar(List<Icone> grupo) throws Exception {
        List<boolean[]> silhuetas = new ArrayList<>();
        for (Icone i : grupo) {
            BufferedImage img = ler(i);
            boolean[] s = new boolean[256];
            for (int k = 0; k < 256; k++) {
                s[k] = (img.getRGB(k % 16, k / 16) >>> 24) != 0;
            }
            silhuetas.add(s);
        }
        for (int a = 0; a < grupo.size(); a++) {
            for (int b = a + 1; b < grupo.size(); b++) {
                int diferentes = 0;
                for (int k = 0; k < 256; k++) {
                    if (silhuetas.get(a)[k] != silhuetas.get(b)[k]) {
                        diferentes++;
                    }
                }
                assertTrue(diferentes >= DIFERENCA_MINIMA,
                        grupo.get(a) + " e " + grupo.get(b) + " têm quase a mesma silhueta (" + diferentes + " pixels)");
            }
        }
    }
}
