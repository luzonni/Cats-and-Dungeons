package com.retronova.game.hud;

/**
 * Quanto o HUD pode crescer sem que as peças se encostem ou saiam da tela.
 *
 * O DEFEITO. O HUD é desenhado em posições fixas — vida à esquerda, relógio no
 * centro, XP à direita, hotbar e Fúria embaixo —, e "HUD size" (2 a 6) e
 * "Margin" (0 a 40) multiplicavam e empurravam tudo sem olhar para a janela. Em
 * 1280x720 com HUD 4, o bloco da vida (136 px de arte vezes 4) passava por baixo
 * do relógio, e o número da vida ficava coberto.
 *
 * A REGRA. A escala que vale é a menor entre a escolhida e a maior que cabe:
 *
 *   - na largura, o bloco da esquerda, um vão e a metade do relógio cabem na
 *     metade da tela (o lado direito é menor e cabe sempre que este cabe);
 *   - na altura, a faixa de cima (retrato e barra de chefe) e a de baixo (hotbar e
 *     Fúria) ocupam no máximo metade da tela: a outra metade é da arena.
 *
 * Calculada a cada leitura a partir do tamanho da imagem do jogo, ela se ajusta
 * sozinha ao trocar a resolução ou redimensionar a janela, e a escolha gravada
 * não é perdida: numa tela maior ela volta a valer.
 *
 * As medidas são em pixels de arte (escala 1) e vêm dos sprites de ui/.
 */
public final class LayoutDoHud {

    private LayoutDoHud() {
    }

    // ----------------------------------------------------------- ajuste fino

    /** Retrato (16x2), frasco (16x2) e barra de vida (36x2): o retrato é desenhado no dobro. */
    public static final int BLOCO_ESQUERDO = 136;
    /** Placa do relógio (clock.png), centrada no topo. */
    public static final int RELOGIO = 80;
    /** Vão mínimo entre o bloco da esquerda e o relógio. */
    public static final int VAO = 6;
    /** Faixa de cima: retrato (32) e a barra de chefe logo abaixo do relógio. */
    public static final int FAIXA_DE_CIMA = 46;
    /** Faixa de baixo: hotbar com as orelhas e a barra da Fúria (34 acima da margem). */
    public static final int FAIXA_DE_BAIXO = 38;
    /** Fração da altura que o HUD pode ocupar, somadas as duas faixas. */
    public static final double FRACAO_DA_ALTURA = 0.5;

    /**
     * A maior escala do HUD que cabe numa imagem deste tamanho, nunca menor que 1.
     *
     * @param largura largura da imagem do jogo, em pixels
     * @param altura  altura da imagem do jogo, em pixels
     * @param margem  a margem da interface, em pixels
     */
    public static int escalaMaxima(int largura, int altura, int margem) {
        if (largura <= 0 || altura <= 0) {
            return Integer.MAX_VALUE;   // janela ainda sem tamanho: não limita nada
        }
        double pelaLargura = (largura / 2.0 - margem) / (BLOCO_ESQUERDO + VAO + RELOGIO / 2.0);
        double pelaAltura = (altura * FRACAO_DA_ALTURA - 2.0 * margem) / (FAIXA_DE_CIMA + FAIXA_DE_BAIXO);
        return Math.max(1, (int) Math.floor(Math.min(pelaLargura, pelaAltura)));
    }
}
