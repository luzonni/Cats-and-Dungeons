package com.retronova.game.hud;

import com.retronova.engine.EscalaDaInterface;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O layout do HUD em toda a matriz: dada a resolução e as preferências, a escala
 * em vigor (a pedida, no teto do layout) nunca põe peça fora da tela nem uma em
 * cima da outra.
 */
class LayoutDoHudTest {

    /** A matriz da correção de resolução, mais 4:3 e uma janela pequena. */
    private static final int[][] TELAS = {
            {1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160},
            {2560, 1080}, {3440, 1440}, {1024, 768}, {800, 600}
    };

    @Test
    void nenhumaPecaSaiDaTelaNemCruzaOutraEmNenhumaCombinacao() {
        for (int[] t : TELAS) {
            for (int ui = EscalaDaInterface.PCT_MINIMO; ui <= EscalaDaInterface.PCT_MAXIMO; ui += EscalaDaInterface.PASSO) {
                for (int hud = EscalaDaInterface.PCT_MINIMO; hud <= EscalaDaInterface.PCT_MAXIMO; hud += EscalaDaInterface.PASSO) {
                    for (int margemRef = 0; margemRef <= EscalaDaInterface.MARGEM_MAXIMA; margemRef += 4) {
                        int margem = EscalaDaInterface.margem(t[0], t[1], margemRef);
                        int s = Math.min(EscalaDaInterface.hud(t[0], t[1], ui, hud),
                                LayoutDoHud.escalaMaxima(t[0], t[1], margem));
                        String caso = t[0] + "x" + t[1] + " ui " + ui + "% hud " + hud + "% margem " + margemRef;
                        if (s > 1) {
                            assertTrue(LayoutDoHud.cabe(t[0], t[1], margem, s), caso + " escala " + s);
                        }
                        for (LayoutDoHud.Peca p : LayoutDoHud.Peca.values()) {
                            Rectangle r = LayoutDoHud.caixa(p, t[0], t[1], margem, s);
                            assertTrue(r.x >= 0 && r.y >= 0 && r.x + r.width <= t[0] && r.y + r.height <= t[1],
                                    caso + ": " + p + " fora da tela " + r);
                        }
                    }
                }
            }
        }
    }

    @Test
    void cemPorCentoOcupaAMesmaFracaoNasResolucoesMultiplasDaReferencia() {
        // 720p, 1080p, 1440p e 4K são múltiplos de 640x360: a escala é o próprio
        // múltiplo, e o HUD ocupa exatamente a mesma fração da tela.
        int[][] multiplas = {{1280, 720, 2}, {1920, 1080, 3}, {2560, 1440, 4}, {3840, 2160, 6}};
        for (int[] m : multiplas) {
            assertEquals(m[2], EscalaDaInterface.hud(m[0], m[1], 100, 100), m[0] + "x" + m[1]);
            assertEquals(1.0 / EscalaDaInterface.REF_LARGURA, m[2] / (double) m[0], 1e-9);
        }
    }

    @Test
    void aEscalaEMultiplicativaENaoSomada() {
        // 200% do HUD com 50% da interface é 100%: o produto, e não a soma.
        assertEquals(EscalaDaInterface.hud(1920, 1080, 100, 100), EscalaDaInterface.hud(1920, 1080, 50, 200));
        // E a escala sai dos valores de referência, não do valor já escalado.
        assertEquals(6, EscalaDaInterface.hud(1920, 1080, 100, 200));
    }

    @Test
    void textoNuncaFicaIlegivelDe720pParaCima() {
        for (int[] t : TELAS) {
            if (EscalaDaInterface.fator(t[0], t[1]) >= 2) {
                assertTrue(EscalaDaInterface.hud(t[0], t[1], 50, 50) >= EscalaDaInterface.ESCALA_LEGIVEL);
                assertTrue(EscalaDaInterface.menus(t[0], t[1], 50, 50) >= EscalaDaInterface.ESCALA_LEGIVEL);
            }
        }
    }

    @Test
    void osMenusPadraoContinuamComoAntes() {
        // A migração não muda o que o jogador via: 3 em 720p, 5 em 1080p.
        assertEquals(3, EscalaDaInterface.menus(1280, 720, 100, 100));
        assertEquals(5, EscalaDaInterface.menus(1920, 1080, 100, 100));
    }

    @Test
    void osMenusNuncaPassamDaTela() {
        for (int[] t : TELAS) {
            int s = EscalaDaInterface.menus(t[0], t[1], 200, 200);
            assertTrue(EscalaDaInterface.MENU_MINIMO_LARGURA * s <= t[0]
                    && EscalaDaInterface.MENU_MINIMO_ALTURA * s <= t[1], t[0] + "x" + t[1]);
        }
    }

    @Test
    void oSliderParaNoQueCabe() {
        int teto = LayoutDoHud.escalaMaxima(1280, 720, EscalaDaInterface.margem(1280, 720, 7));
        int max = EscalaDaInterface.maiorPctQueCabe(p -> EscalaDaInterface.hud(1280, 720, 100, p), teto);
        assertTrue(EscalaDaInterface.hud(1280, 720, 100, max) <= teto);
        assertFalse(max < EscalaDaInterface.PCT_MAXIMO
                && EscalaDaInterface.hud(1280, 720, 100, max + EscalaDaInterface.PASSO) <= teto);
    }

    @Test
    void valorGravadoForaDaFaixaVoltaParaElaNoPasso() {
        assertEquals(50, EscalaDaInterface.pctValido(10));
        assertEquals(200, EscalaDaInterface.pctValido(900));
        assertEquals(125, EscalaDaInterface.pctValido(133));
    }
}
