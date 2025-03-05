package com.retronova.game.hud;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class HUD implements Activity {

    private final BufferedImage frameBust;
    private final BufferedImage chains;
    private final BufferedImage bust;
    private final BufferedImage[] bottle;
    private int indexBottle;

    private int count;

    private BufferedImage vignette;

    private final Hotbar hotbar;

    public HUD(Player player) {
        this.hotbar = new Hotbar(player);
        SpriteSheet sheetBust = new SpriteSheet("ui","bust", Configs.HUDSCALE);
        SpriteSheet sheetBottle = new SpriteSheet("ui","lifebottle", Configs.HUDSCALE);
        int indexBust = 0;
        for(int i = 0; i < Player.TEMPLATES.length; i++) {
            if(player.getName().equals(Player.TEMPLATES[i].getName())) {
                indexBust = i;
            }
        }
        this.bust = sheetBust.getSpriteWithIndex(indexBust ,1);
        this.frameBust = sheetBust.getSpriteWithIndex(1,0);
        this.chains = sheetBust.getSpriteWithIndex(0,0);
        this.bottle = sheetBottle.getSprites(0);

    }

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
        hotbar.tick();
        count++;
        if(count > 5) {
            indexBottle++;
            if(indexBottle > bottle.length-1) {
                indexBottle = 0;
            }
            count = 0;
        }
    }

    @Override
    public void render(Graphics2D g) {
        vignette(g);
        bust(g);
        bottleLife(g);
        hotbar.render(g);
    }

    private void bust(Graphics2D g) {
        int x = Configs.MARGIN;
        int y = Configs.MARGIN;
        g.drawImage(this.chains, x, y - (this.frameBust.getHeight() - Configs.SCALE), null);
        g.drawImage(this.frameBust, x, y, null);
        g.drawImage(this.bust, x, y, null);
    }

    private void bottleLife(Graphics2D g) {
        Player player = Game.getPlayer();
        int x = Configs.MARGIN + this.frameBust.getWidth();
        int y = Configs.MARGIN;
        g.drawImage(bottle[indexBottle], x, y, Engine.window);
        int w = Configs.UISCALE * 36;
        int h = Configs.UISCALE * 6;
        int xx = x + bottle[indexBottle].getWidth();
        int yy = y + bottle[indexBottle].getHeight()/2 - h/2;
        g.setStroke(new BasicStroke(Configs.UISCALE * 2));
        g.drawImage(this.chains, xx + w/2 - chains.getWidth()/2, yy - this.chains.getHeight(), null);
        g.setColor(new Color(135, 35, 65));
        g.fillRect(xx, yy, w, h);
        g.setColor(new Color(190, 49, 68));
        double lifeWidth = w * (player.getLife() / player.getLifeSize());
        g.fillRect(xx, yy, (int)lifeWidth, h);
        g.setColor(new Color(9, 18, 44));
        g.drawRect(xx, yy, w, h);
    }

    private void vignette(Graphics2D g) {
        if(Configs.vignette)
            g.drawImage(vignette, 0, 0, null);
    }

    @Override
    public void dispose() {

    }
}
