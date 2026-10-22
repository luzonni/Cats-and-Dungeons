package com.retronova.game.hud;

import com.retronova.engine.EscalaDaInterface;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O layout do HUD em toda a matriz: dada a resolução e as preferências, a escala
 * em vigor (o passo pedido, no teto do layout) nunca põe peça fora da tela nem
 * uma em cima da outra.
 */
class LayoutDoHudTest {

    /** A matriz da correção de resolução, mais 4:3 e uma janela pequena. */
    private static final int[][] TELAS = {
            {1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160},
            {2560, 1080}, {3440, 1440}, {1024, 768}, {800, 600}
    };

    private static EscalaDaInterface.Faixa hud(int[] t, int margem) {
        return EscalaDaInterface.faixaDoHud(t[0], t[1], LayoutDoHud.escalaMaxima(t[0], t[1], margem));
    }

    @Test
    void nenhumaPecaSaiDaTelaNemCruzaOutraEmNenhumaCombinacao() {
        for (int[] t : TELAS) {
            for (int tam = EscalaDaInterface.TAMANHO_MINIMO; tam <= EscalaDaInterface.TAMANHO_MAXIMO; tam++) {
                for (int margemRef = 0; margemRef <= EscalaDaInterface.MARGEM_MAXIMA; margemRef += 4) {
                    int margem = EscalaDaInterface.margem(t[0], t[1], margemRef);
                    int s = hud(t, margem).escala(tam);
                    String caso = t[0] + "x" + t[1] + " passo " + tam + " margem " + margemRef;
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

    @Test
    void oPadraoOcupaAMesmaFracaoNasResolucoesMultiplasDaReferencia() {
        // 720p, 1080p, 1440p e 4K são múltiplos de 640x360: a escala é o próprio
        // múltiplo, e o HUD ocupa exatamente a mesma fração da tela.
        int[][] multiplas = {{1280, 720, 2}, {1920, 1080, 3}, {2560, 1440, 4}, {3840, 2160, 6}};
        for (int[] m : multiplas) {
            assertEquals(m[2], EscalaDaInterface.faixaDoHud(m[0], m[1], 16).escala(0), m[0] + "x" + m[1]);
            assertEquals(1.0 / EscalaDaInterface.REF_LARGURA, m[2] / (double) m[0], 1e-9);
        }
    }

    @Test
    void osMenusPadraoSaoOTamanhoEscolhido() {
        // O padrão é o tamanho escolhido nos testes: 2 em 720p, 3 em 1080p.
        assertEquals(2, EscalaDaInterface.faixaDosMenus(1280, 720).escala(0));
        assertEquals(3, EscalaDaInterface.faixaDosMenus(1920, 1080).escala(0));
    }

    @Test
    void cadaPassoOferecidoMudaOTamanhoDeVerdade() {
        // A barra só mostra passos que existem: de menor() a maior(), cada um dá
        // uma escala diferente da do vizinho, e nada passa do teto da tela.
        for (int[] t : TELAS) {
            EscalaDaInterface.Faixa[] faixas = {
                    hud(t, EscalaDaInterface.margem(t[0], t[1], EscalaDaInterface.MARGEM_PADRAO)),
                    EscalaDaInterface.faixaDosMenus(t[0], t[1])};
            for (EscalaDaInterface.Faixa f : faixas) {
                assertTrue(f.menor() <= 0 && 0 <= f.maior(), t[0] + "x" + t[1] + " sem o Default: " + f);
                for (int tam = f.menor(); tam < f.maior(); tam++) {
                    assertEquals(f.escala(tam) + 1, f.escala(tam + 1), t[0] + "x" + t[1] + " " + f);
                }
                assertTrue(f.escala(f.maior()) <= f.teto());
            }
        }
    }

    @Test
    void passoGravadoQueATelaNaoComportaVoltaParaOQueCabe() {
        // "Largest" gravado em 4K e aberto em 720p vira o maior que cabe lá.
        EscalaDaInterface.Faixa menus720 = EscalaDaInterface.faixaDosMenus(1280, 720);
        assertEquals(menus720.maior(), menus720.efetivo(EscalaDaInterface.TAMANHO_MAXIMO));
        assertEquals(menus720.teto(), menus720.escala(EscalaDaInterface.TAMANHO_MAXIMO));
        assertEquals(EscalaDaInterface.TAMANHO_MAXIMO, EscalaDaInterface.tamanhoValido(99));
        assertEquals("Default", EscalaDaInterface.nome(0));
    }

    @Test
    void textoNuncaFicaIlegivelDe720pParaCima() {
        for (int[] t : TELAS) {
            if (EscalaDaInterface.fator(t[0], t[1]) >= 2) {
                int margem = EscalaDaInterface.margem(t[0], t[1], EscalaDaInterface.MARGEM_PADRAO);
                assertTrue(hud(t, margem).escala(EscalaDaInterface.TAMANHO_MINIMO) >= EscalaDaInterface.ESCALA_LEGIVEL);
                assertTrue(EscalaDaInterface.faixaDosMenus(t[0], t[1]).escala(EscalaDaInterface.TAMANHO_MINIMO)
                        >= EscalaDaInterface.ESCALA_LEGIVEL);
            }
        }
    }

    @Test
    void osMenusNuncaPassamDaTela() {
        for (int[] t : TELAS) {
            int s = EscalaDaInterface.faixaDosMenus(t[0], t[1]).escala(EscalaDaInterface.TAMANHO_MAXIMO);
            assertTrue(EscalaDaInterface.MENU_MINIMO_LARGURA * s <= t[0]
                    && EscalaDaInterface.MENU_MINIMO_ALTURA * s <= t[1], t[0] + "x" + t[1]);
        }
    }
}
