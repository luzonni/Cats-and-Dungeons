package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class GameOver implements Activity {

    private final Player player;
    private final Font gameOverFont = FontG.font(FontG.Game, 48 * Configs.UiScale());
    private final Font escFont = FontG.font(FontG.Game, 15 * Configs.UiScale());
    private long lastBlinkTime = 0;
    private boolean isTextVisible = true;

    public GameOver(Player player) {
        Sound.stopAll();
        Sound.play(Musics.GameOver, true);
        this.player = player;
    }

    @Override
    public void tick() {
        if (KeyBoard.KeyPressed("ESCAPE")) {
            Engine.heapActivity(new Menu());
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlinkTime > 500) {
            isTextVisible = !isTextVisible;
            lastBlinkTime = currentTime;
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());

        g.setFont(gameOverFont);
        FontMetrics fmGameOver = g.getFontMetrics(gameOverFont);
        String gameOverText = "Game Over";
        int gameOverX = (Engine.window.getWidth() - fmGameOver.stringWidth(gameOverText)) / 2;
        int gameOverY = Engine.window.getHeight() / 2 - fmGameOver.getHeight() / 2;

        g.setColor(Color.WHITE);
        g.drawString(gameOverText, gameOverX, gameOverY);

        ESC(g, gameOverY + fmGameOver.getHeight());
    }

    private void ESC(Graphics2D g, int gameOverBottomY) {
        if (isTextVisible) {
            g.setColor(Color.WHITE);
            g.setFont(escFont);
            FontMetrics fmEsc = g.getFontMetrics(escFont);
            String esc = "Press ESC to back to Geral";
            int escX = (Engine.window.getWidth() - fmEsc.stringWidth(esc)) / 2;
            int escY = gameOverBottomY + fmEsc.getHeight();
            g.drawString(esc, escX, escY);
        }
    }

    @Override
    public void dispose() {
    }
}