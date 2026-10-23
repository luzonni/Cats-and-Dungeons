package com.retronova.game.objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A presenca do golpe, que a espada usa para sair da mao e voltar para ela.
 *
 * Ela tem de comecar e terminar em ZERO: se o primeiro quadro do golpe ou o
 * ultimo valessem outra coisa, a espada pularia entre a pose da mao e a do arco,
 * que e o defeito que ela existe para tirar.
 */
class InvestidaTest {

    @Test
    @DisplayName("A presenca sai de zero, fica em um no corte e volta a zero")
    void presenca() {
        Investida inv = Investida.espada();
        assertEquals(0, inv.presenca(0), 1e-9);
        inv.comecar();
        assertEquals(0, inv.presenca(0), 1e-9);
        int duracao = inv.duracao();
        boolean chegouAUm = false;
        double antes = 0;
        for (int t = 0; t < duracao; t++) {
            if (inv.acertaAgora()) {
                assertEquals(1, inv.presenca(0), 1e-9, "no impacto a lamina esta toda no arco");
                chegouAUm = true;
            }
            for (float a = 0; a < 1; a += 0.25f) {
                double agora = inv.presenca(a);
                assertTrue(Math.abs(agora - antes) < 0.2, "salto em t=" + t);
                antes = agora;
            }
            inv.tick();
        }
        assertTrue(chegouAUm);
        assertFalse(inv.ativa());
        assertEquals(0, inv.presenca(0), 1e-9);
    }

    @Test
    @DisplayName("O rastro so pode acender depois do preparo")
    void passouDoPreparo() {
        Investida inv = Investida.espada();
        inv.comecar();
        assertFalse(inv.passouDoPreparo());
        while (!inv.acertaAgora()) {
            inv.tick();
        }
        assertTrue(inv.passouDoPreparo());
    }
}
