package com.retronova.game.items;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As curvas da animação da arma na mão.
 *
 * O QUE SE GUARDA AQUI É A AUSÊNCIA DE SALTO. A camada existe para a arma não
 * pular de pose (no começo do golpe, ao virar, ao perder o alvo), e um salto não
 * dá erro: só aparece na tela, um quadro, e passa. Estes testes andam as curvas
 * em passos de um quarto de tick e conferem que nenhum passo muda o gesto mais
 * do que a ação mais rápida permite, e que o pico cai no tick do disparo.
 */
class AnimacaoDaArmaTest {

    private static final int ACAO = 3, VOLTA = 12;

    @Test
    @DisplayName("O pico do gesto cai no fim da ação, contado do tick real do disparo")
    void picoNoDisparo() {
        assertEquals(1.0, AnimacaoDaArma.curvaDoGesto(0, ACAO, ACAO, VOLTA, 1), 1e-9);
        assertEquals(-0.7, AnimacaoDaArma.curvaDoGesto(0, 0, ACAO, VOLTA, 0.7), 1e-9,
                "a ação parte de onde a antecipação estava, e não de -1");
    }

    @Test
    @DisplayName("Fora do gesto, a curva é a antecipação, e volta a ela sem degrau")
    void voltaParaAAntecipacao() {
        assertEquals(-0.4, AnimacaoDaArma.curvaDoGesto(0.4, ACAO + VOLTA, ACAO, VOLTA, 0), 1e-9);
        double quaseNoFim = AnimacaoDaArma.curvaDoGesto(0.4, ACAO + VOLTA - 1e-6, ACAO, VOLTA, 0);
        assertEquals(-0.4, quaseNoFim, 1e-4);
    }

    @Test
    @DisplayName("Nenhum passo de um quarto de tick salta mais do que a ação permite")
    void semSalto() {
        double maximo = 2.0 / ACAO / 4 * 3;   // ease-out cúbica: até 3x a média no início
        double antes = AnimacaoDaArma.curvaDoGesto(0.8, 0, ACAO, VOLTA, 0.8);
        for (double t = 0.25; t <= ACAO + VOLTA + 2; t += 0.25) {
            double agora = AnimacaoDaArma.curvaDoGesto(0.8, t, ACAO, VOLTA, 0.8);
            assertTrue(Math.abs(agora - antes) <= maximo + 1e-9,
                    "salto de " + (agora - antes) + " em t=" + t);
            antes = agora;
        }
    }

    @Test
    @DisplayName("O clarão nasce cheio no disparo e apaga na metade da volta")
    void clarao() {
        assertEquals(1.0, AnimacaoDaArma.claraoDoDisparo(0, ACAO, VOLTA), 1e-9);
        assertEquals(0.0, AnimacaoDaArma.claraoDoDisparo(ACAO + VOLTA / 2.0, ACAO, VOLTA), 1e-9);
        assertEquals(0.0, AnimacaoDaArma.claraoDoDisparo(1e9, ACAO, VOLTA), 1e-9);
    }

    @Test
    @DisplayName("A virada começa e termina em zero: a arma se recolhe e volta à pose")
    void virada() {
        assertEquals(0.0, AnimacaoDaArma.curvaDaVirada(1), 1e-9);
        assertEquals(0.0, AnimacaoDaArma.curvaDaVirada(0), 1e-9);
        assertEquals(1.0, AnimacaoDaArma.curvaDaVirada(0.66), 1e-2);
    }
}
