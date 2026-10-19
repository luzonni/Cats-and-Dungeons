package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.map.Camera;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.particles.Word;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * A Furia Felina: o dano em area que a horda carrega para o gato.
 *
 * Cada morte enche a barra. Cheia, ela estoura SOZINHA numa onda de choque que
 * sai do gato, fere e empurra tudo num raio grande, sacode a tela e clareia o
 * mundo. Automatica de proposito: no meio do cerco o jogador esta ocupado
 * andando, e o pico de dopamina nao pode depender de lembrar de apertar algo —
 * ele chega no instante em que a sala esta mais cheia, porque e a sala cheia
 * que encheu a barra.
 *
 * Fica fora do Player pelo mesmo motivo do Arsenal: e mecanica de arena, e o
 * Game chama tick e render.
 */
public final class Furia {

    private Furia() {
    }

    /** Mortes para encher a barra. */
    private static final int MORTES = 30;
    /** Duracao da onda, em ticks, e raio final em tiles. */
    private static final int DURACAO = 24;
    private static final double RAIO_FINAL = 7;

    private static double carga;
    private static int onda = -1;           // -1 = sem onda no ar
    private static double origemX, origemY;
    private static final Set<Enemy> atingidos = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Chamado a cada morte de bicho. */
    public static void somarMorte() {
        if (onda >= 0) {
            return;     // a morte causada pela propria onda nao recarrega a proxima
        }
        carga = Math.min(1, carga + 1.0 / MORTES);
    }

    public static double carga() {
        return carga;
    }

    /** Zera tudo: partida nova. */
    public static void reiniciar() {
        carga = 0;
        onda = -1;
        atingidos.clear();
    }

    public static void tick(Player gato) {
        if (onda < 0 && carga >= 1) {
            disparar(gato);
        }
        if (onda < 0) {
            return;
        }
        onda++;
        double raio = raio(onda) * GameObject.SIZE();
        double dano = 30 + gato.getDamage() * 3;
        List<Enemy> bichos = Game.getMap().getEntities(Enemy.class);
        for (Enemy e : bichos) {
            if (e.nascendo() || atingidos.contains(e)) {
                continue;
            }
            double ex = e.getX() + e.getWidth() / 2.0;
            double ey = e.getY() + e.getHeight() / 2.0;
            if (Math.hypot(ex - origemX, ey - origemY) <= raio) {
                atingidos.add(e);
                e.strike(AttackTypes.Explosion, dano);
                e.getPhysical().addForce("furia", 9, Math.atan2(ey - origemY, ex - origemX));
            }
        }
        if (onda >= DURACAO) {
            onda = -1;
            atingidos.clear();
        }
    }

    private static void disparar(Player gato) {
        carga = 0;
        onda = 0;
        atingidos.clear();
        origemX = gato.getX() + gato.getWidth() / 2.0;
        origemY = gato.getY() + gato.getHeight() / 2.0;
        Camera.tremer(16);
        Sound.play(Sounds.Crack);
        Sound.play(Sounds.Conquista);
        Game.getMap().put(new Word("FURY!", origemX, origemY - GameObject.SIZE(), 1.5));
    }

    /** Raio em tiles no tick t da onda: sai rapido e desacelera (ease-out). */
    private static double raio(double t) {
        double x = Math.min(1, t / DURACAO);
        return RAIO_FINAL * (1 - Math.pow(1 - x, 3));
    }

    /** A onda no mundo: dois aneis, um branco e um violeta, apagando ao crescer. */
    public static void renderMundo(Graphics2D g) {
        if (onda < 0) {
            return;
        }
        double t = onda + (Engine.pausado() ? 0 : Engine.alpha());
        double r = raio(t) * GameObject.SIZE();
        float vida = (float) Math.max(0, 1 - t / DURACAO);
        int s = Configs.GameScale();
        Graphics2D o = (Graphics2D) g.create();
        o.setColor(new Color(255, 255, 255, (int) (60 * vida)));
        o.fillOval((int) (origemX - r), (int) (origemY - r), (int) (r * 2), (int) (r * 2));
        o.setStroke(new BasicStroke(6f * s * vida + s));
        o.setColor(new Color(200, 120, 255, (int) (220 * vida)));
        o.drawOval((int) (origemX - r), (int) (origemY - r), (int) (r * 2), (int) (r * 2));
        o.setStroke(new BasicStroke(2f * s));
        o.setColor(new Color(255, 255, 255, (int) (255 * vida)));
        double r2 = r * 0.85;
        o.drawOval((int) (origemX - r2), (int) (origemY - r2), (int) (r2 * 2), (int) (r2 * 2));
        o.dispose();
    }

    /** O clarao de tela no estouro, por cima do mundo. */
    public static void renderTela(Graphics2D g, int w, int h) {
        if (onda < 0 || onda > 8) {
            return;
        }
        int alfa = (int) (120 * (1 - (onda + Engine.alpha()) / 8.0));
        g.setColor(new Color(235, 210, 255, Math.max(0, alfa)));
        g.fillRect(0, 0, w, h);
    }

    /**
     * A barra da Furia, acima da hotbar. Pulsa quando esta quase cheia, para o
     * jogador sentir o estouro chegando.
     */
    public static void renderBarra(Graphics2D g, int w, int h) {
        int s = Configs.HudScale();
        int largura = 70 * s, altura = 4 * s;
        int x = w / 2 - largura / 2;
        int y = h - Configs.Margin() - 34 * s;
        double quase = carga >= 0.85 ? 0.5 + 0.5 * Math.sin(System.nanoTime() / 1e9 * 12) : 0;
        g.setColor(new Color(10, 6, 16, 180));
        g.fillRect(x - s, y - s, largura + 2 * s, altura + 2 * s);
        int cheio = (int) (largura * carga);
        g.setColor(new Color(150 + (int) (80 * quase), 80, 230));
        g.fillRect(x, y, cheio, altura);
        g.setColor(new Color(255, 255, 255, 60 + (int) (120 * quase)));
        g.fillRect(x, y, cheio, Math.max(1, s));
        Font f = FontHandler.font(FontHandler.Septem, 5f * s);
        g.setFont(f);
        String rotulo = "FURY";
        int lw = FontHandler.getWidth(rotulo, f);
        g.setColor(Color.BLACK);
        g.drawString(rotulo, w / 2 - lw / 2 + 1, y - 2 * s + 1);
        g.setColor(new Color(220, 180, 255));
        g.drawString(rotulo, w / 2 - lw / 2, y - 2 * s);
    }
}
