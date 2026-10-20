package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.game.Game;

import java.awt.*;

/**
 * Número de dano de um acerto crítico.
 *
 * O crítico saía como um {@link Word} comum com "!" no fim — branco, do mesmo
 * tamanho do dano normal e com sombra de um passo só. No meio de uma onda ele
 * sumia entre os outros números. Aqui ele ganha a etiqueta "CRIT", um ícone de
 * estouro (no espírito do ícone de crítico do LoL) e contorno fechado, em
 * laranja e ouro, para ser lido de relance.
 *
 * Tudo é desenhado sem antialiasing: o borrado era o que tirava a nitidez.
 */
public class Critico extends Particle {

    /** Estouro de 9x9: 1 = raio, 2 = miolo. */
    private static final int[][] ICONE = {
            {0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 1, 0, 0, 1, 0},
            {0, 0, 1, 0, 1, 0, 1, 0, 0},
            {0, 0, 0, 2, 2, 2, 0, 0, 0},
            {1, 1, 1, 2, 2, 2, 1, 1, 1},
            {0, 0, 0, 2, 2, 2, 0, 0, 0},
            {0, 0, 1, 0, 1, 0, 1, 0, 0},
            {0, 1, 0, 0, 1, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0},
    };

    private static final Color CONTORNO = new Color(0x1A0A0A);
    private static final Color RAIO = new Color(0xFF6A1F);
    private static final Color MIOLO = new Color(0xFFE45C);
    private static final Color NUMERO = new Color(0xFFD23F);
    private static final Color NUMERO_BAIXO = new Color(0xFF8A2A);
    private static final Color ETIQUETA = new Color(0xFF5A3C);

    private final String valor;
    private final Font fonteNumero;
    private final Font fonteEtiqueta;
    private int count;

    public Critico(int dano, double x, double y) {
        super(x, y, 1.1);
        this.valor = String.valueOf(dano);
        int s = Configs.GameScale();
        this.fonteNumero = FontHandler.font(FontHandler.Septem, s * 11);
        this.fonteEtiqueta = FontHandler.font(FontHandler.Septem, s * 6);
    }

    @Override
    public void tick() {
        count++;
        if (count >= getSeconds()) {
            Game.getMap().remove(this);
        }
        // Sobe rápido e desacelera: o golpe "salta" do inimigo.
        double v = Math.max(0.15, 1.2 - count * 0.06);
        this.setY(getY() - v * Configs.GameScale());
    }

    @Override
    public void render(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int s = Configs.GameScale();

        int larguraNumero = FontHandler.getWidth(valor, fonteNumero);
        int alturaNumero = FontHandler.getHeight(valor, fonteNumero);
        int icone = ICONE.length * s;
        int folga = 2 * s;
        int total = icone + folga + larguraNumero;

        int x = (int) getX() - total / 2;
        int base = (int) getY() + alturaNumero / 2;

        desenharIcone(g, x, base - alturaNumero / 2 - icone / 2, s);

        int nx = x + icone + folga;
        texto(g, valor, fonteNumero, nx, base, NUMERO, NUMERO_BAIXO, s);

        String etiqueta = "CRIT";
        int ey = base - alturaNumero - 2 * s;
        texto(g, etiqueta, fonteEtiqueta, nx, ey, ETIQUETA, ETIQUETA, s);
        g.dispose();
    }

    private static void desenharIcone(Graphics2D g, int x, int y, int s) {
        g.setColor(CONTORNO);
        for (int i = 0; i < ICONE.length; i++) {
            for (int j = 0; j < ICONE[i].length; j++) {
                if (ICONE[i][j] != 0) {
                    g.fillRect(x + (j - 1) * s, y + (i - 1) * s, 3 * s, 3 * s);
                }
            }
        }
        for (int i = 0; i < ICONE.length; i++) {
            for (int j = 0; j < ICONE[i].length; j++) {
                if (ICONE[i][j] == 0) {
                    continue;
                }
                g.setColor(ICONE[i][j] == 2 ? MIOLO : RAIO);
                g.fillRect(x + j * s, y + i * s, s, s);
            }
        }
        g.setColor(Color.white);
        g.fillRect(x + 4 * s, y + 4 * s, s, s);
    }

    /** Contorno fechado + preenchimento em duas faixas, como {@code TextFx}. */
    private static void texto(Graphics2D g, String t, Font f, int x, int y,
                              Color topo, Color baixo, int s) {
        g.setFont(f);
        g.setColor(CONTORNO);
        g.drawString(t, x, y + 2 * s);
        for (int dx = -s; dx <= s; dx += s) {
            for (int dy = -s; dy <= s; dy += s) {
                if (dx != 0 || dy != 0) {
                    g.drawString(t, x + dx, y + dy);
                }
            }
        }
        g.setColor(topo);
        g.drawString(t, x, y);
        if (!baixo.equals(topo)) {
            int ascent = g.getFontMetrics().getAscent();
            Shape clip = g.getClip();
            g.clipRect(x, y - ascent / 3, FontHandler.getWidth(t, f) + s, ascent);
            g.setColor(baixo);
            g.drawString(t, x, y);
            g.setClip(clip);
        }
    }
}
