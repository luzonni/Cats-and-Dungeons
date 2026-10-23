package com.retronova.menus;

/** Relógio da apresentação: pular muda só o desenho, nunca ouro ou estatísticas. */
final class ApresentacaoDoFim {
    static final int TOTAL = 60;
    static final int TITULO = 18;
    static final int RESUMO = 12, LOJA = 20, PAINEL = 22;
    static final int BARRAS = 24, CRESCIMENTO = 30;
    static final int OURO = 42, BRILHO = 18;
    static final int DESLOCAMENTO = 8;

    private int ticks;

    void tick() { if (ticks < TOTAL) ticks++; }
    void concluir() { ticks = TOTAL; }
    boolean terminou() { return ticks >= TOTAL; }
    boolean aceitaPulo() { return ticks >= TITULO; }
    double tempo(float alfa) { return ticks == 0 ? 0 : Math.min(TOTAL, ticks + alfa); }

    static float progresso(double tempo, int inicio, int duracao) {
        double t = Math.clamp((tempo - inicio) / duracao, 0, 1);
        return (float) (1 - Math.pow(1 - t, 3));
    }
}
