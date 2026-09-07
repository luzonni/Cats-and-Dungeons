package com.retronova.game.objects.entities.furniture;

/**
 * Passa os quadros de uma chama.
 *
 * Vale para tocha e braseiro, que animam do mesmo jeito e só mudam de sprite.
 *
 * A fase inicial vem da posição da peça no mapa. Sem isso todas as chamas da
 * sala trocam de quadro no mesmo tick e o resultado lê como um efeito ligando e
 * desligando, não como fogo — o olho percebe o sincronismo na hora.
 */
class Fogo {

    /** Cinco quadros a cada sete ticks dá algo perto de nove trocas por segundo. */
    private static final int TICKS_POR_QUADRO = 7;

    private final Furniture dono;
    private int contador;

    Fogo(Furniture dono, double x, double y) {
        this.dono = dono;
        this.contador = (int) Math.abs(x * 3 + y * 5) % (TICKS_POR_QUADRO * 5);
    }

    void tick() {
        contador++;
        if (contador % TICKS_POR_QUADRO == 0) {
            dono.proximoQuadro();
        }
    }
}
