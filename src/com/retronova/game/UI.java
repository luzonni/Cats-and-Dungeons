package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.FontG;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class UI implements Activity {

    private int padding;
    private Player player;
    private BufferedImage HUD;
    private BufferedImage[] busts;
    private int indexBust;

    UI(Player player) {
        this.padding = Engine.UISCALE * 3;
        this.player = player;
        SpriteSheet sp = new SpriteSheet("ui","HUD", Engine.UISCALE);
        this.HUD = sp.getSHEET().getSubimage(0, 0, 64 * Engine.UISCALE, 16 * Engine.UISCALE);
        this.busts = new BufferedImage[Game.PLAYERS.length];
        for(int i = 0; i < busts.length; i++) {
            this.busts[i] = sp.getSprite(i, 1);
            if(player.getName().equals(Game.PLAYERS[i].getName())) {
                this.indexBust = i;
            }
        }
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {
        drawPlayerLife(padding, padding, g);
    }

    private void drawPlayerLife(int x, int y,Graphics2D g) {
        g.drawImage(HUD, x, y, null);
        int prefX = x + 15 * Engine.UISCALE;
        int prefY = y + 7 * Engine.UISCALE;
        int width = 47 * Engine.UISCALE;
        int height = 2 * Engine.UISCALE;
        double lifeLength = width * ( 1 - (player.getLife() / player.getLifeSize()));
        if(lifeLength < 0)
            lifeLength = 0;
        g.setColor(Color.black);
        g.fillRect(prefX + width - (int)lifeLength, prefY, (int)lifeLength, height);
        g.drawImage(this.busts[indexBust], x, y, null);
        Font font = FontG.font(Engine.UISCALE*4);
        String value = "" + (int)player.getLife() + "/" + (int)player.getLifeSize();
        g.setFont(font);
        g.setColor(Color.white);
        g.drawString(value, x + 16 * Engine.UISCALE, y + 14 * Engine.UISCALE);
    }

    @Override
    public void dispose() {

    }
}
