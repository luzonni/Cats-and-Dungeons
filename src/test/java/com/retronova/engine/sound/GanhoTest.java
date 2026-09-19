package com.retronova.engine.sound;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da curva de volume.
 *
 * O QUE ESTÁ GUARDADO AQUI NÃO DÁ ERRO QUANDO QUEBRA, e é por isso que está em
 * teste. Uma curva de volume errada não lança exceção nem falha o build: ela
 * apenas soa mal, e "soa mal" é justamente o que ninguém consegue afirmar com
 * segurança sem medir. Três propriedades importam.
 *
 * AS PONTAS SÃO EXATAS. Topo tem que dar 1 — o slider no máximo é o nível de
 * referência da mixagem, não "quase ele" —, e o pé tem que dar 0 de verdade.
 * Exponencial pura nunca chega a zero; se a rampa do pé sumir num refactor, o
 * mínimo do slider passa a deixar vazar -40 dB e ninguém percebe até alguém
 * reclamar de som em sala silenciosa.
 *
 * A CURVA É PERCEPTUALMENTE UNIFORME. É a razão de ela existir: passos iguais do
 * slider devem valer o mesmo tanto de mudança percebida. Em decibéis isso quer
 * dizer que a diferença entre 40 e 50 tem que ser igual à diferença entre 80 e
 * 90 — que é exatamente o que a curva linear antiga não fazia.
 *
 * A MIGRAÇÃO É REDONDA. {@code posicaoDe} é a inversa de {@code de}, e é ela que
 * impede o config.json de quem já jogava de virar silêncio na primeira abertura.
 */
class GanhoTest {

    private static double db(double ganho) {
        return 20 * Math.log10(ganho);
    }

    @Test
    @DisplayName("o topo do slider é o nível de referência, exatamente 1")
    void topo() {
        assertEquals(1.0, Ganho.de(100), 1e-9);
    }

    @Test
    @DisplayName("o pé do slider é silêncio de verdade, e não -40 dB")
    void pe() {
        assertEquals(0.0, Ganho.de(0), 1e-12);
    }

    @Test
    @DisplayName("nunca sai do intervalo, nem com valor fora da escala")
    void limites() {
        for (int v : new int[] {-50, -1, 0, 1, 50, 99, 100, 101, 900}) {
            double g = Ganho.de(v);
            assertTrue(g >= 0 && g <= 1, "ganho fora de [0,1] em " + v + ": " + g);
        }
    }

    @Test
    @DisplayName("a curva é monotônica: subir o slider nunca baixa o som")
    void monotonica() {
        double anterior = -1;
        for (int v = 0; v <= 100; v++) {
            double g = Ganho.de(v);
            assertTrue(g > anterior, "nao subiu em " + v);
            anterior = g;
        }
    }

    @Test
    @DisplayName("passos iguais do slider valem decibéis iguais")
    void perceptualmenteUniforme() {
        // Fora da rampa do pé, que é linear de propósito.
        double referencia = db(Ganho.de(50)) - db(Ganho.de(40));
        for (int v = 20; v + 10 <= 100; v += 10) {
            double passo = db(Ganho.de(v + 10)) - db(Ganho.de(v));
            assertEquals(referencia, passo, 0.01,
                    "o passo de " + v + " para " + (v + 10) + " destoa dos outros");
        }
    }

    @Test
    @DisplayName("o curso inteiro cobre os 40 dB prometidos")
    void alcance() {
        // Dez passos de 10% cobrindo 40 dB dão 4 dB por passo. É a mesma coisa que
        // dizer "o alcance é 40 dB", só que medida onde ela importa: no que o
        // jogador sente ao arrastar.
        assertEquals(4.0, db(Ganho.de(100)) - db(Ganho.de(90)), 0.01);
        // E o topo da rampa cai onde a exponencial diz: A·e^(0,1·B) = 0,0158.
        assertEquals(-36.0, db(Ganho.de(10)), 0.1);
    }

    @Test
    @DisplayName("posicaoDe desfaz de: a migração devolve o mesmo volume")
    void migracaoEhRedonda() {
        for (int v = 10; v <= 100; v += 5) {
            double ganho = Ganho.de(v);
            assertEquals(v, Ganho.posicaoDe(ganho), 1,
                    "ida e volta perdeu a posicao " + v);
        }
    }

    @Test
    @DisplayName("o padrão antigo de 20% vira uma posição que soa igual")
    void migracaoDoPadraoAntigo() {
        // No mundo linear, MUSIC=20 valia ganho 0,20. Na curva nova a posicao que
        // produz 0,20 e 65 — e e esse o numero que o config.json antigo tem que
        // receber, senao 20 lidos pela curva nova dariam -32 dB, dezoito abaixo.
        int migrado = Ganho.posicaoDe(0.20);
        assertEquals(65, migrado, 1);
        assertEquals(0.20, Ganho.de(migrado), 0.005);
        assertTrue(db(Ganho.de(20)) < -30,
                "se 20 nao fosse muito mais baixo na curva nova, a migracao seria inutil");
    }

    @Test
    @DisplayName("ganho zero e negativo não quebram a inversa")
    void inversaNoPe() {
        assertEquals(0, Ganho.posicaoDe(0));
        assertEquals(0, Ganho.posicaoDe(-1));
    }
}
