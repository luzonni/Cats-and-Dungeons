package com.retronova.game.items;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A regra do baú, separada da apresentação (ver RecompensaDoBau). O que estes
 * testes guardam é que trocar a tela não mudou o que o jogador recebe.
 */
class RecompensaDoBauTest {

    @Test
    @DisplayName("todo prêmio sorteado é uma arma ou uma carta, nunca os dois nem nenhum")
    void premioTemUmaCoisaSo() {
        Melhorias m = new Melhorias();
        for (int i = 0; i < 2000; i++) {
            RecompensaDoBau.Premio p = RecompensaDoBau.sortear(m, 0);
            assertTrue((p.arma() == null) != (p.carta() == null), "prêmio com " + p);
            assertNotNull(p.nome());
            assertNotNull(p.efeito());
            assertNotNull(p.cor());
            assertNotNull(p.grau());
        }
    }

    @Test
    @DisplayName("sem arma nenhuma, arma sai perto de 55% das vezes")
    void chanceDeArmaSemNenhuma() {
        Melhorias m = new Melhorias();
        int armas = 0, n = 20000;
        for (int i = 0; i < n; i++) {
            if (RecompensaDoBau.sortear(m, 0).arma() != null) {
                armas++;
            }
        }
        assertEquals(RecompensaDoBau.CHANCE_DE_ARMA_SEM_NENHUMA, armas / (double) n, 0.02);
    }

    @Test
    @DisplayName("arma sorteada vem no próximo nível dela")
    void armaNoProximoNivel() {
        Melhorias m = new Melhorias();
        ArmaExtra primeira = m.armasDisponiveis().getFirst();
        m.subirArma(primeira);
        for (int i = 0; i < 5000; i++) {
            RecompensaDoBau.Premio p = RecompensaDoBau.sortear(m, 0);
            if (p.arma() != null) {
                assertEquals(m.nivel(p.arma()) + 1, p.nivelNovo(), p.toString());
            }
        }
    }

    @Test
    @DisplayName("o grau da arma: nova é épica, nível é raro; evolução é lendária")
    void grauDaArma() {
        ArmaExtra a = ArmaExtra.values()[0];
        assertEquals(Raridade.EPICO, new RecompensaDoBau.Premio(null, a, 1).grau());
        assertEquals(Raridade.RARO, new RecompensaDoBau.Premio(null, a, 2).grau());
        assertEquals(Raridade.LENDARIO, new RecompensaDoBau.Premio(null, a, RecompensaDoBau.EVOLUCAO).grau());
    }

    @Test
    @DisplayName("a prévia lista as armas disponíveis e todas as melhorias")
    void previaCobreOPool() {
        Melhorias m = new Melhorias();
        List<String> possiveis = RecompensaDoBau.possiveis(m);
        for (ArmaExtra a : m.armasDisponiveis()) {
            assertTrue(possiveis.contains(a.name()), a.name());
        }
        for (Melhoria mel : Melhoria.values()) {
            assertTrue(possiveis.contains(mel.name()), mel.name());
        }
    }
}
