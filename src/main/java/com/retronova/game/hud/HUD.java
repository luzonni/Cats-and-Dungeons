package com.retronova.game.hud;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.engine.graphics.SpriteHandler;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class HUD implements Activity {

    private BufferedImage frameBust;
    private BufferedImage chains;
    private BufferedImage bust;
    private BufferedImage[] bottle;
    private int indexBottle;

    private int count;

    private BufferedImage vignette;

    private final Hotbar hotbar;
    private final WaveDisplay waveDisplay;
    private final BarraDeChefe barraDeChefe = new BarraDeChefe();

    private final Player player;
    /** Escala do HUD com que os sprites atuais foram recortados. */
    private int escalaAplicada = -1;

    public HUD(Player player) {
        this.player = player;
        this.hotbar = new Hotbar(player);
        this.waveDisplay = new WaveDisplay();
        recriar();
    }

    /**
     * Refatia busto e frascos na escala corrente.
     *
     * Como na hotbar, sao recortes de folhas ja escaladas: trocar so a imagem nao
     * resolve, tem de refazer os recortes quando "HUD size" muda.
     */
    private void recriar() {
        this.escalaAplicada = Configs.HudScale();
        int s = escalaDoRetrato();
        SpriteHandler sheetBust = new SpriteHandler("ui", "bust", s);
        SpriteHandler sheetBottle = new SpriteHandler("ui", "lifebottle", s);
        int indexBust = 0;
        for (int i = 0; i < Player.TEMPLATES.length; i++) {
            if (player.getName().equals(Player.TEMPLATES[i].getName())) {
                indexBust = i;
            }
        }
        this.bust = sheetBust.getSpriteWithIndex(indexBust, 1);
        this.frameBust = sheetBust.getSpriteWithIndex(1, 0);
        this.chains = sheetBust.getSpriteWithIndex(0, 0);
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
        if (escalaAplicada != Configs.HudScale()) {
            recriar();
        }
        createVignette();
        hotbar.tick();
        waveDisplay.tick();
        barraDeChefe.tick();
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
        renderXpBar(g);
        waveDisplay.render(g);
        barraDeChefe.render(g);
        hotbar.render(g);
    }

    private void bust(Graphics2D g) {
        int x = Configs.Margin();
        int y = Configs.Margin();
        g.drawImage(this.chains, x, y - (this.frameBust.getHeight() - escalaDoRetrato()), null);
        g.drawImage(this.frameBust, x, y, null);
        g.drawImage(this.bust, x, y, null);
    }

    private void bottleLife(Graphics2D g) {
        Player player = Game.getPlayer();
        int x = Configs.Margin() + this.frameBust.getWidth();
        int y = Configs.Margin();
        g.drawImage(bottle[indexBottle], x, y, Engine.window);
        int s = escalaDoRetrato();
        int w = s * 36;
        int h = s * 5;
        int xx = x + bottle[indexBottle].getWidth();
        int yy = y + bottle[indexBottle].getHeight()/2;
        g.setStroke(new BasicStroke(s));
        g.drawImage(this.chains, xx + w/2 - chains.getWidth()/2, yy - this.chains.getHeight(), null);
        g.setColor(new Color(135, 35, 65));
        g.fillRect(xx, yy, w, h);
        g.setColor(new Color(190, 49, 68));
        double lifeWidth = w * (player.getLife() / player.getLifeSize());
        g.fillRect(xx, yy, (int)lifeWidth, h);
        g.setColor(new Color(9, 18, 44));
        g.drawRect(xx, yy, w, h);
        // O número em cima da barra: a barra diz "quanto", o número diz "quanto mesmo".
        String vida = (int) Math.ceil(Math.max(0, player.getLife())) + "/" + (int) player.getLifeSize();
        Font fonte = FontHandler.font(FontHandler.Game, Configs.HudScale() * 8);
        int fw = FontHandler.getWidth(vida, fonte);
        int fh = FontHandler.getHeight(vida, fonte);
        TextFx.draw(g, vida, fonte, xx + w / 2 - fw / 2, yy + h / 2 + fh / 2, Palette.TEXT, Configs.HudScale());
    }

    /**
     * Retrato e vida no dobro da escala da HUD.
     *
     * Na escala normal o gato e a barra ficavam do tamanho de um icone, e sao as
     * duas coisas que o jogador mais precisa ler de relance no meio da horda.
     */
    private static int escalaDoRetrato() {
        return Configs.HudScale() * 2;
    }

    private void renderXpBar(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Player player = Game.getPlayer();
        int size = Configs.HudScale() * 15;
        int x = Engine.window.getWidth() - size - Configs.Margin();
        int y = Configs.Margin();
        Font font = FontHandler.font(FontHandler.Game,Configs.HudScale() * 8);
        String value = "" + player.getLevel();
        int wf = FontHandler.getWidth(value, font);
        int hf = FontHandler.getHeight(value, font);
        g2.setStroke(new BasicStroke(Configs.HudScale()));
        g2.setFont(font);
        g2.setColor(new Color(20, 64, 96));
        g2.fillOval(x - Configs.HudScale(), y - Configs.HudScale(), size + Configs.HudScale() *2, size + Configs.HudScale() *2);
        g2.setColor(new Color(160, 200, 120));
        int percent = (int)((double)360 * (player.getXp() / player.getXpLength()));
        g2.fillArc(x - Configs.HudScale(), y - Configs.HudScale(), size + Configs.HudScale() *2, size + Configs.HudScale() *2, 90, percent);
        g2.setColor(new Color(9, 18, 44));
        g2.fillOval(x, y, size, size);
        // O circulo e suavizado, o numero nao: o mesmo acabamento do texto da GUI.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        TextFx.draw(g2, value, x + size/2 - wf/2, y + size/2 + hf/2, Palette.TEXT, Configs.HudScale());
        g2.dispose();
    }

    private void vignette(Graphics2D g) {
        if(Configs.Vignette())
            g.drawImage(vignette, 0, 0, null);
    }

    @Override
    public void dispose() {

    }
}
