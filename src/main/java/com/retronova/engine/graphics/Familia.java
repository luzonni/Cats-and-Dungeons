package com.retronova.engine.graphics;

import java.awt.Color;

/**
 * AS FAMÍLIAS DE COR DO JOGO: as mesmas dos ícones, para o mundo falar a mesma
 * língua do card.
 *
 * Cada família é uma rampa de cinco tons — sombra funda, sombra, base, luz e
 * brilho — gerada da MESMA fórmula de tools/GenIcones.java, a partir de uma cor
 * oficial puxada um quinto para o tom frio da paleta:
 *
 *     âncora = mistura(cor oficial, Palette.MAIN, 0,2)
 *     rampa  = [âncora→OUTLINE 60%, →OUTLINE 30%, âncora, →TEXT 30%, →TEXT 60%]
 *
 * O metal (ACO) é a escada da própria interface. O FamiliaTest confere que toda
 * cor dos PNG de ícone está numa rampa daqui: se o gerador mudar de fórmula e o
 * jogo não, o teste quebra.
 *
 * USO: efeito, número de dano, coletável, barra no mundo — tudo que é desenhado
 * na arena e tem um ícone correspondente pega a cor da família do ícone. Fogo é
 * {@link #FOGO} no selo, na brasa do inimigo e no número do dano de fogo.
 *
 * As versões transparentes ({@link #tom(int, int)}) ficam prontas em degraus de
 * opacidade: nenhuma cor nova por quadro.
 */
public enum Familia {
    ACO(new int[] {0x3E4160, 0x525779, 0x7E849C, 0xABB1C1, 0xFCFCFC}),
    VIDA(0xC23A4E),      // BarraDeChefe.VIDA
    FOGO(0xDF864D),      // Elemento.FOGO
    GELO(0x85C4D4),      // Elemento.GELO
    AGUA(0x79A8D4),      // Elemento.AGUA
    TERRA(0xB07E41),     // Elemento.TERRA
    AR(0xD7FFE4),        // Elemento.AR
    OURO(0xFFD850),      // Raridade.LENDARIO
    RARO(0x4FA3D9),      // Raridade.RARO
    EPICO(0xA05FD0),     // Raridade.EPICO
    VERDE(0x5BE07A),     // Passiva.AREA, ArmaExtra.AURA
    ROSA(0xFF6FA8),      // ArmaExtra.NOVELO
    ELETRICO(0x7FD8FF);  // ArmaExtra.RAIO

    /** Os degraus da rampa. */
    public static final int SOMBRA_FUNDA = 0, SOMBRA = 1, BASE = 2, LUZ = 3, BRILHO = 4;
    /** Degraus de opacidade guardados por tom. */
    private static final int DEGRAUS = 16;

    private final Color[] rampa = new Color[5];
    private final Color[][] transparentes = new Color[5][DEGRAUS + 1];

    Familia(int[] tons) {
        for (int i = 0; i < 5; i++) {
            rampa[i] = new Color(tons[i]);
        }
    }

    Familia(int oficial) {
        this(rampaDe(oficial));
    }

    static int[] rampaDe(int oficial) {
        int outline = 0x211A2B, main = 0x525779, text = 0xFCFCFC;
        int a = misturar(oficial, main, 0.2);
        return new int[] {misturar(a, outline, 0.6), misturar(a, outline, 0.3), a,
                misturar(a, text, 0.3), misturar(a, text, 0.6)};
    }

    /** Um tom da rampa, opaco. */
    public Color tom(int degrau) {
        return rampa[degrau];
    }

    /** Um tom da rampa com opacidade (0-255), arredondada para um dos degraus guardados. */
    public Color tom(int degrau, int alfa) {
        int d = Math.max(0, Math.min(DEGRAUS, Math.round(alfa / 255f * DEGRAUS)));
        Color[] linha = transparentes[degrau];
        if (linha[d] == null) {
            Color c = rampa[degrau];
            linha[d] = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, Math.round(d * 255f / DEGRAUS)));
        }
        return linha[d];
    }

    private static int misturar(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }
}
