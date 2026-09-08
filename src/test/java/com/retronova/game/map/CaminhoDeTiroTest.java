package com.retronova.game.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da linha de tiro.
 *
 * A regra que estes testes guardam e a que estava faltando e travava o jogo: a
 * CAIXA DO ALVO NAO BLOQUEIA O TIRO CONTRA ELE. Enquanto o raio ia ate o centro
 * do inimigo amostrando tudo pelo caminho, um bicho encostado na parede era
 * declarado inalcancavel — o bloco atras dele contava como obstaculo — e a arma
 * ficava mirando para sempre sem disparar. E a mesma regra que os roguelikes
 * usam para campo de visao: a casa do alvo nao esconde o alvo.
 *
 * O mapa aqui e sintetico, entao nada disto depende de arena carregada.
 */
class CaminhoDeTiroTest {

    /** Nenhum bloco em lugar nenhum. */
    private static final GameMap.Solidez ABERTO = (x, y) -> false;

    private static final double LARGURA = 24;

    /** Lado de um tile em pixels de tela, e o passo de amostragem que dai sai. */
    private static final int TILE = 64;
    private static final double PASSO = TILE / 4d;

    /** Um inimigo de um tile, com o canto superior esquerdo aqui. */
    private static Rectangle inimigoEm(int x, int y) {
        return new Rectangle(x, y, TILE, TILE);
    }

    @Test
    @DisplayName("campo aberto tem caminho")
    void aberto() {
        assertTrue(GameMap.caminhoDeTiro(0, 0, inimigoEm(400, 0), LARGURA, PASSO, ABERTO));
    }

    @Test
    @DisplayName("parede no meio bloqueia")
    void paredeNoMeio() {
        GameMap.Solidez muro = (x, y) -> x > 190 && x < 210;
        assertFalse(GameMap.caminhoDeTiro(0, 0, inimigoEm(400, 0), LARGURA, PASSO, muro));
    }

    @Test
    @DisplayName("inimigo ENCOSTADO na parede continua alcancavel")
    void alvoColadoNaParede() {
        // O bloco comeca onde o inimigo termina: e o caso das plantas coladas no
        // muro, que ficavam imunes a tudo que nao fosse corpo a corpo.
        Rectangle alvo = inimigoEm(400, 0);
        GameMap.Solidez muroAtras = (x, y) -> x >= alvo.getMaxX();
        assertTrue(GameMap.caminhoDeTiro(0, 0, alvo, LARGURA, PASSO, muroAtras));
    }

    @Test
    @DisplayName("inimigo DENTRO da parede continua alcancavel")
    void alvoDentroDaParede() {
        // Mais extremo: o proprio tile do inimigo e solido. O tiro ainda tem de
        // sair, porque o que bloqueia um tiro e o que esta ANTES do alvo.
        Rectangle alvo = inimigoEm(400, 0);
        GameMap.Solidez muroNoAlvo = (x, y) -> x >= alvo.getX();
        assertTrue(GameMap.caminhoDeTiro(0, 0, alvo, LARGURA, PASSO, muroNoAlvo));
    }

    @Test
    @DisplayName("a fresta estreita demais para o projetil bloqueia")
    void frestaEstreita() {
        // Uma fenda de dez pixels numa parede: a linha de espessura zero passa,
        // o projetil de vinte e quatro nao. Dizer que ha caminho aqui faz a arma
        // atirar contra o vao e o tiro morrer no primeiro tick.
        GameMap.Solidez comFresta = (x, y) -> x > 190 && x < 210 && Math.abs(y) > 5;
        assertFalse(GameMap.caminhoDeTiro(0, 0, inimigoEm(400, 0), LARGURA, PASSO, comFresta));
        // A mesma fenda, larga o bastante, deixa passar.
        GameMap.Solidez frestaLarga = (x, y) -> x > 190 && x < 210 && Math.abs(y) > 40;
        assertTrue(GameMap.caminhoDeTiro(0, 0, inimigoEm(400, 0), LARGURA, PASSO, frestaLarga));
    }
}
