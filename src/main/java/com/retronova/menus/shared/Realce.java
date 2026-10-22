package com.retronova.menus.shared;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Palette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * O realce de hover e de foco do jogo inteiro, num lugar só.
 *
 * O PADRÃO É O DOS BOTÕES DA TELA INICIAL: o elemento cresce um pouco a partir do
 * centro, ganha um brilho suave por trás e clareia. Nada de contorno. Antes cada
 * parte do jogo inventava o seu — anel grosso nos botões focados, moldura branca
 * nas casas do inventário e da loja, borda nas abas, o item que ENCOLHIA ao passar
 * o mouse —, e o resultado era um jogo com quatro gramáticas para "você está
 * aqui". Quem desenha hover ou foco chama daqui, e as telas não divergem de novo.
 *
 * O FOCO DO TECLADO E DO CONTROLE É O MESMO REALCE, mais um marcador. A Xbox
 * Accessibility Guideline 113 pede que o foco seja sempre visível e avisa que um
 * brilho sutil sozinho pode passar despercebido para quem enxerga pouco; ela
 * recomenda combinar técnicas (elemento maior, preenchimento, cor). O foco aqui
 * soma três: o mesmo crescimento e brilho do hover, o estado claro do elemento e
 * duas setas que respiram dos lados. A WCAG 2.4.13 mede a troca entre focado e não
 * focado em pelo menos 3:1 de contraste; o estado claro mais as setas em ACCENT
 * sobre o fundo escuro passam disso com folga — sem precisar de contorno grosso.
 *
 * Todos os números de ajuste fino estão no topo.
 */
public final class Realce {

    private Realce() {
    }

    // ----------------------------------------------------------- ajuste fino

    /** Quanto o realce anda por tick, de 0 a 1: 0,2 acende em 5 ticks (~0,08 s). */
    public static final float VELOCIDADE = 0.2f;
    /** Crescimento máximo, em pixels nativos, de cada lado (vertical: metade). */
    public static final int CRESCIMENTO = 2;
    /** Camadas do brilho e o quanto ele se espalha por camada, em pixels nativos. */
    public static final int CAMADAS = 4;
    /** Opacidade da camada mais interna do brilho (0-255). */
    public static final int ALFA = 26;
    /** Clareamento por cima do elemento realçado, para os que não têm estado claro (0-255). */
    public static final int CLAREAMENTO = 34;
    /** Tamanho da seta do marcador de foco e a distância dela ao elemento, em pixels nativos. */
    public static final int SETA = 4, AFASTAMENTO = 3;
    /** Quanto a seta respira para os lados, em pixels nativos, e em quantos segundos. */
    public static final double RESPIRO = 1.0, PERIODO_DO_RESPIRO = 0.9;

    // ----------------------------------------------------------- estado

    /**
     * O realce de um elemento, animado: sobe e desce em rampa, em vez de ligar e
     * desligar. O salto entre dois tamanhos lê como falha de desenho; a rampa curta
     * é o que faz o elemento parecer responder.
     */
    public static final class Estado {
        private float valor, alvo;

        /** Chamado todo tick. */
        public void tick(boolean aceso) {
            alvo = aceso ? 1f : 0f;
            if (valor != alvo) {
                valor += Math.signum(alvo - valor) * VELOCIDADE;
                valor = Math.max(0f, Math.min(1f, valor));
            }
        }

        /** O valor interpolado entre ticks, para a rampa fluir acima de 60 FPS. */
        public float noQuadro() {
            if (valor == alvo) {
                return valor;
            }
            float r = valor + Math.signum(alvo - valor) * VELOCIDADE * Engine.alpha();
            return Math.max(0f, Math.min(1f, r));
        }
    }

    // ----------------------------------------------------------- desenho

    /**
     * A folga mínima entre dois elementos focáveis lado a lado (Yes/No, Try
     * Again/Main Menu), em pixels de tela: o bastante para as setas do foco de um
     * não encostarem no outro e parecerem dele.
     */
    public static int folgaEntreVizinhos(int s) {
        return 2 * (SETA + AFASTAMENTO + 2) * s;
    }

    /** O retângulo crescido pelo realce, a partir do centro. */
    public static Rectangle crescer(Rectangle r, float intensidade, int s) {
        if (intensidade <= 0f) {
            return r;
        }
        int cresce = Math.round(intensidade * s * CRESCIMENTO);
        return new Rectangle(r.x - cresce, r.y - cresce / 2, r.width + cresce * 2, r.height + cresce);
    }

    /** O brilho por trás, na cor de destaque. */
    public static void brilho(Graphics2D g, Rectangle r, float intensidade, int s) {
        brilho(g, r, intensidade, s, Palette.ACCENT);
    }

    /**
     * O brilho por trás do elemento: retângulos concêntricos com opacidade
     * decrescente, e não um gradiente — o resultado é o mesmo nesta escala e não
     * custa criar uma pintura por quadro para cada elemento da tela.
     */
    public static void brilho(Graphics2D g, Rectangle r, float intensidade, int s, Color cor) {
        if (intensidade <= 0.02f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        for (int i = CAMADAS; i >= 1; i--) {
            int margem = i * s;
            int alfa = (int) (ALFA * intensidade * (1f - (i - 1) / (float) CAMADAS));
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), Math.max(0, alfa)));
            g2.fillRect(r.x - margem, r.y - margem, r.width + margem * 2, r.height + margem * 2);
        }
        g2.dispose();
    }

    /**
     * O mesmo brilho, para elementos de cantos arredondados (as cartas): mesmas
     * camadas e mesma opacidade, só sem as quinas retas aparecendo por fora da
     * curva. A cor é a do elemento, como o brilho do gato na seleção.
     */
    public static void brilhoArredondado(Graphics2D g, Rectangle r, float intensidade, int s, Color cor, int arco) {
        if (intensidade <= 0.02f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        for (int i = CAMADAS; i >= 1; i--) {
            int margem = i * s;
            int alfa = (int) (ALFA * 2 * intensidade * (1f - (i - 1) / (float) CAMADAS));
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), Math.max(0, Math.min(255, alfa))));
            g2.fillRoundRect(r.x - margem, r.y - margem, r.width + margem * 2, r.height + margem * 2,
                    arco + margem * 2, arco + margem * 2);
        }
        g2.dispose();
    }

    /** Clareia o elemento por cima: o "estado claro" de quem não tem sprite próprio para ele. */
    public static void clarear(Graphics2D g, Rectangle r, float intensidade) {
        if (intensidade <= 0.02f) {
            return;
        }
        g.setColor(new Color(255, 255, 255, (int) (CLAREAMENTO * intensidade)));
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    /**
     * O hover completo para elementos sem sprite de estado (casas, linhas, abas):
     * brilho por trás e clareamento por cima. Retorna o retângulo crescido, para
     * quem quiser desenhar o conteúdo nele.
     */
    public static Rectangle realcar(Graphics2D g, Rectangle r, float intensidade, int s) {
        Rectangle maior = crescer(r, intensidade, s);
        brilho(g, maior, intensidade, s);
        return maior;
    }

    /**
     * As duas setas do foco, uma de cada lado, apontando para o elemento.
     *
     * Elas respiram para os lados devagar: um marcador parado vira parte do
     * desenho e o olho para de vê-lo; um que se move pouco continua dizendo "é
     * aqui" sem disputar atenção com o resto da tela.
     */
    public static void marcadorDeFoco(Graphics2D g, Rectangle r, int s) {
        marcadorDeFoco(g, r, s, Palette.TEXT);
    }

    public static void marcadorDeFoco(Graphics2D g, Rectangle r, int s, Color cor) {
        double t = System.nanoTime() / 1e9;
        int respiro = (int) Math.round((0.5 + 0.5 * Math.sin(t * 2 * Math.PI / PERIODO_DO_RESPIRO)) * RESPIRO * s);
        int lado = SETA * s;
        int cy = r.y + r.height / 2;
        int esq = r.x - (AFASTAMENTO * s) - respiro;
        int dir = r.x + r.width + (AFASTAMENTO * s) + respiro;
        Graphics2D g2 = (Graphics2D) g.create();
        // Sombra de um pixel embaixo, para a seta ler sobre fundo claro também.
        for (int camada = 0; camada < 2; camada++) {
            g2.setColor(camada == 0 ? new Color(0, 0, 0, 140) : cor);
            int d = camada == 0 ? s : 0;
            for (int i = 0; i < lado; i += Math.max(1, s)) {
                int altura = (lado - i) * 2 - Math.max(1, s);
                // seta da esquerda aponta para a direita (para o elemento)
                g2.fillRect(esq - lado + i, cy - altura / 2 + d, Math.max(1, s), Math.max(1, altura));
                // seta da direita aponta para a esquerda
                g2.fillRect(dir + lado - i - Math.max(1, s), cy - altura / 2 + d, Math.max(1, s), Math.max(1, altura));
            }
        }
        g2.dispose();
    }
}
