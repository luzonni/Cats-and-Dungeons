package com.retronova.game.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O teto do HUD: as peças de cima nunca se cruzam e as faixas não comem a arena. */
class LayoutDoHudTest {

    @Test
    void hud4Em720pCaiPara3() {
        // O caso relatado: HUD 4 em 1280x720 punha o relógio em cima da vida.
        assertEquals(3, LayoutDoHud.escalaMaxima(1280, 720, 20));
    }

    @Test
    void em1080pCabeAte5() {
        assertEquals(5, LayoutDoHud.escalaMaxima(1920, 1080, 20));
    }

    @Test
    void oBlocoDaVidaNuncaPassaPorBaixoDoRelogio() {
        int[][] telas = {{1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1080},
                {2560, 1440}, {3440, 1440}, {3840, 2160}, {1024, 768}};
        for (int[] t : telas) {
            for (int margem = 0; margem <= 40; margem += 5) {
                int s = LayoutDoHud.escalaMaxima(t[0], t[1], margem);
                int fimDaVida = margem + LayoutDoHud.BLOCO_ESQUERDO * s + LayoutDoHud.VAO * s;
                int inicioDoRelogio = t[0] / 2 - LayoutDoHud.RELOGIO * s / 2;
                assertTrue(fimDaVida <= inicioDoRelogio,
                        t[0] + "x" + t[1] + " margem " + margem + ": vida termina em " + fimDaVida
                                + ", relógio começa em " + inicioDoRelogio);
                int faixas = 2 * margem + (LayoutDoHud.FAIXA_DE_CIMA + LayoutDoHud.FAIXA_DE_BAIXO) * s;
                assertTrue(faixas <= t[1] * LayoutDoHud.FRACAO_DA_ALTURA || s == 1,
                        t[0] + "x" + t[1] + ": faixas ocupam " + faixas);
            }
        }
    }

    @Test
    void janelaMinusculaAindaTemHud() {
        assertEquals(1, LayoutDoHud.escalaMaxima(320, 180, 40));
    }
}
