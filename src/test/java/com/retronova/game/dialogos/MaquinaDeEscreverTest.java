package com.retronova.game.dialogos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaquinaDeEscreverTest {

    @Test
    @DisplayName("Na velocidade normal, um segundo escreve as letras por segundo dela")
    void ritmo() {
        MaquinaDeEscrever m = new MaquinaDeEscrever("a".repeat(200), 1);
        for (int i = 0; i < 60; i++) {
            m.tick();
        }
        assertEquals(MaquinaDeEscrever.LETRAS_POR_SEGUNDO[1], m.visiveis(), 1);
    }

    @Test
    @DisplayName("O primeiro aperto completa a fala; a instantânea já nasce completa")
    void completar() {
        MaquinaDeEscrever m = new MaquinaDeEscrever("Hello there, cat.", 0);
        m.tick();
        assertFalse(m.completo());
        m.completar();
        assertTrue(m.completo());
        assertTrue(new MaquinaDeEscrever("Hello", 3).completo());
    }

    @Test
    @DisplayName("Depois de um ponto seguido de espaço a escrita para um instante")
    void pausaNaFrase() {
        MaquinaDeEscrever m = new MaquinaDeEscrever("Oh. Hi", 2);
        int ticks = 0;
        while (m.visiveis() < 3) {
            m.tick();
            ticks++;
        }
        assertEquals(3, m.visiveis());
        for (int i = 0; i < MaquinaDeEscrever.PAUSA_FRASE; i++) {
            assertEquals(0, m.tick(), "escreveu durante a pausa");
        }
        m.tick();
        assertTrue(m.visiveis() > 3);
        assertTrue(ticks > 0);
    }

    @Test
    @DisplayName("A quebra de linha respeita a largura e não corta palavra")
    void quebra() {
        List<String> l = MaquinaDeEscrever.quebrar("aaa bbb ccc ddd", 7, String::length);
        assertEquals(List.of("aaa bbb", "ccc ddd"), l);
        assertEquals(List.of("palavragrande"), MaquinaDeEscrever.quebrar("palavragrande", 3, String::length));
    }
}
