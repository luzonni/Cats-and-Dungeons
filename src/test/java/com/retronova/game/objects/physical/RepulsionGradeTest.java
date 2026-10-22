package com.retronova.game.objects.physical;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** As contas da grade da Repulsion: em que celula cai cada ponto, e chaves sem colisao. */
class RepulsionGradeTest {

    @Test
    void celulaArredondaParaBaixoInclusiveNoNegativo() {
        assertEquals(0, Repulsion.celulaDe(0, 48));
        assertEquals(0, Repulsion.celulaDe(47.9, 48));
        assertEquals(1, Repulsion.celulaDe(48, 48));
        // Sem floor, -0,5 viraria 0 e a celula -1 se fundiria com a 0.
        assertEquals(-1, Repulsion.celulaDe(-0.5, 48));
        assertEquals(-2, Repulsion.celulaDe(-48.1, 48));
    }

    @Test
    void chavesDeCelulasVizinhasNaoColidem() {
        Set<Long> chaves = new HashSet<>();
        for (int x = -50; x <= 50; x++) {
            for (int y = -50; y <= 50; y++) {
                chaves.add(Repulsion.chave(x, y));
            }
        }
        assertEquals(101 * 101, chaves.size());
    }

    @Test
    void trocarXeYDaOutraChave() {
        assertNotEquals(Repulsion.chave(3, 7), Repulsion.chave(7, 3));
        assertNotEquals(Repulsion.chave(-1, 0), Repulsion.chave(0, -1));
    }

    @Test
    void doisCorposQueSeTocamFicamEmCelulasVizinhas() {
        // Lado da celula = maior corpo. Centros a menos de um lado de distancia
        // nunca ficam a mais de uma celula, em nenhum eixo.
        double celula = 48;
        for (double a = -200; a < 200; a += 7.3) {
            for (double d = 0; d < celula; d += 5.1) {
                int ca = Repulsion.celulaDe(a, celula), cb = Repulsion.celulaDe(a + d, celula);
                assertEquals(true, Math.abs(ca - cb) <= 1, "a=" + a + " d=" + d);
            }
        }
    }
}
