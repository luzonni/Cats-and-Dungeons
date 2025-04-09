package com.retronova.game.hud;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.Game;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class WaveDisplay {

    private final BufferedImage clock;
    private final Point position;
    private int amountOfEnemies;

    WaveDisplay() {
        this.clock = new SpriteSheet("ui", "clock", Configs.HudScale()).getSHEET();
        this.position = new Point();
    }

    public void tick() {
        this.position.setLocation(Engine.window.getWidth()/2, Configs.Margin());
        this.amountOfEnemies = Game.getMap().getEntities(Enemy.class).size();
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

        int xLevel = x + 18 * Configs.HudScale();
        int yLevel = y + 7 * Configs.HudScale();
        String level = "Level: " + (Game.getGame().getLevel()+1);
        g.setColor(new Color(0x872341));
        g.drawString(level, xLevel + Configs.HudScale(), yLevel + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(level, xLevel, yLevel);

        int xCount = x + 18 * Configs.HudScale();
        int yCount = y + 13 * Configs.HudScale();
        String count = "Spawns: " + Game.getWave().currentSpawn() + " / " + Game.getWave().amountSpawns();
        g.setColor(new Color(0x872341));
        g.drawString(count, xCount + Configs.HudScale(), yCount + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(count, xCount, yCount);

        int xAmount = x + 18 * Configs.HudScale();
        int yAmount = y + 19 * Configs.HudScale();
        String amount = "Enemies: " + this.amountOfEnemies;
        g.setColor(new Color(0x872341));
        g.drawString(amount, xAmount + Configs.HudScale(), yAmount + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(amount, xAmount, yAmount);


    }

}
