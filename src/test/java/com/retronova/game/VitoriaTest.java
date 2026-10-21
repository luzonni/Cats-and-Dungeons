package com.retronova.game;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A vitoria sobre o chefe final: paga uma vez, aparece no resumo e some ao reiniciar. */
class VitoriaTest {

    private static String benchAntes;

    @BeforeAll
    static void semGravarNoDisco() {
        // Em modo bench o Meta nao grava: o teste nao mexe no meta.json de ninguem.
        benchAntes = System.getProperty("cd.bench");
        System.setProperty("cd.bench", "true");
    }

    @AfterAll
    static void restaurar() {
        if (benchAntes == null) {
            System.clearProperty("cd.bench");
        } else {
            System.setProperty("cd.bench", benchAntes);
        }
    }

    @BeforeEach
    void corridaNova() {
        Estatisticas.reiniciar();
    }

    @Test
    void vitoriaSoContaUmaVezPorCorrida() {
        assertFalse(Estatisticas.venceu());
        assertTrue(Estatisticas.registrarVitoria("Finn"), "a primeira vez conta");
        assertTrue(Estatisticas.venceu());
        assertFalse(Estatisticas.registrarVitoria("Finn"), "a segunda nao paga de novo");
    }

    @Test
    void reiniciarApagaAVitoria() {
        Estatisticas.registrarVitoria("Finn");
        Estatisticas.reiniciar();
        assertFalse(Estatisticas.venceu());
        assertTrue(Estatisticas.registrarVitoria("Finn"), "a corrida nova pode vencer de novo");
    }

    @Test
    void resumoSomaOBonusSoQuandoVenceu() {
        int base = Meta.ouroDaCorrida(1800, 400);

        Estatisticas.encerrar(1800, 400, 20, "Finn");
        assertEquals(base, Estatisticas.ouroGanho(), "sem vitoria, so o ouro da corrida");

        Estatisticas.registrarVitoria("Finn");
        Estatisticas.encerrar(1800, 400, 20, "Finn");
        assertEquals(base + Meta.BONUS_DE_VITORIA, Estatisticas.ouroGanho(),
                "com vitoria, o bonus entra no numero mostrado");
    }

    @Test
    void vencerPagaMaisQueSobreviverAteOFim() {
        // Trinta minutos de tempo, sem nenhum abate, rendem 300. O bonus tem de ser
        // um acrescimo que se sinta, e nao um arredondamento.
        assertTrue(Meta.BONUS_DE_VITORIA >= Meta.ouroDaCorrida(1800, 0) / 2);
    }
}
