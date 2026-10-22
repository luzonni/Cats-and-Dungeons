package com.retronova.game.hud;

import java.awt.Rectangle;

/**
 * Onde cada peça do HUD fica, e até onde o HUD pode crescer.
 *
 * ---------------------------------------------------------------------------
 * ÂNCORAS, E NÃO POSIÇÕES SOLTAS
 *
 * Cada peça tem uma ÂNCORA (um canto, o meio de uma borda), um DESLOCAMENTO a
 * partir dela e um TAMANHO, os dois em pixels de arte da resolução de referência
 * (ver EscalaDaInterface). A posição na tela é calculada daí a cada leitura, junto
 * com o tamanho, pela margem e pela escala em vigor:
 *
 *     topo-esquerda:  x = margem + dx·s           y = margem + dy·s
 *     topo-centro:    x = largura/2 − w·s/2       y = margem + dy·s
 *     topo-direita:   x = largura − margem − w·s  y = margem + dy·s
 *     base-centro:    x = largura/2 − w·s/2       y = altura − margem − (dy + h)·s
 *
 * Antes, cada classe do HUD fazia a própria conta com a margem e números
 * próprios, e nada sabia das outras: aumentar o HUD punha o relógio em cima da
 * vida, e a única defesa era um teto calculado à parte com as medidas copiadas.
 * Agora as medidas moram AQUI, quem desenha pede a caixa daqui, e o teto é
 * calculado sobre as mesmas caixas.
 *
 * ---------------------------------------------------------------------------
 * O TETO
 *
 * A escala do HUD que vale é a menor entre a escolhida e a maior em que (1) toda
 * peça cabe dentro da margem, (2) nenhuma peça cruza outra, e (3) as faixas de
 * cima e de baixo somam no máximo metade da altura — a outra metade é da arena.
 *
 * Peça nova: um item em {@link Peca}, com âncora, deslocamento e tamanho em
 * pixels de arte, e quem desenha usa {@link #caixa}. O teto e o teste
 * (LayoutDoHudTest) passam a cobri-la sozinhos.
 */
public final class LayoutDoHud {

    private LayoutDoHud() {
    }

    /** Onde a peça se prende à tela. */
    public enum Ancora {
        TOPO_ESQUERDA, TOPO_CENTRO, TOPO_DIREITA, BASE_CENTRO
    }

    /**
     * As peças do HUD, em pixels de arte (escala 1). As medidas vêm dos sprites de
     * ui/ e do desenho de cada classe.
     */
    public enum Peca {
        /** Retrato (16, desenhado em dobro), frasco (16, em dobro) e barra de vida (36, em dobro). */
        VIDA(Ancora.TOPO_ESQUERDA, 0, 136, 32),
        /** Placa do relógio (clock.png). */
        RELOGIO(Ancora.TOPO_CENTRO, 0, 80, 24),
        /** Círculo do XP: 15 de diâmetro mais o anel. */
        XP(Ancora.TOPO_DIREITA, 0, 17, 17),
        /** Nome do chefe e a barra embaixo dele, abaixo da vida (32) e do vão (4). */
        CHEFE(Ancora.TOPO_CENTRO, 36, 180, 16),
        /** Rótulo "FURY" e a barra da Fúria, acima da hotbar. */
        FURIA(Ancora.BASE_CENTRO, 29, 72, 14),
        /** As cinco casas (16) com as orelhas por cima (6). */
        HOTBAR(Ancora.BASE_CENTRO, 0, 80, 22);

        public final Ancora ancora;
        /** Deslocamento a partir da âncora, para dentro da tela. */
        public final int dy;
        public final int largura, altura;

        Peca(Ancora ancora, int dy, int largura, int altura) {
            this.ancora = ancora;
            this.dy = dy;
            this.largura = largura;
            this.altura = altura;
        }
    }

    // ----------------------------------------------------------- ajuste fino

    /** Fração da altura que as faixas do HUD podem ocupar, somadas. */
    public static final double FRACAO_DA_ALTURA = 0.5;
    /** Vão mínimo entre duas peças, em pixels de arte. */
    public static final int VAO = 4;
    /** Maior escala testada. */
    private static final int ESCALA_MAXIMA = 16;

    /** A caixa da peça agora: imagem do jogo, margem e escala do HUD em vigor. */
    public static Rectangle caixa(Peca p) {
        return caixa(p, com.retronova.engine.Engine.window.largura(), com.retronova.engine.Engine.window.altura(),
                com.retronova.engine.Configs.Margin(), com.retronova.engine.Configs.HudScale());
    }

    /** A caixa da peça na tela, em pixels, para esta tela, margem e escala. */
    public static Rectangle caixa(Peca p, int largura, int altura, int margem, int s) {
        int w = p.largura * s, h = p.altura * s;
        int x = switch (p.ancora) {
            case TOPO_ESQUERDA -> margem;
            case TOPO_CENTRO, BASE_CENTRO -> largura / 2 - w / 2;
            case TOPO_DIREITA -> largura - margem - w;
        };
        int y = p.ancora == Ancora.BASE_CENTRO ? altura - margem - (p.dy * s) - h : margem + p.dy * s;
        return new Rectangle(x, y, w, h);
    }

    /** A maior escala em que todas as peças cabem sem se cruzar, nunca menor que 1. */
    public static int escalaMaxima(int largura, int altura, int margem) {
        if (largura <= 0 || altura <= 0) {
            return Integer.MAX_VALUE;   // janela ainda sem tamanho: não limita nada
        }
        for (int s = ESCALA_MAXIMA; s > 1; s--) {
            if (cabe(largura, altura, margem, s)) {
                return s;
            }
        }
        return 1;
    }

    /** Todas as peças dentro da margem, sem se cruzar, e as faixas em até metade da altura. */
    public static boolean cabe(int largura, int altura, int margem, int s) {
        Peca[] pecas = Peca.values();
        Rectangle[] caixas = new Rectangle[pecas.length];
        Rectangle dentro = new Rectangle(margem, margem, largura - 2 * margem, altura - 2 * margem);
        int topo = 0, base = 0;
        for (int i = 0; i < pecas.length; i++) {
            caixas[i] = caixa(pecas[i], largura, altura, margem, s);
            if (!dentro.contains(caixas[i])) {
                return false;
            }
            int alcance = (pecas[i].dy + pecas[i].altura) * s;
            if (pecas[i].ancora == Ancora.BASE_CENTRO) {
                base = Math.max(base, alcance);
            } else {
                topo = Math.max(topo, alcance);
            }
        }
        if (topo + base + 2 * margem > altura * FRACAO_DA_ALTURA) {
            return false;
        }
        int vao = VAO * s;
        for (int i = 0; i < caixas.length; i++) {
            Rectangle a = new Rectangle(caixas[i].x - vao / 2, caixas[i].y - vao / 2,
                    caixas[i].width + vao, caixas[i].height + vao);
            for (int j = i + 1; j < caixas.length; j++) {
                if (a.intersects(caixas[j])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Desenha as caixas, as âncoras e a margem, para conferir o layout (F7, ver
     * Debugging): o retângulo de cada peça, um ponto na âncora e a linha da margem.
     */
    public static void desenharDepuracao(java.awt.Graphics2D g, int largura, int altura, int margem, int s) {
        g.setColor(new java.awt.Color(255, 255, 0, 160));
        g.drawRect(margem, margem, largura - 2 * margem - 1, altura - 2 * margem - 1);
        java.awt.Font f = new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12);
        g.setFont(f);
        for (Peca p : Peca.values()) {
            Rectangle r = caixa(p, largura, altura, margem, s);
            g.setColor(new java.awt.Color(0, 255, 255, 50));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(new java.awt.Color(0, 255, 255, 220));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            int ax = switch (p.ancora) {
                case TOPO_ESQUERDA -> margem;
                case TOPO_CENTRO, BASE_CENTRO -> largura / 2;
                case TOPO_DIREITA -> largura - margem;
            };
            int ay = p.ancora == Ancora.BASE_CENTRO ? altura - margem : margem;
            g.setColor(java.awt.Color.MAGENTA);
            g.fillRect(ax - 3, ay - 3, 6, 6);
            g.setColor(java.awt.Color.WHITE);
            g.drawString(p.name(), r.x + 2, r.y + 12);
        }
        g.setColor(java.awt.Color.WHITE);
        g.drawString("HUD s=" + s + "  margem=" + margem + "px  teto=" + escalaMaxima(largura, altura, margem),
                margem + 4, altura - margem - 4);
    }
}
