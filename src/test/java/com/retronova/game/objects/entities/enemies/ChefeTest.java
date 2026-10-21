package com.retronova.game.objects.entities.enemies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A conta das fases: fatias iguais da vida, sem abrir o jogo. */
class ChefeTest {

    @Test
    void tresFasesComecamEmDoisTercosEUmTerco() {
        assertEquals(0, Chefe.faseDe(100, 100, 3), "vida cheia e a primeira fase");
        assertEquals(0, Chefe.faseDe(67, 100, 3), "logo acima de 66% ainda e a primeira");
        assertEquals(1, Chefe.faseDe(66, 100, 3), "a segunda comeca em 66%");
        assertEquals(1, Chefe.faseDe(34, 100, 3));
        assertEquals(2, Chefe.faseDe(33, 100, 3), "a terceira comeca em 33%");
        assertEquals(2, Chefe.faseDe(1, 100, 3));
    }

    @Test
    void duasFasesVirammNaMetade() {
        assertEquals(0, Chefe.faseDe(51, 100, 2));
        assertEquals(1, Chefe.faseDe(50, 100, 2));
    }

    @Test
    void chefeDeUmaFaseSoNuncaMuda() {
        assertEquals(0, Chefe.faseDe(100, 100, 1));
        assertEquals(0, Chefe.faseDe(1, 100, 1));
    }

    @Test
    void vidaFeraDoIntervaloFicaNasPontas() {
        assertEquals(2, Chefe.faseDe(0, 100, 3), "morto e a ultima fase");
        assertEquals(2, Chefe.faseDe(-10, 100, 3));
        assertEquals(0, Chefe.faseDe(150, 100, 3), "vida acima do maximo nao passa da primeira");
    }

    @Test
    void vidaMaximaInvalidaNaoQuebra() {
        assertEquals(0, Chefe.faseDe(10, 0, 3));
        assertEquals(0, Chefe.faseDe(10, -5, 3));
    }
}
