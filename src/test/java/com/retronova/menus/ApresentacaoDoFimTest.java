package com.retronova.menus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApresentacaoDoFimTest {
    @Test
    void entradasSaoSequenciaisESemExtrapolar() {
        assertEquals(0, ApresentacaoDoFim.progresso(0, 12, 22));
        assertEquals(0, ApresentacaoDoFim.progresso(12, 12, 22));
        assertTrue(ApresentacaoDoFim.progresso(20, 12, 22) > 0);
        assertEquals(0, ApresentacaoDoFim.progresso(20, 20, 22));
        float antes = 0;
        for (int t = 0; t < 100; t++) {
            float p = ApresentacaoDoFim.progresso(t, 24, 30);
            assertTrue(p >= antes && p <= 1);
            antes = p;
        }
        assertEquals(1, antes);
    }

    @Test
    void cliqueSoPodeRevelarDepoisDoTitulo() {
        var cena = new ApresentacaoDoFim();
        assertEquals(0, cena.tempo(0.9f));
        for (int t = 1; t < ApresentacaoDoFim.TITULO; t++) {
            cena.tick(); assertFalse(cena.aceitaPulo());
        }
        cena.tick();
        assertTrue(cena.aceitaPulo());
        assertFalse(cena.terminou());
        cena.concluir();
        assertTrue(cena.terminou());
        assertEquals(ApresentacaoDoFim.TOTAL, cena.tempo(0.9f));
    }

    @Test
    void terminaNaturalmenteEmUmSegundo() {
        var cena = new ApresentacaoDoFim();
        for (int t = 0; t < ApresentacaoDoFim.TOTAL - 1; t++) cena.tick();
        assertFalse(cena.terminou());
        cena.tick(); assertTrue(cena.terminou());
        for (int t = 0; t < 100; t++) cena.tick();
        assertEquals(ApresentacaoDoFim.TOTAL, cena.tempo(0.5f));
    }
}
