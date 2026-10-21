package com.retronova.game.interfaces;

import com.retronova.engine.inputs.keyboard.KeyBoard;

import java.util.function.IntPredicate;

/**
 * Foco andando por uma grade de casas, com as setas ou o D-pad.
 *
 * As telas de grade (inventário, loja) eram só de mouse, e o controle não tem
 * mouse. Em vez de cada tela inventar o próprio "para onde vai o foco", a regra
 * mora aqui, e as casas DESATIVADAS são puladas — a mochila começa com menos
 * casas do que o painel desenha, e o foco não pode parar numa casa que não existe.
 */
final class Grade {

    private Grade() {
    }

    /**
     * Lê as setas deste tick e devolve o foco novo, ou o mesmo se nenhuma seta
     * foi apertada ou não há para onde ir.
     */
    static int navegar(int foco, int colunas, int total, IntPredicate ativa) {
        if (KeyBoard.KeyPressed("Left")) {
            return mover(foco, colunas, total, ativa, -1, 0);
        }
        if (KeyBoard.KeyPressed("Right")) {
            return mover(foco, colunas, total, ativa, 1, 0);
        }
        if (KeyBoard.KeyPressed("Up")) {
            return mover(foco, colunas, total, ativa, 0, -1);
        }
        if (KeyBoard.KeyPressed("Down")) {
            return mover(foco, colunas, total, ativa, 0, 1);
        }
        return foco;
    }

    /**
     * Um passo na direção (dx, dy).
     *
     * Na horizontal, anda até a próxima casa ativa da mesma fileira. Na vertical,
     * vai para a fileira seguinte que tiver alguma casa ativa e escolhe a coluna
     * mais próxima da atual: descer de uma mochila de sete casas para a hotbar não
     * pode prender o foco só porque a coluna de baixo não existe.
     */
    static int mover(int foco, int colunas, int total, IntPredicate ativa, int dx, int dy) {
        int fileiras = (total + colunas - 1) / colunas;
        int linha = foco / colunas, coluna = foco % colunas;
        if (dx != 0) {
            for (int c = coluna + dx; c >= 0 && c < colunas; c += dx) {
                int i = linha * colunas + c;
                if (i < total && ativa.test(i)) {
                    return i;
                }
            }
            return foco;
        }
        for (int l = linha + dy; l >= 0 && l < fileiras; l += dy) {
            for (int distancia = 0; distancia < colunas; distancia++) {
                for (int c : new int[] {coluna - distancia, coluna + distancia}) {
                    int i = l * colunas + c;
                    if (c >= 0 && c < colunas && i < total && ativa.test(i)) {
                        return i;
                    }
                }
            }
        }
        return foco;
    }
}
