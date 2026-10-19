package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.items.ArmaExtra;
import com.retronova.game.items.Melhorias;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * O que as armas extras fazem a cada tick e como aparecem.
 *
 * Fica fora do Player porque nao e do gato: e do que ele ganhou na roleta. O
 * Game chama tick e render; os niveis moram em Melhorias, para viajarem no save.
 *
 * O desenho e procedural, de proposito, e no traco da casa: blocos de pixel do
 * tamanho de GameScale. Nao ha folha de sprite para estas armas, e esperar uma
 * seria deixar o sistema sem rosto.
 */
public final class Arsenal {

    private Arsenal() {
    }

    /** Relogio proprio: gira mesmo com o gato parado. */
    private static long tempo;

    /** Ultimo tick em que cada bicho levou de cada arma, para nao bater todo tick. */
    private static final Map<Enemy, long[]> ULTIMO_GOLPE = new WeakHashMap<>();

    // ------------------------------------------------------------ numeros

    private static int bolas(int nivel) {
        return nivel <= 0 ? 0 : 1 + nivel / 2;                 // 1,2,2,3,3
    }

    private static int livros(int nivel) {
        return nivel <= 0 ? 0 : Math.min(4, nivel == 4 ? 3 : nivel);   // 1,2,3,3,4
    }

    private static double raioAura(int nivel) {
        return (1.4 + 0.3 * nivel) * GameObject.SIZE();
    }

    // -------------------------------------------------------------- tick

    public static void tick(Player gato) {
        tempo++;
        ArmasDeArea.tick(gato);   // as de area tem relogio proprio
        Melhorias m = gato.getMelhorias();
        int novelo = m.nivel(ArmaExtra.NOVELO);
        int grimorio = m.nivel(ArmaExtra.GRIMORIO);
        int aura = m.nivel(ArmaExtra.AURA);
        if (novelo + grimorio + aura == 0) {
            return;
        }
        List<Enemy> bichos = Game.getMap().getEntities(Enemy.class);
        double cx = gato.getX() + gato.getWidth() / 2.0;
        double cy = gato.getY() + gato.getHeight() / 2.0;
        int s = Configs.GameScale();

        if (novelo > 0) {
            double dano = 5 + 2 * novelo + gato.getDamage() * 0.35;
            for (double[] p : posicoesNovelo(cx, cy, novelo, 0)) {
                Rectangle r = new Rectangle((int) p[0] - 4 * s, (int) p[1] - 4 * s, 8 * s, 8 * s);
                acertar(bichos, r, 0, 18, dano, AttackTypes.Impact, 0, cx, cy);
            }
        }
        if (grimorio > 0) {
            double dano = 9 + 3 * grimorio + gato.getDamage() * 0.5;
            double empurrao = grimorio >= 4 ? 7 : 4;
            for (double[] p : posicoesGrimorio(cx, cy, grimorio, 0)) {
                Rectangle r = new Rectangle((int) p[0] - 5 * s, (int) p[1] - 6 * s, 10 * s, 12 * s);
                acertar(bichos, r, 1, 30, dano, AttackTypes.Sorcery, empurrao, cx, cy);
            }
        }
        if (aura > 0 && tempo % Math.max(20, 42 - 4 * aura) == 0) {
            double dano = 3 + 1.8 * aura + gato.getDamage() * 0.15;
            double raio = raioAura(aura);
            for (Enemy e : bichos) {
                if (e.nascendo()) {
                    continue;
                }
                double dx = e.getX() + e.getWidth() / 2.0 - cx;
                double dy = e.getY() + e.getHeight() / 2.0 - cy;
                if (dx * dx + dy * dy < raio * raio) {
                    e.strike(AttackTypes.Poison, dano);
                }
            }
        }
    }

    private static void acertar(List<Enemy> bichos, Rectangle area, int arma, int intervalo,
                                double dano, AttackTypes tipo, double empurrao,
                                double cx, double cy) {
        for (Enemy e : bichos) {
            if (e.nascendo() || !e.getBounds().intersects(area)) {
                continue;
            }
            long[] ultimo = ULTIMO_GOLPE.computeIfAbsent(e, k -> new long[]{-999, -999});
            if (tempo - ultimo[arma] < intervalo) {
                continue;
            }
            ultimo[arma] = tempo;
            e.strike(tipo, dano);
            if (empurrao > 0) {
                double ang = Math.atan2(e.getY() - cy, e.getX() - cx);
                e.getPhysical().addForce("arsenal", empurrao, ang);
            }
        }
    }

    // ---------------------------------------------------------- posicoes

    private static double[][] posicoesNovelo(double cx, double cy, int nivel, float alpha) {
        int n = bolas(nivel);
        double raio = 1.7 * GameObject.SIZE();
        double giro = (tempo + alpha) * 0.085;
        double[][] p = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = giro + i * Math.PI * 2 / n;
            p[i] = new double[]{cx + Math.cos(a) * raio, cy + Math.sin(a) * raio};
        }
        return p;
    }

    private static double[][] posicoesGrimorio(double cx, double cy, int nivel, float alpha) {
        int n = livros(nivel);
        double raio = 2.7 * GameObject.SIZE();
        double giro = -(tempo + alpha) * 0.05;    // ao contrario do novelo
        double[][] p = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = giro + i * Math.PI * 2 / n;
            p[i] = new double[]{cx + Math.cos(a) * raio, cy + Math.sin(a) * raio};
        }
        return p;
    }

    // ------------------------------------------------------------ render

    /**
     * Desenha as armas em volta do gato. Chamado DENTRO da translacao de
     * interpolacao do gato, entao usa a posicao de tick dele e anda junto.
     */
    public static void render(Graphics2D g, Player gato) {
        Melhorias m = gato.getMelhorias();
        int novelo = m.nivel(ArmaExtra.NOVELO);
        int grimorio = m.nivel(ArmaExtra.GRIMORIO);
        int aura = m.nivel(ArmaExtra.AURA);
        if (novelo + grimorio + aura == 0) {
            return;
        }
        float alpha = Engine.pausado() ? 0f : Engine.alpha();
        double cx = gato.getX() + gato.getWidth() / 2.0;
        double cy = gato.getY() + gato.getHeight() / 2.0;
        int s = Configs.GameScale();

        if (aura > 0) {
            int r = (int) raioAura(aura);
            double pulso = 0.5 + 0.5 * Math.sin((tempo + alpha) * 0.12);
            g.setColor(new Color(91, 224, 122, (int) (28 + 22 * pulso)));
            g.fillOval((int) cx - r, (int) cy - r, r * 2, r * 2);
            g.setColor(new Color(150, 255, 170, (int) (70 + 50 * pulso)));
            g.setStroke(new BasicStroke(s));
            g.drawOval((int) cx - r, (int) cy - r, r * 2, r * 2);
        }
        if (grimorio > 0) {
            for (double[] p : posicoesGrimorio(cx, cy, grimorio, alpha)) {
                desenharLivro(g, (int) p[0], (int) p[1], s);
            }
        }
        if (novelo > 0) {
            for (double[] p : posicoesNovelo(cx, cy, novelo, alpha)) {
                desenharNovelo(g, (int) p[0], (int) p[1], s, (tempo + alpha) * 0.3);
            }
        }
    }

    private static void desenharNovelo(Graphics2D g, int x, int y, int s, double giro) {
        int r = 4 * s;
        g.setColor(new Color(0x7A2A4A));
        g.fillOval(x - r - s, y - r - s, (r + s) * 2, (r + s) * 2);
        g.setColor(new Color(0xFF6FA8));
        g.fillOval(x - r, y - r, r * 2, r * 2);
        // Os fios: tres arcos girando, que e o que faz a bola parecer rolar.
        g.setColor(new Color(0xFFC2DA));
        g.setStroke(new BasicStroke(Math.max(1, s)));
        for (int i = 0; i < 3; i++) {
            int inicio = (int) Math.toDegrees(giro) + i * 60;
            g.drawArc(x - r + s, y - r + s, (r - s) * 2, (r - s) * 2, inicio, 100);
        }
        g.setColor(Color.WHITE);
        g.fillRect(x - r / 2, y - r / 2, s, s);
    }

    private static void desenharLivro(Graphics2D g, int x, int y, int s) {
        int w = 9 * s;
        int h = 11 * s;
        int x0 = x - w / 2;
        int y0 = y - h / 2;
        g.setColor(new Color(255, 214, 90, 60));
        g.fillOval(x0 - 3 * s, y0 - 3 * s, w + 6 * s, h + 6 * s);
        g.setColor(new Color(0x2A1A10));
        g.fillRect(x0 - s, y0 - s, w + 2 * s, h + 2 * s);
        g.setColor(new Color(0x7A2E2E));
        g.fillRect(x0, y0, w, h);
        g.setColor(new Color(0xF2E6C8));            // paginas
        g.fillRect(x0 + w - 2 * s, y0 + s, s, h - 2 * s);
        g.setColor(new Color(0xC9A227));            // cruz dourada
        g.fillRect(x0 + w / 2 - s, y0 + 2 * s, s * 2 - s, h - 4 * s);
        g.fillRect(x0 + 2 * s, y0 + h / 3, w - 5 * s, s);
    }
}
