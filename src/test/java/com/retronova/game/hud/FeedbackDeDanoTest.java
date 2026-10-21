package com.retronova.game.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As curvas do feedback de dano: proporcionais, contidas, e nada sem motivo. */
class FeedbackDeDanoTest {

    @Test
    void arranhaoNaoAcendeNada() {
        assertEquals(0f, FeedbackDeDano.intensidadeDoClarao(0.005), "veneno aos pouquinhos nao pisca a tela");
        assertEquals(0f, FeedbackDeDano.intensidadeDoClarao(0));
    }

    @Test
    void claraoCresceComODanoEParaNoMaximo() {
        float pequeno = FeedbackDeDano.intensidadeDoClarao(0.05);
        float medio = FeedbackDeDano.intensidadeDoClarao(0.12);
        assertTrue(pequeno > 0 && pequeno < 0.5f, "um golpe de 5% acende pouco: " + pequeno);
        assertTrue(medio > pequeno);
        assertEquals(1f, FeedbackDeDano.intensidadeDoClarao(0.2), 1e-6, "20% da vida ja e o maximo");
        assertEquals(1f, FeedbackDeDano.intensidadeDoClarao(0.9), 1e-6, "e nao passa dele");
    }

    @Test
    void pulsoSoComVidaBaixa() {
        for (int t = 0; t < 60; t++) {
            assertEquals(0f, FeedbackDeDano.intensidadeDoPulso(0.5, t), "metade da vida: nada");
            assertEquals(0f, FeedbackDeDano.intensidadeDoPulso(0.25, t), "na fronteira: nada");
            assertEquals(0f, FeedbackDeDano.intensidadeDoPulso(0, t), "morto: nada");
        }
    }

    @Test
    void pulsoBateEMaisForteComMenosVida() {
        float pico = 0, vale = 1;
        for (int t = 0; t < 60; t++) {
            float p = FeedbackDeDano.intensidadeDoPulso(0.1, t);
            pico = Math.max(pico, p);
            vale = Math.min(vale, p);
        }
        assertTrue(pico > vale, "o pulso tem batida, nao e constante");

        float quaseMorto = FeedbackDeDano.intensidadeDoPulso(0.02, 4);
        float machucado = FeedbackDeDano.intensidadeDoPulso(0.2, 4);
        assertTrue(quaseMorto > machucado, "menos vida, pulso mais forte");
        assertTrue(quaseMorto <= 1f);
    }
}
