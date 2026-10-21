package com.retronova.game.hud;

import com.retronova.engine.graphics.Palette;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.graphics.UiSprite;
import com.retronova.game.map.arena.Waves;
import com.retronova.game.Game;
import com.retronova.game.map.arena.Arena;

import java.awt.*;

/**
 * O relogio da corrida, no alto da tela: o ponteiro da uma volta so, a corrida
 * inteira, e o texto diz quanto falta ate os 30 minutos, quando nasce o Rato Rei.
 */
public class WaveDisplay {

    private final UiSprite clock;
    private final Point position;

    WaveDisplay() {
        this.clock = new UiSprite("ui", "clock");
        this.position = new Point();
    }

    public void tick() {
        this.position.setLocation(Engine.window.getWidth()/2, Configs.Margin());
    }

    public void render(Graphics2D g) {
        int x = this.position.x - clock.largura()/2;
        int y = this.position.y;
        if(Game.getMap() instanceof Arena) {
            renderClock(x, y, g);
            renderTimer(x, y, g);
        }
    }

    private void renderClock(int x, int y, Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g.drawImage(this.clock.imagem(), x, y, null);
        int xCenter = x + 12 * Configs.HudScale();
        int yCenter = y + 12 * Configs.HudScale();
        int waveSeconds = Game.getWave().getSeconds();
        double rad = (Math.PI * 2) * Math.min(1d, waveSeconds / (Waves.DURACAO_MINUTOS * 60d));
        int xPole = (int)(xCenter + Math.cos(rad - Math.PI/2) * (6*Configs.HudScale()));
        int yPole = (int)(yCenter + Math.sin(rad - Math.PI/2) * (6*Configs.HudScale()));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2*Configs.HudScale(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(xCenter, yCenter, xPole, yPole);
    }

    /**
     * O painel e so o relogio: quanto falta ate os 30 minutos. Abates e inimigos
     * vivos sairam — ninguem decide nada olhando para eles no meio da horda.
     */
    private void renderTimer(int x, int y, Graphics2D g) {
        int s = Configs.HudScale();
        int total = Waves.DURACAO_MINUTOS * 60;
        int seg = Game.getWave().getSeconds();
        // "BOSS!" so enquanto o rei esta de pe: depois de vencido o relogio volta a contar.
        boolean chefe = seg >= total && !com.retronova.game.Estatisticas.venceu();
        String texto = chefe ? "BOSS!" : String.format("%02d:%02d", seg / 60, seg % 60);
        boolean passou = seg >= total;
        String meta = !chefe && !passou ? "/" + Waves.DURACAO_MINUTOS + ":00" : "";
        int tx = x + 26 * s;
        // O TEXTO CABE NA PLACA, e não o contrário. A largura das fontes não cresce
        // exatamente com a escala do HUD, e em algumas escalas o "/30:00" passava da
        // borda direita do sprite. Mede e, se não couber, desce a fonte um ponto
        // por vez.
        int livre = x + clock.largura() - 4 * s - tx;
        Font font = null, miuda = null;
        int largura = 0;
        for (float tam = 8; tam >= 5; tam -= 1) {
            font = FontHandler.font(FontHandler.Game, s * tam);
            miuda = FontHandler.font(FontHandler.Septem, s * (tam - 2));
            largura = FontHandler.getWidth(texto, font)
                    + (meta.isEmpty() ? 0 : 2 * s + FontHandler.getWidth(meta, miuda));
            if (largura <= livre) {
                break;
            }
        }
        int alt = FontHandler.getHeight(texto, font);
        int ty = y + 12 * s + alt / 2;
        TextFx.draw(g, texto, font, tx, ty, chefe ? Palette.WARN : Palette.TEXT, s);
        if (!meta.isEmpty()) {
            TextFx.draw(g, meta, miuda, tx + FontHandler.getWidth(texto, font) + 2 * s, ty, Palette.LIGHT, s);
        }
    }

}
