package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Familia;
import java.awt.Graphics2D;

/** Despedida em pixels na paleta da chegada; sobe e se desfaz junto com o corpo. */
final class EspiritoDoGato {
    static final int SUBIDA = 12;
    private static final int FRAGMENTOS = 5;

    private EspiritoDoGato() { }

    static void desenhar(Graphics2D g, Player gato, float fase) {
        if (fase <= 0 || fase >= 1) return;
        int s = Configs.GameScale();
        int alfa = (int) (255 * Math.min(1, fase * 6) * (1 - fase));
        int x = (int) gato.getX() + gato.getWidth() / 2;
        int y = (int) gato.getY() + gato.getHeight() / 3 - Math.round(SUBIDA * s * fase);
        int oscila = (int) Math.round(Math.sin(fase * Math.PI * 3) * s);
        g.setColor(Familia.ACO.tom(Familia.BASE, alfa));
        g.fillRect(x - 2 * s + oscila, y - 2 * s, 4 * s, 4 * s);
        g.setColor(Familia.ACO.tom(Familia.LUZ, alfa));
        g.fillRect(x - s + oscila, y - 4 * s, 2 * s, 5 * s);
        g.setColor(Familia.ACO.tom(Familia.BRILHO, alfa));
        g.fillRect(x - s + oscila, y - s, 2 * s, 2 * s);
        for (int i = 0; i < FRAGMENTOS; i++) {
            double angulo = i * Math.PI * 2 / FRAGMENTOS + fase * Math.PI;
            int raio = (int) ((2 + fase * 6) * s);
            int px = x + (int) (Math.cos(angulo) * raio);
            int py = y + (int) (Math.sin(angulo) * raio * 0.6);
            g.setColor(Familia.ACO.tom(i % 2 == 0 ? Familia.LUZ : Familia.BASE, alfa));
            g.fillRect(px, py, s, s);
        }
    }
}
