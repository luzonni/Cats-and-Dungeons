package com.retronova.game.objects.entities.utilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.geom.Line2D;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da colisao de projetil.
 *
 * Guardam as duas decisoes que consertaram o sistema, e que se desfazem com
 * facilidade numa refatoracao futura:
 *
 *  1. A CAIXA E MENOR QUE O QUADRO. Enquanto ela teve os dezesseis pixels do
 *     quadro — um tile inteiro na tela — todo tiro nascia ja encostando em quem
 *     estivesse ao lado e raspava a parede de qualquer corredor.
 *
 *  2. O TESTE E NO CAMINHO, NAO NA POSICAO. Caixa pequena e velocidade alta
 *     tunelam: entre dois quadros o projetil aparece antes e depois do inimigo,
 *     sem nunca estar em cima dele.
 *
 * Tudo aqui e geometria pura, entao nao depende de mapa, janela nem estado
 * global do jogo.
 */
class ProjetilTest {

    /** Lado do quadro de um sprite, em pixels de arte. */
    private static final double QUADRO = 16;

    /** Lado do projetil em pixels de tela, na escala 4 do jogo. */
    private static final double LARGURA = 24;

    private static Line2D percurso(double x1, double y1, double x2, double y2) {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    @Test
    @DisplayName("a caixa de colisao e menor que o quadro do desenho")
    void caixaMenorQueODesenho() {
        assertTrue(Projetil.LADO < QUADRO,
                "caixa do tamanho do quadro faz o tiro nascer colidindo e morrer na parede");
    }

    @Test
    @DisplayName("acerta o inimigo que o tiro atravessa entre dois quadros")
    void naoTunela() {
        Rectangle inimigo = new Rectangle(200, 0, 64, 64);
        // Um tick que salta de antes para depois do inimigo. Em nenhum dos dois
        // instantes o projetil esta em cima dele.
        assertFalse(inimigo.contains(180, 32), "premissa: comeca fora");
        assertFalse(inimigo.contains(300, 32), "premissa: termina fora");
        assertTrue(Projetil.noCaminho(inimigo, percurso(180, 32, 300, 32)));
    }

    @Test
    @DisplayName("nao acerta quem esta fora do caminho")
    void passaLonge() {
        Rectangle inimigo = new Rectangle(200, 0, 64, 64);
        assertFalse(Projetil.noCaminho(inimigo, percurso(180, 300, 300, 300)));
    }

    @Test
    @DisplayName("acerta quem esta parado em cima do alvo")
    void percursoDeComprimentoZero() {
        Rectangle inimigo = new Rectangle(200, 0, 64, 64);
        assertTrue(Projetil.noCaminho(inimigo, percurso(210, 10, 210, 10)));
        assertFalse(Projetil.noCaminho(inimigo, percurso(10, 10, 10, 10)));
    }

    @Test
    @DisplayName("a folga do projetil conta como acerto de raspao")
    void folgaDeMinkowski() {
        Rectangle inimigo = new Rectangle(200, 0, 64, 64);
        // Um tiro que passa DOIS pixels acima da caixa. Na tela o projetil, que
        // tem vinte e quatro pixels de lado, cobre o inimigo; com a linha crua do
        // meio isso contava como erro.
        Line2D raspao = percurso(180, -2, 300, -2);
        assertFalse(inimigo.intersectsLine(raspao), "premissa: a linha crua erra");
        assertTrue(Projetil.noCaminho(Projetil.comFolga(inimigo, LARGURA), raspao));
    }

    @Test
    @DisplayName("a folga nao alcanca quem esta longe do caminho")
    void folgaNaoEImas() {
        Rectangle inimigo = new Rectangle(200, 0, 64, 64);
        Line2D longe = percurso(180, -200, 300, -200);
        assertFalse(Projetil.noCaminho(Projetil.comFolga(inimigo, LARGURA), longe));
    }

    @Test
    @DisplayName("acerta o primeiro quando o caminho cruza dois")
    void oPrimeiroDoCaminho() {
        Rectangle perto = new Rectangle(100, 0, 64, 64);
        Rectangle longe = new Rectangle(300, 0, 64, 64);
        Line2D tiro = percurso(0, 32, 400, 32);
        assertTrue(Projetil.noCaminho(perto, tiro));
        assertTrue(Projetil.noCaminho(longe, tiro));
        // Quem decide entre os dois e a distancia ate a origem, em atingido().
        assertTrue(Math.hypot(100 - 0, 32 - 32) < Math.hypot(300 - 0, 32 - 32));
    }
}
