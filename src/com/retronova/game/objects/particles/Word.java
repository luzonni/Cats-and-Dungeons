package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontG;
import com.retronova.game.Game;

import java.awt.*;

public class Word extends Particle {

    private final String value;
    private final Font font;
    private final Rectangle boundsFont;
    private int count;

    public Word(String value, double x, double y, double seconds) {
        super(x, y, seconds);
        this.value = value;
        this.font = FontG.font(FontG.Septem, Configs.GameScale() * 8);
        int wF = FontG.getWidth(value, font);
        int hF = FontG.getHeight(value, font);
        boundsFont = new Rectangle(wF, hF);
        setX(getX() - boundsFont.width/2d);
        setY(getY() - boundsFont.height/2d);
        double scaleW = (double) boundsFont.width / getWidth();
        double scaleH = (double) boundsFont.height / getHeight();
        setWidth(scaleW);
        setHeight(scaleH);
    }

    @Override
    public void tick() {
        count++;
        if (count >= getSeconds()) {
            Game.getMap().remove(this);
        }
        this.setY(getY() - 0.25d * Configs.GameScale());
    }

    @Override
    public void render(Graphics2D g) {
        String value = this.value;
        int x = (int)getX();
        int y = (int)getY() + boundsFont.height;
        g.setFont(font);
        g.setColor(Color.black);
        g.drawString(value, x + Configs.GameScale(), y + Configs.GameScale());
        g.setColor(Color.white);
        g.drawString(value, x, y);
    }
}
