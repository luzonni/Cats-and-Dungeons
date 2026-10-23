package com.retronova.menus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class LayoutDoFimTest {
    @ParameterizedTest
    @CsvSource({"640,360,1", "960,540,2", "1280,720,2", "1740,980,3", "1920,1080,3", "1920,1080,4"})
    void relatorioOcupaJanelaSemEncolherFonte(int w, int h, int s) {
        int topo = 30 * s, base = h - 26 * s;
        var l = LayoutDoFim.calcular(w, s, topo, base, 22 * s, 6 * s);
        assertTrue(l.abas().width > w * 0.88);
        assertEquals(12 * s, l.personagem().x);
        assertEquals(w - 12 * s, l.danos().x + l.danos().width);
        assertTrue(l.personagem().width > 0 && l.atributos().width > 0 && l.danos().width > 0);
        assertTrue(l.personagem().y + l.personagem().height < l.botoesY());
        assertTrue(l.atributos().x > l.personagem().x + l.personagem().width);
        assertTrue(l.danos().x > l.atributos().x + l.atributos().width);
        assertEquals(l.loja().x + l.loja().width, l.danos().x + l.danos().width);
        assertTrue(l.personagem().height > 0);
    }
}
