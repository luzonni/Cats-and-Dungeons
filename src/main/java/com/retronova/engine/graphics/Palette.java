package com.retronova.engine.graphics;

import java.awt.Color;

/**
 * Paleta única do jogo, medida na arte oficial.
 *
 * Estas cores já eram usadas soltas pelas interfaces in-game (Inter, Store,
 * Inventory) e pelos sprites de UI. Os menus usavam um cinza-azulado próprio,
 * de outro sistema de design; centralizar aqui existe para que isso não
 * volte a acontecer.
 *
 * DE ONDE ELAS VÊM. O jogo nasceu com uma paleta vermelha e azul-marinho,
 * escolhida antes de a arte oficial existir. Ela não conversava com a ilustração
 * do título — que é fria, escura e acinzentada — nem com os personagens, e a
 * interface sempre pareceu colada por cima do jogo em vez de fazer parte dele.
 *
 * COMO ELAS FORAM MEDIDAS, E POR QUE ISSO IMPORTA. A primeira versão pegou as
 * famílias de cor mais frequentes de {@code icons/Gato.png}, e saiu escura demais:
 * o que ocupa área naquela ilustração é o fundo noturno, então a escada inteira
 * da interface caiu no poço e pulou as faixas médias.
 *
 * A medição certa é por FAIXA DE LUMINOSIDADE. A arte distribui cerca de 20% dos
 * pixels em cada faixa até 45% de valor, mais 11% entre 45 e 55%, e ainda 12%
 * acima disso — ela é uma cena de meio-tom com fundo escuro, não uma cena preta.
 * Cada cor abaixo é a mais comum DENTRO da sua faixa, e não a mais comum da
 * imagem toda; assim a interface herda a distribuição da arte e não só o porão
 * dela.
 *
 * A medição e a conversão dos sprites estão em {@code tools/GenTema.java}, que é
 * também onde ficam os originais preservados.
 */
public final class Palette {

    /**
     * Fundo cheio de tela e base do véu.
     *
     * O tom mais fundo da arte, o do chão sob os personagens.
     */
    public static final Color DARKEST = new Color(0x180E1B);

    /** Contorno e sombra dura de texto. */
    public static final Color OUTLINE = new Color(0x211A2B);
    /** Sombra / preenchimento de elemento secundário. */
    public static final Color DEEP = new Color(0x3E4160);
    /** Preenchimento principal: o corpo de painel e de botão. */
    public static final Color MAIN = new Color(0x525779);
    /** Borda e sombreado claro. */
    public static final Color LIGHT = new Color(0x7E849C);
    /** Texto sobre fundo escuro. O branco do título. */
    public static final Color TEXT = new Color(0xFCFCFC);

    /**
     * Foco, seleção e brilho de passagem do mouse.
     *
     * É a luz da lua da arte — a fonte de luz da própria ilustração. Houve uma
     * tentativa com o ouro da coroa, que é o único calor da cena: isolado num
     * anel de foco ele não lê como parte da arte, lê como amarelo de alerta sobre
     * ardósia. Um realce frio destaca igual e continua dentro da mesma família.
     */
    public static final Color ACCENT = new Color(0xABB1C1);

    /**
     * Aviso: dinheiro insuficiente, ação recusada.
     *
     * É o vermelho das bandeiras da arte, na parte delas que pega a luz da lua —
     * o tom mais claro que a ilustração tem nessa família. Continua sendo a única
     * cor quente da interface, e é isso que faz um aviso ler como exceção: tudo
     * ao redor é frio.
     */
    public static final Color WARN = new Color(0x7F4750);

    /** Véu escuro sobre a cena, para sobreposições. */
    public static final Color VEIL = new Color(0x18, 0x0E, 0x1B, 200);

    private Palette() { }
}
