package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontG;
import com.retronova.game.Game;

import java.awt.*;

public class Number extends Particle {

    private final int number;
    private final Font font;
    private int count;

    public Number(int number, double x, double y, double seconds) {
        super(x, y, seconds);
        this.number = number;
        this.font = FontG.font(FontG.Septem, Configs.GameScale() * 8);
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
        String value = String.valueOf(number);
        int wF = FontG.getWidth(value, font);
        int hF = FontG.getHeight(value, font);
        int x = (int)getX() - wF/2 - Game.C.getX();
        int y = (int)getY() - hF/2 - Game.C.getY();
        g.setFont(font);
        g.setColor(Color.black);
        g.drawString(value, x + Configs.GameScale(), y + Configs.GameScale());
        g.setColor(Color.white);
        g.drawString(value, x, y);
    }
}
