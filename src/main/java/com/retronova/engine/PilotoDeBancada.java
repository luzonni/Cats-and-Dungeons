package com.retronova.engine;

import com.retronova.engine.inputs.keyboard.KeyBoard;

/**
 * Um jogador falso para a bancada: anda pelo campo trocando de direção e dá dash
 * de tempos em tempos. Ligado com -Dcd.piloto=true, junto de -Dcd.bench.
 *
 * Existe para medir o caso que se queixa de travar — horda grande e o gato
 * correndo e dando dash, com a câmera varrendo o campo — sem ninguém precisar
 * jogar enquanto o medidor (-Dcd.perf) e o Flight Recorder gravam. Parado, o gato
 * só vê a mesma meia dúzia de bichos, e a medição não reproduz a queixa.
 *
 * Aperta as teclas pela mesma porta do controle ({@link KeyBoard#pad}), então o
 * jogo lê exatamente o que leria de um jogador.
 */
public final class PilotoDeBancada {

    /** As oito direções, como pares de teclas seguradas. */
    private static final String[][] DIRECOES = {
            {"W"}, {"W", "D"}, {"D"}, {"S", "D"}, {"S"}, {"S", "A"}, {"A"}, {"W", "A"}};
    private static final String[] TODAS = {"W", "A", "S", "D"};

    private PilotoDeBancada() {
    }

    public static void iniciar() {
        Thread t = new Thread(PilotoDeBancada::pilotar, "Piloto de bancada");
        t.setDaemon(true);
        t.start();
    }

    private static void pilotar() {
        int direcao = 0;
        long ate = 0;
        try {
            Thread.sleep(2000);                       // deixa o gato chegar
            while (true) {
                long agora = System.currentTimeMillis();
                if (agora >= ate) {
                    // Gira 3 posições: zigue-zague que cruza o campo em vez de voltar.
                    direcao = (direcao + 3) % DIRECOES.length;
                    for (String k : TODAS) {
                        KeyBoard.pad(k, false);
                    }
                    for (String k : DIRECOES[direcao]) {
                        KeyBoard.pad(k, true);
                    }
                    ate = agora + 900;
                }
                KeyBoard.pad("Space", true);          // o jogo consome no tick: um toque
                // Cartas de nivel e escolha de elemento pausam a partida ate alguem
                // escolher. As setas ja vem do A/D acima; o Enter confirma, e o jogo
                // segue em vez de a medicao ficar congelada numa tela parada.
                KeyBoard.pad("Enter", true);
                Thread.sleep(800);
            }
        } catch (InterruptedException fim) {
            Thread.currentThread().interrupt();
        }
    }
}
