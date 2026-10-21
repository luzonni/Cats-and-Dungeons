package com.retronova.game.interfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes do foco andando pela grade do inventário e da loja.
 *
 * A grade do inventário é a que tem armadilha: 5 colunas, mochila nas três
 * primeiras fileiras e hotbar na quarta, com casas desativadas no fim da mochila
 * e da hotbar. O foco nunca pode parar numa delas, nem ficar preso por causa delas.
 */
class GradeTest {

    private static final int COLUNAS = 5, TOTAL = 20;

    /** Mochila com 'mochila' casas (0-14) e hotbar com 'hotbar' casas (15-19). */
    private static IntPredicate inventario(int mochila, int hotbar) {
        return i -> i < 15 ? i < mochila : i - 15 < hotbar;
    }

    private static int mover(int foco, IntPredicate ativa, int dx, int dy) {
        return Grade.mover(foco, COLUNAS, TOTAL, ativa, dx, dy);
    }

    @Test
    @DisplayName("anda casa a casa numa grade cheia")
    void grade() {
        IntPredicate cheia = inventario(15, 5);
        assertEquals(1, mover(0, cheia, 1, 0));
        assertEquals(5, mover(0, cheia, 0, 1));
        assertEquals(15, mover(10, cheia, 0, 1), "da última fileira da mochila desce para a hotbar");
    }

    @Test
    @DisplayName("na borda, fica onde está")
    void borda() {
        IntPredicate cheia = inventario(15, 5);
        assertEquals(0, mover(0, cheia, -1, 0));
        assertEquals(0, mover(0, cheia, 0, -1));
        assertEquals(19, mover(19, cheia, 1, 0));
        assertEquals(19, mover(19, cheia, 0, 1));
    }

    @Test
    @DisplayName("descer para uma coluna que não existe escolhe a mais próxima")
    void colunaMaisProxima() {
        // Mochila de 7 casas: a segunda fileira tem só as colunas 0 e 1.
        IntPredicate inv = inventario(7, 3);
        assertEquals(6, mover(4, inv, 0, 1), "da coluna 4 vai para a 1, a mais perto que existe");
        assertEquals(16, mover(6, inv, 0, 1), "pula a terceira fileira, vazia, até a hotbar");
    }

    @Test
    @DisplayName("na horizontal, não entra em casa desativada")
    void horizontal() {
        IntPredicate inv = inventario(7, 3);
        assertEquals(6, mover(6, inv, 1, 0), "a casa 7 não existe: fica na 6");
        assertEquals(17, mover(16, inv, 1, 0));
        assertEquals(17, mover(17, inv, 1, 0));
    }
}
