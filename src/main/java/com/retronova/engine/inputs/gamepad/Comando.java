package com.retronova.engine.inputs.gamepad;

/**
 * As peças do controle que o jogador vê, para a tela de Controles e as dicas.
 *
 * NÃO É O {@link Gamepad.Botao}. O Botao é o que o XInput lê, bit a bit: as quatro
 * direções do D-pad são quatro botões, e os analógicos nem são botões. Aqui é o
 * desenho: um D-pad, um analógico de cada lado, e o clique de cada analógico à
 * parte. A tela de Controles liga uma ação a uma destas peças, nunca a um bit.
 */
public enum Comando {
    LT, LB, RT, RB,
    ANALOGICO_ESQ, ANALOGICO_DIR, L3, R3,
    DPAD, VIEW, MENU,
    A, B, X, Y;

    /** A peça em que fica um botão do XInput. */
    public static Comando de(Gamepad.Botao botao) {
        return switch (botao) {
            case CIMA, BAIXO, ESQUERDA, DIREITA -> DPAD;
            case START -> MENU;
            case VIEW -> VIEW;
            case L3 -> L3;
            case R3 -> R3;
            case LB -> LB;
            case RB -> RB;
            case LT -> LT;
            case RT -> RT;
            case A -> A;
            case B -> B;
            case X -> X;
            case Y -> Y;
        };
    }
}
