package com.retronova.game.map;

/** Impulso curto em pixels da tela: o zoom não pode apagar a pancada da morte. */
final class ImpactoDaMorte {
    static final int DURACAO = 24;
    static final int AMPLITUDE = 8;
    private static final double FREQUENCIA_X = 1.15, FREQUENCIA_Y = 1.60;
    private static final double PROPORCAO_Y = 0.65, FASE_Y = 0.8;
    private int idade = DURACAO;

    void iniciar() { idade = 0; }
    void tick() { if (idade < DURACAO) idade++; }

    double x(float alfa) { return xEm(idade + alfa); }
    double y(float alfa) { return yEm(idade + alfa); }

    static double xEm(double tempo) {
        if (tempo >= DURACAO) return 0;
        return Math.cos(tempo * FREQUENCIA_X) * forca(tempo);
    }

    static double yEm(double tempo) {
        if (tempo >= DURACAO) return 0;
        return Math.sin(tempo * FREQUENCIA_Y + FASE_Y) * forca(tempo) * PROPORCAO_Y;
    }

    private static double forca(double tempo) {
        double restante = 1 - Math.clamp(tempo / DURACAO, 0, 1);
        return AMPLITUDE * restante * restante;
    }
}
