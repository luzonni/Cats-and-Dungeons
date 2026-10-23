package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Brilho;
import com.retronova.engine.graphics.Familia;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Estatisticas;
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

    /** Retomada: a barra volta com a carga gravada, sem onda no ar. */
    public static void repor(double gravada) {
        reiniciar();
        carga = Math.max(0, Math.min(1, gravada));
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
        double raio = raio(onda) * GameObject.SIZE() * Bonus.area(gato);
        Estatisticas.fonte("Feline Fury");
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
        Estatisticas.fonte(null);
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
        double r = raio(t) * GameObject.SIZE() * Bonus.area(Game.getPlayer());
        float vida = (float) Math.max(0, 1 - t / DURACAO);
        int s = Configs.GameScale();
        // A onda na família roxa da barra e dos ícones (Familia.EPICO), e o disco
        // de imagem pronta (Brilho). Eram um violeta e um branco puro soltos aqui.
        Graphics2D o = (Graphics2D) g.create();
        Brilho.disco(o, origemX, origemY, r, Familia.EPICO.tom(Familia.BRILHO, (int) (60 * vida)));
        o.setStroke(new BasicStroke(6f * s * vida + s));
        o.setColor(Familia.EPICO.tom(Familia.BASE, (int) (220 * vida)));
        o.drawOval((int) (origemX - r), (int) (origemY - r), (int) (r * 2), (int) (r * 2));
        o.setStroke(new BasicStroke(2f * s));
        o.setColor(Familia.EPICO.tom(Familia.BRILHO, (int) (255 * vida)));
        double r2 = r * 0.85;
        o.drawOval((int) (origemX - r2), (int) (origemY - r2), (int) (r2 * 2), (int) (r2 * 2));
        o.dispose();
    }

    /**
     * O clarão de tela no estouro, por cima do mundo.
     *
     * MAIS FRACO QUE ANTES (de 120 para {@link #CLARAO} de opacidade): a Fúria
     * estoura a cada trinta mortes, várias vezes por minuto na horda cheia, e um
     * clarão de quase metade da tela a cada vez é o tipo de flash que a Game
     * Accessibility Guidelines manda evitar. A onda no chão já anuncia o estouro.
     */
    public static void renderTela(Graphics2D g, int w, int h) {
        if (onda < 0 || onda > 8) {
            return;
        }
        int alfa = (int) (CLARAO * (1 - (onda + Engine.alpha()) / 8.0));
        g.setColor(Familia.EPICO.tom(Familia.BRILHO, Math.max(0, alfa)));
        g.fillRect(0, 0, w, h);
    }

    /** Opacidade máxima do clarão de tela no estouro (0-255). */
    private static final int CLARAO = 60;

    // ------------------------------------------------------------- a barra

    /**
     * A COR DA FÚRIA É A FAMÍLIA ROXA DOS ÍCONES: a cor do grau épico
     * (Raridade.EPICO) puxada um quinto para o tom frio da paleta, e a rampa dela
     * pela mesma fórmula de tools/GenIcones — sombra, base, luz e brilho. Antes era
     * um lilás saturado solto no código, com moldura preta e o rótulo em
     * drawString com sombra preta: a única barra da HUD fora do desenho das outras.
     */
    private static final Color BASE = Familia.EPICO.tom(Familia.BASE);
    private static final Color SOMBRA = Familia.EPICO.tom(Familia.SOMBRA), LUZ = Familia.EPICO.tom(Familia.LUZ),
            BRILHO = Familia.EPICO.tom(Familia.BRILHO);
    /** O pulso de quase cheia, em degraus prontos da base até o brilho: nenhuma cor nova por quadro. */
    private static final Color[] PULSO = new Color[8];
    /** Em quantas partes a barra é marcada: um traço a cada dez mortes, como as fases do chefe. */
    private static final int PARTES = 3;

    static {
        for (int i = 0; i < PULSO.length; i++) {
            PULSO[i] = misturar(BASE, LUZ, i / (float) (PULSO.length - 1));
        }
    }

    /**
     * A barra da Furia, acima da hotbar, no desenho das barras da HUD (ver
     * BarraDeChefe): contorno da paleta, fundo fundo, o preenchimento com a luz no
     * fio de cima e a sombra no de baixo, e um traço a cada terço. Pulsa quando
     * está quase cheia, para o jogador sentir o estouro chegando.
     */
    public static void renderBarra(Graphics2D g, int w, int h) {
        int s = Configs.HudScale();
        int largura = 70 * s, altura = 4 * s;
        int x = w / 2 - largura / 2;
        // A barra fica 9 pixels de arte abaixo do topo da peça: o rótulo vai em cima.
        int y = com.retronova.game.hud.LayoutDoHud.caixa(com.retronova.game.hud.LayoutDoHud.Peca.FURIA).y + 9 * s;
        double quase = carga >= 0.85 ? 0.5 + 0.5 * Math.sin(com.retronova.game.Game.tempoVisual() * 12) : 0;
        int fio = Math.max(1, s);

        g.setColor(Palette.OUTLINE);
        g.fillRect(x - s, y - s, largura + 2 * s, altura + 2 * s);
        g.setColor(Palette.DEEP);
        g.fillRect(x, y, largura, altura);
        int cheio = (int) (largura * carga);
        if (cheio > 0) {
            g.setColor(PULSO[(int) Math.round(quase * (PULSO.length - 1))]);
            g.fillRect(x, y, cheio, altura);
            g.setColor(quase > 0.5 ? BRILHO : LUZ);
            g.fillRect(x, y, cheio, fio);
            g.setColor(SOMBRA);
            g.fillRect(x, y + altura - fio, cheio, fio);
        }
        g.setColor(Palette.OUTLINE);
        for (int p = 1; p < PARTES; p++) {
            int px = x + largura * p / PARTES;
            g.fillRect(px - fio / 2, y - s, fio, altura + 2 * s);
        }

        Font f = FontHandler.font(FontHandler.Game, 5f * s);
        String rotulo = "FURY";
        int lw = FontHandler.getWidth(rotulo, f);
        TextFx.draw(g, rotulo, f, w / 2 - lw / 2, y - 2 * s, quase > 0.5 ? BRILHO : Palette.TEXT, s);
    }

    private static Color misturar(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
