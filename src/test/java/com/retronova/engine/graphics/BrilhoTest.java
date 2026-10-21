package com.retronova.engine.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O cache do Brilho: a cor fica, a opacidade cai em degraus, e o numero de imagens e limitado. */
class BrilhoTest {

    @Test
    void aCorSeMantemNaChave() {
        int chave = Brilho.chave(new Color(255, 200, 60, 128));
        assertEquals(0xFFC83C, chave & 0xFFFFFF);
    }

    @Test
    void opacidadeCheiaContinuaCheia() {
        assertEquals(255, Brilho.chave(new Color(10, 20, 30, 255)) >>> 24);
    }

    @Test
    void opacidadeFicaPertoDaPedida() {
        for (int a = 1; a <= 255; a++) {
            int obtida = Brilho.chave(new Color(0, 0, 0, a)) >>> 24;
            assertTrue(Math.abs(obtida - a) <= 255 / 16 / 2 + 1, "pedida " + a + ", obtida " + obtida);
        }
    }

    @Test
    void oPulsoDaAuraNaoEnchesOCache() {
        // A aura pulsa entre 40 e 140 de opacidade: no maximo uns poucos degraus.
        Set<Integer> chaves = new HashSet<>();
        for (int a = 40; a <= 140; a++) {
            chaves.add(Brilho.chave(new Color(255, 200, 60, a)));
        }
        assertTrue(chaves.size() <= 8, "imagens para o pulso inteiro: " + chaves.size());
    }
}
