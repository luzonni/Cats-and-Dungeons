package com.retronova.engine.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O teto de vozes por efeito: o que o mixer deixa de misturar sem ninguem ouvir a diferenca. */
class LimiteDeVozesTest {

    private static final long LONGO = 2000;   // um efeito de dois segundos

    @Test
    void oMesmoSomNoMesmoInstanteSoTocaUmaVez() {
        LimiteDeVozes l = new LimiteDeVozes();
        assertTrue(l.tocar(Sounds.Sword, 1000, LONGO));
        assertFalse(l.tocar(Sounds.Sword, 1000, LONGO), "mesmo milissegundo");
        assertFalse(l.tocar(Sounds.Sword, 1000 + LimiteDeVozes.INTERVALO_MINIMO - 1, LONGO));
        assertTrue(l.tocar(Sounds.Sword, 1000 + LimiteDeVozes.INTERVALO_MINIMO, LONGO));
    }

    @Test
    void naoPassaDoTetoDeVozesSimultaneas() {
        LimiteDeVozes l = new LimiteDeVozes();
        long t = 0;
        for (int i = 0; i < LimiteDeVozes.VOZES_POR_SOM; i++) {
            assertTrue(l.tocar(Sounds.Coin, t, LONGO), "voz " + (i + 1));
            t += LimiteDeVozes.INTERVALO_MINIMO;
        }
        assertFalse(l.tocar(Sounds.Coin, t, LONGO), "a voz a mais e descartada");
    }

    @Test
    void quandoUmaVozTerminaAbreLugar() {
        LimiteDeVozes l = new LimiteDeVozes();
        long t = 0;
        for (int i = 0; i < LimiteDeVozes.VOZES_POR_SOM; i++) {
            l.tocar(Sounds.Coin, t, 300);
            t += LimiteDeVozes.INTERVALO_MINIMO;
        }
        assertFalse(l.tocar(Sounds.Coin, t, 300));
        assertTrue(l.tocar(Sounds.Coin, 300, 300), "a primeira terminou em 300 ms");
    }

    @Test
    void efeitosDiferentesNaoDisputamOMesmoTeto() {
        LimiteDeVozes l = new LimiteDeVozes();
        long t = 0;
        for (int i = 0; i < LimiteDeVozes.VOZES_POR_SOM; i++) {
            l.tocar(Sounds.Coin, t, LONGO);
            t += LimiteDeVozes.INTERVALO_MINIMO;
        }
        assertTrue(l.tocar(Sounds.Sword, t, LONGO), "moedas cheias nao calam a espada");
        assertTrue(l.tocar(Sounds.Woosh, t, LONGO));
    }
}
