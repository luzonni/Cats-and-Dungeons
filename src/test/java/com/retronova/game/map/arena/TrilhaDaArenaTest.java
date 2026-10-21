package com.retronova.game.map.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A musica dinamica: volume pela horda, calmaria antes do chefe, e quanto falta para ele. */
class TrilhaDaArenaTest {

    private static final int LONGE = Integer.MAX_VALUE;

    @Test
    void volumeSobeComAHorda() {
        assertEquals(TrilhaDaArena.VOLUME_CALMO, TrilhaDaArena.volumeAlvo(0, LONGE, false), 1e-9);
        assertEquals(1.0, TrilhaDaArena.volumeAlvo(1, LONGE, false), 1e-9);
        assertEquals(1.0, TrilhaDaArena.volumeAlvo(5, LONGE, false), 1e-9, "horda acima do cheio nao estoura");
        assertTrue(TrilhaDaArena.volumeAlvo(0.5, LONGE, false) > TrilhaDaArena.volumeAlvo(0.2, LONGE, false));
    }

    @Test
    void comChefeATrilhaDeleVemCheia() {
        assertEquals(1.0, TrilhaDaArena.volumeAlvo(0, 0, true), 1e-9);
        assertEquals(1.0, TrilhaDaArena.volumeAlvo(0, LONGE, true), 1e-9);
    }

    @Test
    void calmariaSoNosUltimosVinteSegundos() {
        double normal = TrilhaDaArena.volumeAlvo(1, LONGE, false);
        assertEquals(normal, TrilhaDaArena.volumeAlvo(1, TrilhaDaArena.CALMARIA, false), 1e-9,
                "na fronteira ainda nao comecou");
        assertTrue(TrilhaDaArena.volumeAlvo(1, 10, false) < normal, "no meio ja baixou");
        assertEquals(TrilhaDaArena.FUNDO_DA_CALMARIA, TrilhaDaArena.volumeAlvo(1, 0, false), 1e-9,
                "na hora do chefe, no fundo");
        assertTrue(TrilhaDaArena.volumeAlvo(1, 5, false) < TrilhaDaArena.volumeAlvo(1, 15, false),
                "quanto mais perto, mais baixo");
    }

    @Test
    void contaAteOProximoChefe() {
        // Corrida no comeco: o primeiro chefe gato vem aos 5 minutos.
        assertEquals(300, Waves.segundosAteOProximoChefe(0, 1, false));
        assertEquals(30, Waves.segundosAteOProximoChefe(4.5, 1, false));
        // Depois do ultimo gato (25 min), o proximo e o rei, aos 30.
        assertEquals(240, Waves.segundosAteOProximoChefe(26, 6, false));
        // Na hora, zero; nunca negativo.
        assertEquals(0, Waves.segundosAteOProximoChefe(30.5, 6, false));
        // Rei ja nasceu e nao ha mais gato por vir: nenhum chefe.
        assertEquals(Integer.MAX_VALUE, Waves.segundosAteOProximoChefe(31, 7, true));
    }
}
