package com.retronova.game;

import com.retronova.game.Conquistas.Conquista;
import com.retronova.game.Conquistas.Progresso;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As conquistas sao funcoes do progresso: nada de disco, nada de janela. */
class ConquistasTest {

    @Test
    void progressoNovoNaoTemNada() {
        Progresso p = new Progresso();
        assertEquals(0, p.totalDesbloqueadas());
        for (Conquista c : Conquista.values()) {
            assertFalse(c.alcancada(p), c.nome());
            assertEquals(0, c.fracao(p), 1e-9);
        }
    }

    @Test
    void abatesSomamEntreCorridas() {
        Progresso p = new Progresso();
        p.registrarCorrida(60, 600, 5, 0, 10);
        p.registrarCorrida(60, 500, 5, 0, 10);
        assertEquals(1100, p.abatesTotais());
        assertTrue(Conquista.EXTERMINADOR.alcancada(p), "600 + 500 passa de mil");
        assertTrue(Conquista.PRIMEIRO_SANGUE.alcancada(p));
    }

    @Test
    void osMelhoresSoSobemNuncaDesce() {
        Progresso p = new Progresso();
        p.registrarCorrida(20 * 60, 0, 25, 12, 120);
        p.registrarCorrida(1 * 60, 0, 3, 1, 5);          // uma corrida ruim nao desfaz nada
        assertTrue(Conquista.VETERANO.alcancada(p));
        assertTrue(Conquista.EXPERIENTE.alcancada(p));
        assertTrue(Conquista.CACADOR_DE_TESOUROS.alcancada(p));
        assertTrue(Conquista.CERCADO.alcancada(p));
    }

    @Test
    void metasDeTempoSaoEscalonadas() {
        Progresso p = new Progresso();
        p.registrarCorrida(5 * 60, 0, 1, 0, 0);
        assertTrue(Conquista.SOBREVIVENTE.alcancada(p), "cinco minutos exatos contam");
        assertFalse(Conquista.VETERANO.alcancada(p));
        assertFalse(Conquista.MARATONA.alcancada(p));
    }

    @Test
    void orgulhoFelinoPedeOsTresGatosDiferentes() {
        Progresso p = new Progresso();
        p.registrarVitoria("Finn");
        p.registrarVitoria("Finn");
        p.registrarVitoria("Muffin");
        assertTrue(Conquista.CACADOR_DE_REIS.alcancada(p));
        assertEquals(2, Conquista.ORGULHO_FELINO.valor(p), "o mesmo gato duas vezes conta uma");
        assertFalse(Conquista.ORGULHO_FELINO.alcancada(p));

        p.registrarVitoria("Azrael");
        assertTrue(Conquista.ORGULHO_FELINO.alcancada(p));
    }

    @Test
    void novasDesdeListaSoOQueMudou() {
        Progresso antes = new Progresso();
        antes.registrarCorrida(60, 10, 2, 0, 5);          // ja tem o Primeiro Sangue
        Progresso depois = antes.copia();
        depois.registrarCorrida(6 * 60, 0, 2, 0, 5);      // e agora aguentou cinco minutos

        List<Conquista> novas = depois.novasDesde(antes);
        assertEquals(List.of(Conquista.SOBREVIVENTE), novas);
        assertTrue(antes.novasDesde(antes).isEmpty(), "sem mudanca, nada novo");
    }

    @Test
    void copiaNaoCompartilhaEstado() {
        Progresso p = new Progresso();
        p.registrarVitoria("Finn");
        Progresso c = p.copia();
        c.registrarVitoria("Azrael");
        assertEquals(1, p.vitorias());
        assertEquals(1, Conquista.ORGULHO_FELINO.valor(p));
        assertEquals(2, Conquista.ORGULHO_FELINO.valor(c));
    }

    @Test
    void valorMostradoNaoPassaDaMeta() {
        Progresso p = new Progresso();
        p.registrarCorrida(60, 5000, 1, 0, 0);
        assertEquals(1000, Conquista.EXTERMINADOR.valorMostrado(p), "nunca 5000 / 1000");
        assertEquals(1.0, Conquista.EXTERMINADOR.fracao(p), 1e-9);
    }

    @Test
    void jsonDaIdaEVoltaSemPerderNada() throws Exception {
        Progresso p = new Progresso();
        p.registrarCorrida(1900, 1234, 22, 11, 130);
        p.registrarVitoria("Muffin");
        p.registrarVitoria("Azrael");

        Progresso lido = Progresso.deJson(p.paraJson());
        for (Conquista c : Conquista.values()) {
            assertEquals(c.valor(p), c.valor(lido), c.nome());
        }
        assertEquals(p.totalDesbloqueadas(), lido.totalDesbloqueadas());
    }

    @Test
    void jsonTortoOuVazioViraZeroEmVezDeQuebrar() throws Exception {
        Progresso vazio = Progresso.deJson("{}");
        assertEquals(0, vazio.totalDesbloqueadas());

        Progresso torto = Progresso.deJson("{\"abatesTotais\": \"muitos\", \"melhorTempo\": -50,"
                + " \"gatosVencedores\": [1, null, \"Finn\"]}");
        assertEquals(0, torto.abatesTotais(), "texto no lugar de numero vira zero");
        assertEquals(0, Conquista.SOBREVIVENTE.valor(torto), "negativo vira zero");
        assertEquals(1, Conquista.ORGULHO_FELINO.valor(torto), "so o nome valido entra");
    }
}
