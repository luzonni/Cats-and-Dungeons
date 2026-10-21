package com.retronova.game.hud;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.enemies.Chefe;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/**
 * A barra de vida do chefe, no alto da tela, abaixo do relógio.
 *
 * Sem ela o jogador não sabia quanto faltava: batia num bicho enorme e opaco
 * sem retorno nenhum. A barra mostra o nome, a vida e uma marca onde cada fase
 * começa, então a mudança de comportamento deixa de ser surpresa.
 *
 * Entre a vida real e a barra existe um pedaço mais claro que escorre devagar:
 * é o dano recente. Ele faz um golpe grande ser lido como golpe grande, mesmo
 * quando a barra é larga e o pedaço tirado é pequeno.
 */
final class BarraDeChefe {

    private static final Color VIDA = new Color(0xC23A4E);
    private static final Color VIDA_ALTA = new Color(0xE0647A);
    private static final Color DANO_RECENTE = new Color(0xF2D9A0);

    /** Quanto da barra o "dano recente" ainda mostra, de 0 a 1. */
    private double recente = -1;
    private Chefe acompanhado;

    void tick() {
        Chefe chefe = procurar();
        if (chefe != acompanhado) {
            acompanhado = chefe;
            recente = chefe == null ? -1 : fracao(chefe);
        }
        if (chefe == null) {
            return;
        }
        double alvo = fracao(chefe);
        // Sobe de uma vez (cura), desce devagar (dano): o rastro de quem apanhou.
        recente = alvo >= recente ? alvo : Math.max(alvo, recente - 0.004);
    }

    void render(Graphics2D g) {
        Chefe chefe = acompanhado;
        if (chefe == null) {
            return;
        }
        int s = Configs.HudScale();
        int w = Math.min(Engine.window.getWidth() - 40 * s, 180 * s);
        int h = 6 * s;
        int x = Engine.window.getWidth() / 2 - w / 2;
        int y = Configs.Margin() + 36 * s;

        Font fonte = FontHandler.font(FontHandler.Game, 7f * s);
        String nome = chefe.nome();
        int nw = FontHandler.getWidth(nome, fonte);
        TextFx.draw(g, nome, fonte, x + w / 2 - nw / 2, y - 2 * s, Palette.TEXT, s);

        g.setColor(Palette.OUTLINE);
        g.fillRect(x - s, y - s, w + 2 * s, h + 2 * s);
        g.setColor(Palette.DEEP);
        g.fillRect(x, y, w, h);

        double fracao = fracao(chefe);
        g.setColor(DANO_RECENTE);
        g.fillRect(x, y, (int) (w * Math.max(fracao, recente)), h);
        g.setColor(VIDA);
        g.fillRect(x, y, (int) (w * fracao), h);
        g.setColor(VIDA_ALTA);
        g.fillRect(x, y, (int) (w * fracao), Math.max(1, s));

        // Onde cada fase começa: um traço na barra.
        g.setColor(Palette.OUTLINE);
        for (int f = 1; f < chefe.fases(); f++) {
            int px = x + (int) (w * (1 - f / (double) chefe.fases()));
            g.fillRect(px - Math.max(1, s / 2), y - s, Math.max(1, s), h + 2 * s);
        }
    }

    private static double fracao(Chefe c) {
        double max = c.getLifeSize();
        return max <= 0 ? 0 : Math.max(0, Math.min(1, c.getLife() / max));
    }

    /** O chefe vivo e já nascido; se houver mais de um, o de menos vida. */
    private static Chefe procurar() {
        if (Game.getMap() == null) {
            return null;
        }
        List<Enemy> inimigos = Game.getMap().getEntities(Enemy.class);
        Chefe achado = null;
        for (Enemy e : inimigos) {
            if (e instanceof Chefe c && !c.nascendo() && c.getLife() > 0
                    && (achado == null || fracao(c) < fracao(achado))) {
                achado = c;
            }
        }
        return achado;
    }
}
