package com.retronova.engine;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Pede ao Windows um relógio de 1 ms enquanto o jogo roda.
 *
 * POR QUE ISTO EXISTE. O limitador de quadros espera dormindo, e no Windows o
 * {@code Thread.sleep(1)} dura o que o relógio do sistema deixar: com a resolução
 * padrão (15,6 ms) ele dormiu, medido nesta máquina, 8,9 ms em média e 19 ms no
 * pior caso; minutos depois, com outro programa tendo pedido 1 ms, dormiu 1,5 ms.
 * Desde o Windows 10 2004 a resolução é POR PROCESSO, e o Java não a pede sozinho
 * — então o mesmo jogo ficava liso ou aos trancos conforme o que mais estava
 * aberto. É o que fazem a SDL e a Unity: {@code timeBeginPeriod(1)} na abertura,
 * {@code timeEndPeriod(1)} no fim (documentação da winmm, "timeBeginPeriod").
 *
 * Fora do Windows não há o que fazer: o Linux e o macOS já dormem com precisão
 * de microssegundos. E se a winmm não carregar, o limitador continua funcionando
 * — ele mede o sono de verdade e gira no trecho final (ver Engine).
 */
final class RelogioDoSistema {

    private RelogioDoSistema() {
    }

    private interface WinMM extends Library {
        int timeBeginPeriod(int ms);

        int timeEndPeriod(int ms);
    }

    private static WinMM winmm;

    /** Pede 1 ms. Chamado uma vez, na abertura. */
    static void pedirPrecisao() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        try {
            winmm = Native.load("winmm", WinMM.class);
            int r = winmm.timeBeginPeriod(1);
            System.out.println("Timer: 1 ms" + (r == 0 ? "" : " (refused: " + r + ")"));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> winmm.timeEndPeriod(1), "timeEndPeriod"));
        } catch (Throwable semWinmm) {
            winmm = null;
            System.out.println("Timer: default (" + semWinmm.getClass().getSimpleName() + ")");
        }
    }
}
