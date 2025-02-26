package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.FontG;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class HUD implements Activity {

    private final int padding;
    private final BufferedImage HUD;
    private final BufferedImage[] busts;
    private int indexBust;
    private BufferedImage vignette;

    HUD(Player player) {
        this.padding = Engine.MARGIN;
        SpriteSheet sp = new SpriteSheet("ui","HUD", Engine.HUDSCALE);
        this.HUD = sp.getSHEET().getSubimage(0, 0, 64 * Engine.HUDSCALE, 16 * Engine.HUDSCALE);
        this.busts = new BufferedImage[Player.TEMPLATES.length];
        for(int i = 0; i < busts.length; i++) {
            this.busts[i] = sp.getSprite(i, 1);
            if(player.getName().equals(Player.TEMPLATES[i].getName())) {
                this.indexBust = i;
            }
        }
    }

    /**
     * Esta função serve para criar uma imagem do tamanho da tela, toda vez que a janela for redimensionada.
     * Isso serve como otimização, pois desenhar um degrade radial usando canal alfa é pesado, então esta função
     * cria a imagem para so depois renderiza-la.
     */
    private void createVignette() {
        if(vignette == null || vignette.getWidth() != Engine.window.getWidth() || vignette.getHeight() != Engine.window.getHeight()) {
            int Size = Engine.window.getWidth();
            BufferedImage CurrentVignette = new BufferedImage(Size, Size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = getRadialGraphics2D(Size, CurrentVignette);
            graphics.fillRect(0, 0, Size, Size);
            vignette = new BufferedImage(Engine.window.getWidth(), Engine.window.getHeight(), BufferedImage.TYPE_INT_ARGB);
            vignette.getGraphics().drawImage(CurrentVignette, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }
    }

    private static Graphics2D getRadialGraphics2D(float Size, BufferedImage CurrentVignette) {
        Point2D center = new Point2D.Float(Size /2, Size /2);
        float radius = Size /1.2f;
        Point2D focus = new Point2D.Float(Size /2, Size /2);
        float[] dist = {0.2f, 0.8f};
        Color[] colors = {new Color(0, 0, 0 , 0), new Color(0, 0, 0, 100)};
        RadialGradientPaint p = new RadialGradientPaint(center, radius, focus, dist, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        Graphics2D graphics = (Graphics2D) CurrentVignette.getGraphics();
        graphics.setPaint(p);
        return graphics;
    }

    @Override
    public void tick() {
        createVignette();
    }

    @Override
    public void render(Graphics2D g) {
        vignette(g);
        drawPlayerLife(padding, padding, g);
    }

    private void drawPlayerLife(int x, int y,Graphics2D g) {
        Player player = Game.getPlayer();
        g.drawImage(HUD, x, y, null);
        int prefX = x + 15 * Engine.HUDSCALE;
        int prefY = y + 7 * Engine.HUDSCALE;
        int width = 47 * Engine.HUDSCALE;
        int height = 2 * Engine.HUDSCALE;
        double lifeLength = width * ( 1 - (player.getLife() / player.getLifeSize()));
        if(lifeLength < 0)
            lifeLength = 0;
        g.setColor(Color.black);
        g.fillRect(prefX + width - (int)lifeLength, prefY, (int)lifeLength, height);
        g.drawImage(this.busts[indexBust], x, y, null);
        Font font = FontG.font(Engine.HUDSCALE*4);
        String value = "" + (int)player.getLife() + "/" + (int)player.getLifeSize();
        g.setFont(font);
        g.setColor(Color.white);
        g.drawString(value, x + 16 * Engine.HUDSCALE, y + 14 * Engine.HUDSCALE);
    }

    private void vignette(Graphics2D g) {
        if(Engine.vignette)
            g.drawImage(vignette, 0, 0, null);
    }

    @Override
    public void dispose() {

    }
}
