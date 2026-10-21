package com.retronova.engine.inputs;

/**
 * Quem está no comando agora: o mouse, o teclado ou o controle.
 *
 * DOIS MODOS PARA O JOGADOR, mouse e teclado de um lado, controle do outro. Mexeu
 * o mouse, o jogo é de mouse; apertou qualquer coisa no controle, o jogo é de
 * controle — e no modo controle o mouse deixa de existir para as telas (ver
 * Mouse.getX). Sem isso, um ponteiro esquecido em cima de uma carta roubava o
 * foco de volta a cada tick, e o D-pad não conseguia sair dela: o jogador tinha
 * de tirar o mouse da janela para o controle funcionar.
 *
 * O teclado conta à parte só para o FOCO: as setas também andam pelas grades
 * (inventário, loja) e acendem o realce, mas não escondem o mouse, porque teclado
 * e mouse são usados juntos.
 *
 * Decide quem mexeu por último, e não quem está ligado: mouse e controle podem
 * estar os dois plugados — o que importa é a mão do jogador.
 */
public final class Entrada {

    private enum Modo { MOUSE, TECLADO, CONTROLE }

    private static volatile Modo modo = Modo.MOUSE;

    private Entrada() {
    }

    /** O mouse se mexeu ou clicou. */
    public static void mouse() {
        modo = Modo.MOUSE;
    }

    /** Uma tecla do teclado foi apertada. */
    public static void tecla() {
        modo = Modo.TECLADO;
    }

    /** O controle foi usado: botão, gatilho ou analógico. */
    public static void controle() {
        modo = Modo.CONTROLE;
    }

    /** O controle está no comando: o mouse some das telas e da janela. */
    public static boolean usandoControle() {
        return modo == Modo.CONTROLE;
    }

    /**
     * O mouse está no comando: só então "estar em cima" de algo conta como hover.
     *
     * Com o teclado no comando, um ponteiro PARADO em cima de um botão roubava o
     * foco das setas a cada tick — o jogador apertava para baixo e o foco voltava
     * para onde o mouse tinha ficado. Mexer o mouse devolve o comando a ele.
     */
    public static boolean porMouse() {
        return modo == Modo.MOUSE;
    }

    /** As setas ou o D-pad estão no comando: as telas de grade mostram e usam o foco. */
    public static boolean porFoco() {
        return modo != Modo.MOUSE;
    }
}
