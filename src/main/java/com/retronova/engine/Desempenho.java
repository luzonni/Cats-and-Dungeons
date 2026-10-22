package com.retronova.engine;

import java.util.Arrays;

/**
 * Medidor de ritmo de quadros, ligado com -Dcd.perf=true.
 *
 * "Parece travado" tem tres causas que se sentem igual e se consertam de jeitos
 * opostos: quadro CARO (o desenho nao cabe no tempo), quadro IRREGULAR (cabe,
 * mas sai em intervalos desiguais) e tick em RAJADA (a logica recupera atraso
 * aos solavancos). Este medidor separa as tres: imprime a cada dois segundos o
 * custo medio do tick e do desenho, e os intervalos entre quadros — media, o
 * pior um por cento e o maximo. Media boa com p99 ruim e irregularidade, nao
 * lentidao.
 */
public final class Desempenho {

    public static final boolean LIGADO = Boolean.getBoolean("cd.perf");

    private static final int MAX = 4096;
    private static final double[] intervalos = new double[MAX];
    private static int n;
    private static long ultimoQuadro;
    private static double somaRender, somaTick;
    private static int ticks, rajadas;
    private static long inicioJanela = System.nanoTime();
    private static long gcAntes, coletasAntes;

    private Desempenho() {
    }

    static void quadro(long custoRenderNs) {
        if (!LIGADO) {
            return;
        }
        long agora = System.nanoTime();
        if (ultimoQuadro != 0 && n < MAX) {
            intervalos[n++] = (agora - ultimoQuadro) / 1e6;
        }
        ultimoQuadro = agora;
        somaRender += custoRenderNs / 1e6;
        if (agora - inicioJanela >= 2_000_000_000L) {
            imprimir(agora);
        }
    }

    static void tick(long custoNs, int executadosNaVolta) {
        if (!LIGADO) {
            return;
        }
        somaTick += custoNs / 1e6;
        ticks++;
        if (executadosNaVolta > 1) {
            rajadas++;
        }
    }

    /** Soma do custo de cada trecho nomeado do desenho, nesta janela. */
    private static final java.util.Map<String, Double> secoes = new java.util.LinkedHashMap<>();

    /** Mede um trecho do desenho. Chame com o tempo que ele levou. */
    public static void secao(String nome, long ns) {
        if (LIGADO) {
            secoes.merge(nome, ns / 1e6, Double::sum);
        }
    }

    private static void imprimir(long agora) {
        if (!secoes.isEmpty() && n > 0) {
            StringBuilder sb = new StringBuilder("[perf]   por quadro:");
            for (java.util.Map.Entry<String, Double> e : secoes.entrySet()) {
                sb.append(String.format(java.util.Locale.ROOT, " %s=%.2fms", e.getKey(), e.getValue() / n));
            }
            System.out.println(sb);
            secoes.clear();
        }
        double segundos = (agora - inicioJanela) / 1e9;
        double[] v = Arrays.copyOf(intervalos, n);
        Arrays.sort(v);
        double media = Arrays.stream(v).average().orElse(0);
        double p99 = v.length == 0 ? 0 : v[Math.min(v.length - 1, (int) (v.length * 0.99))];
        double max = v.length == 0 ? 0 : v[v.length - 1];
        // DESVIO PADRÃO do intervalo: a medida direta de "inconstante". Média boa
        // com desvio alto é o quadro que sai na hora errada, não o quadro caro.
        double var = 0;
        for (double x : v) {
            var += (x - media) * (x - media);
        }
        double desvio = v.length == 0 ? 0 : Math.sqrt(var / v.length);
        // O GC DESTA JANELA, para casar travadas com coleta de lixo.
        long gc = 0, coletas = 0;
        for (java.lang.management.GarbageCollectorMXBean b
                : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            gc += Math.max(0, b.getCollectionTime());
            coletas += Math.max(0, b.getCollectionCount());
        }
        System.out.printf(java.util.Locale.ROOT,
                "[perf] fps=%.0f  quadro: media=%.2fms desvio=%.2fms p99=%.2fms (1%%low=%.0ffps) max=%.2fms"
                        + "  render=%.2fms  tick=%.2fms (%d/s, %d rajadas)  gc=%dms em %d coletas%n",
                n / segundos, media, desvio, p99, p99 == 0 ? 0 : 1000 / p99, max,
                n == 0 ? 0 : somaRender / n, ticks == 0 ? 0 : somaTick / ticks,
                (int) (ticks / segundos), rajadas, gc - gcAntes, coletas - coletasAntes);
        gcAntes = gc;
        coletasAntes = coletas;
        n = 0;
        somaRender = 0;
        somaTick = 0;
        ticks = 0;
        rajadas = 0;
        inicioJanela = agora;
    }
}
