package com.retronova.game.hud;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.Game;
import com.retronova.game.map.arena.Arena;

import java.awt.*;
import java.awt.image.BufferedImage;

public class WaveDisplay {

    private final BufferedImage clock;
    private final Point position;

    WaveDisplay() {
        this.clock = new SpriteSheet("ui", "clock", Configs.HudScale()).getSHEET();
        this.position = new Point();
    }

    public void tick() {
        this.position.setLocation(Engine.window.getWidth()/2, Configs.Margin());
    }

    public void render(Graphics2D g) {
        int x = this.position.x - clock.getWidth()/2;
        int y = this.position.y;
        if(Game.getMap() instanceof Arena) {
            renderClock(x, y, g);
            renderValues(x, y, g);
        }
    }

    private void renderClock(int x, int y, Graphics2D g) {
        g.drawImage(this.clock, x, y, null);
        int xCenter = (int)(x + 7.5 * Configs.HudScale());
        int yCenter = (int)(y + 7.5 * Configs.HudScale());
        int waveSeconds = Game.getWave().getSeconds();
        System.out.println(waveSeconds);
        double rad = ((Math.PI*2)/60) * waveSeconds;
        int xPole = (int)(xCenter + Math.cos(rad - Math.PI/2) * (3*Configs.HudScale()));
        int yPole = (int)(yCenter + Math.sin(rad - Math.PI/2) * (3*Configs.HudScale()));
        g.setStroke(new BasicStroke(Configs.HudScale()));
        g.drawLine(xCenter, yCenter, xPole, yPole);
    }

    private void renderValues(int x, int y, Graphics2D g) {
        Font font = FontG.font(FontG.Septem, Configs.HudScale()*6);
        g.setFont(font);

        int xLevel = x + 15 * Configs.HudScale();
        int yLevel = y + 6 * Configs.HudScale();
        String level = "Lvl: " + Game.getGame().getLevel();
        g.setColor(new Color(0x872341));
        g.drawString(level, xLevel + Configs.HudScale(), yLevel + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(level, xLevel, yLevel);

        int xCount = x + 15 * Configs.HudScale();
        int yCount = y + 11 * Configs.HudScale();
        String count = "Spw: " + Game.getWave().currentSpawn() + " / " + Game.getWave().amountSpawns();
        g.setColor(new Color(0x872341));
        g.drawString(count, xCount + Configs.HudScale(), yCount + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(count, xCount, yCount);


    }

}
