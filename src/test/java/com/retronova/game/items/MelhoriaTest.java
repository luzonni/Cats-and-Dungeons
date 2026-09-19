package com.retronova.game.items;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das cartas de melhoria.
 *
 * O QUE ESTES TESTES GUARDAM SÃO REGRAS DE BALANCEAMENTO, e balanceamento quebra
 * calado: nada lança exceção quando uma melhoria passa a render mais na décima
 * cópia do que na primeira, ou quando o lendário começa a sair com a mesma
 * frequência do comum. O jogo continua rodando, só deixa de ser um jogo.
 *
 * As três regras que sustentam a progressão inteira:
 *
 *   1. RARIDADE É QUANTO. Um grau mais alto sempre rende mais que o anterior —
 *      senão a cor da carta mente para quem escolhe.
 *   2. REPETIR RENDE MENOS, SEMPRE. É o que faz espalhar valer mais do que
 *      insistir, e é a única coisa que impede vinte cartas de dano.
 *   3. E NUNCA RENDE ZERO. Uma cópia que não valesse nada transformaria a carta
 *      num logro: o jogador escolheria algo inerte e o jogo não teria dito.
 */
class MelhoriaTest {

    @Test
    @DisplayName("um grau mais alto sempre rende mais")
    void raridadeSobe() {
        for (Melhoria m : Melhoria.values()) {
            double anterior = 0;
            for (Raridade r : Raridade.values()) {
                double v = Math.abs(m.valor(r));
                assertTrue(v > anterior,
                        m + " em " + r + " rende " + v + ", nao mais que o grau abaixo ("
                                + anterior + ") — a cor da carta estaria mentindo");
                anterior = v;
            }
        }
    }

    @Test
    @DisplayName("repetir rende cada vez menos, e nunca mais")
    void desgasteNuncaSobe() {
        double anterior = Melhoria.desgaste(0);
        assertEquals(1.0, anterior, 1e-9, "a primeira copia tem de valer inteira");
        for (int i = 1; i < 30; i++) {
            double agora = Melhoria.desgaste(i);
            assertTrue(agora <= anterior,
                    "a copia " + (i + 1) + " rende " + agora + ", MAIS que a anterior ("
                            + anterior + ") — insistir passaria a ser melhor que espalhar");
            anterior = agora;
        }
    }

    @Test
    @DisplayName("nenhuma copia rende zero, por mais que se repita")
    void desgasteTemPiso() {
        for (int i = 0; i < 200; i++) {
            assertTrue(Melhoria.desgaste(i) >= 0.1,
                    "a copia " + (i + 1) + " nao rende nada; a carta viraria um logro");
        }
    }

    @Test
    @DisplayName("a cadencia melhora, e nao piora")
    void frenesiSubtrai() {
        // O atributo e o INTERVALO entre golpes: melhorar e DIMINUIR. Um sinal
        // trocado aqui deixaria a arma mais lenta com a carta que promete o
        // contrario, e nada no jogo acusaria.
        for (Raridade r : Raridade.values()) {
            assertTrue(Melhoria.FRENESI.valor(r) < 0,
                    "FRENESI em " + r + " esta positivo, o que ATRASA o golpe");
        }
    }

    @Test
    @DisplayName("toda melhoria tem um atributo que o motor entende")
    void todasTemAtributo() {
        for (Melhoria m : Melhoria.values()) {
            assertNotNull(m.atributo(), m + " nao mexe em nada");
            assertNotNull(m.nome());
            assertTrue(m.atributo().getHeapable(),
                    m + " mexe num atributo que SUBSTITUI em vez de somar; a segunda "
                            + "carta apagaria a primeira");
        }
    }

    @Test
    @DisplayName("o texto da carta diz o valor desta copia, nao o de tabela")
    void textoAcompanhaODesgaste() {
        String primeira = Melhoria.FURIA.efeito(Raridade.COMUM, 0);
        String oitava = Melhoria.FURIA.efeito(Raridade.COMUM, 7);
        assertTrue(!primeira.equals(oitava),
                "a oitava copia rende menos e a carta tem de dizer isso; se as duas "
                        + "prometem o mesmo, o jogador ve o desgaste como defeito");
    }

    // ------------------------------------------------------------- sorteio

    @Test
    @DisplayName("o comum sai muito mais que o lendario")
    void pesosRespeitamAOrdem() {
        int anterior = Integer.MAX_VALUE;
        for (Raridade r : Raridade.values()) {
            assertTrue(r.peso() > 0, r + " nunca sairia");
            assertTrue(r.peso() < anterior,
                    r + " sai tanto quanto o grau abaixo; um lendario frequente deixa "
                            + "de ser lendario");
            anterior = r.peso();
        }
    }

    @Test
    @DisplayName("o sorteio de raridade cobre os quatro graus e respeita a proporcao")
    void sorteioSegueOsPesos() {
        Map<Raridade, Integer> contagem = new EnumMap<>(Raridade.class);
        int rodadas = 200_000;
        for (int i = 0; i < rodadas; i++) {
            contagem.merge(Raridade.sortear(), 1, Integer::sum);
        }
        int total = 0;
        for (Raridade r : Raridade.values()) {
            total += r.peso();
        }
        for (Raridade r : Raridade.values()) {
            double esperado = r.peso() / (double) total;
            double obtido = contagem.getOrDefault(r, 0) / (double) rodadas;
            assertTrue(Math.abs(obtido - esperado) < 0.01,
                    r + " saiu " + Math.round(obtido * 100) + "% das vezes, esperado "
                            + Math.round(esperado * 100) + "%");
        }
    }

    @Test
    @DisplayName("a oferta nao repete a mesma melhoria")
    void ofertaSemRepetir() {
        Melhorias ledger = new Melhorias();
        for (int rodada = 0; rodada < 500; rodada++) {
            var oferta = ledger.oferecer(3, 0, 0);
            assertEquals(3, oferta.size());
            assertEquals(3, oferta.stream().map(Melhorias.Carta::melhoria).distinct().count(),
                    "duas cartas iguais desperdicam uma das tres opcoes");
        }
    }

    // ------------------------------------------------------- a carta de presagio

    @Test
    @DisplayName("a promocao sobe no maximo UM degrau")
    void promocaoNaoPulaDegrau() {
        // Um comum nunca pode virar lendario de uma vez: o presagio aproxima o
        // topo, nao teleporta para ele. Com a chance no teto, mil sorteios cobrem
        // de sobra qualquer encadeamento acidental.
        for (int i = 0; i < 5000; i++) {
            Raridade r = Raridade.sortear(1.0);
            assertNotNull(r);
        }
        assertEquals(Raridade.RARO, Raridade.COMUM.acima());
        assertEquals(Raridade.LENDARIO, Raridade.EPICO.acima());
    }

    @Test
    @DisplayName("o topo nao sobe mais")
    void lendarioNaoTemAcima() {
        assertEquals(Raridade.LENDARIO, Raridade.LENDARIO.acima(),
                "sem isto, promover uma lendaria estouraria o vetor de raridades");
    }

    @Test
    @DisplayName("promocao zero nao muda nada")
    void semPresagioNadaMuda() {
        Map<Raridade, Integer> contagem = new EnumMap<>(Raridade.class);
        int rodadas = 100_000;
        for (int i = 0; i < rodadas; i++) {
            contagem.merge(Raridade.sortear(0), 1, Integer::sum);
        }
        double comuns = contagem.getOrDefault(Raridade.COMUM, 0) / (double) rodadas;
        assertTrue(Math.abs(comuns - 0.60) < 0.01,
                "sem presagio o comum tem de sair em 60% das vezes, saiu " + comuns);
    }

    @Test
    @DisplayName("por mais presagio que se junte, o comum nunca some")
    void promocaoTemTeto() {
        // O TETO E O QUE MANTEM A RARIDADE EXISTINDO. Promocao certa e a mesma
        // coisa que apagar o degrau de baixo — e um sistema de quatro graus em que
        // o primeiro nunca aparece e um sistema de tres.
        int comuns = 0;
        int rodadas = 100_000;
        for (int i = 0; i < rodadas; i++) {
            if (Raridade.sortear(50.0) == Raridade.COMUM) {   // absurdo de proposito
                comuns++;
            }
        }
        double fatia = comuns / (double) rodadas;
        double esperado = 0.60 * (1 - Raridade.PROMOCAO_MAXIMA);
        assertTrue(Math.abs(fatia - esperado) < 0.02,
                "com presagio no absurdo o comum saiu " + Math.round(fatia * 100)
                        + "%, esperado " + Math.round(esperado * 100)
                        + "% — o teto nao esta segurando");
    }

    @Test
    @DisplayName("o presagio some das ultimas salas")
    void presagioSaiNoFim() {
        Melhorias ledger = new Melhorias();
        // No fim da corrida ele nao melhora mais nada: seria uma opcao morta, e
        // opcao morta pune quem ainda nao conhece o jogo.
        for (int rodada = 0; rodada < 500; rodada++) {
            var tarde = ledger.oferecer(3, 19, 0);
            assertTrue(tarde.stream().noneMatch(c -> c.melhoria() == Melhoria.PRESAGIO),
                    "Presagio foi oferecido na ultima sala, onde nao vale nada");
        }
        // E continua existindo no comeco, senao a carta nunca apareceria.
        boolean apareceu = false;
        for (int rodada = 0; rodada < 500 && !apareceu; rodada++) {
            apareceu = ledger.oferecer(3, 0, 0).stream()
                    .anyMatch(c -> c.melhoria() == Melhoria.PRESAGIO);
        }
        assertTrue(apareceu, "Presagio nunca aparece nem no inicio");
    }

    @Test
    @DisplayName("tirando o presagio ainda sobram tres cartas para oferecer")
    void semPresagioAindaHaOferta() {
        assertTrue(Melhoria.values().length - 1 >= 3,
                "nas ultimas salas o Presagio sai do baralho; sem folga a oferta "
                        + "encolheria em silencio");
    }

    @Test
    @DisplayName("ha melhorias suficientes para uma oferta de tres")
    void tresCartasPossiveis() {
        assertTrue(Melhoria.values().length >= 3,
                "com menos de tres melhorias a escolha obrigatoria deixa de ser escolha");
    }
}
