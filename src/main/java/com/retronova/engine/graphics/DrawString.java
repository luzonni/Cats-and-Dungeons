package com.retronova.engine.graphics;

import com.retronova.engine.Configs;

import java.awt.*;

public class DrawString {

    public static void draw(String value, Font font, int x, int y, Graphics2D g) {
        g.setFont(font);
        int h = FontHandler.getHeight(value, font);
        int s = Configs.HudScale();
        g.setColor(Color.black);
        g.drawString(value, x + s, y + h + s);
        g.setColor(Color.white);
        g.drawString(value, x, y + h);
    }

    public static void draw(String value, Font font, Point p, Graphics2D g) {
        g.setFont(font);
        int h = FontHandler.getHeight(value, font);
        int s = Configs.HudScale();
        g.setColor(Color.black);
        g.drawString(value, p.x + s, p.y + h + s);
        g.setColor(Color.white);
        g.drawString(value, p.x, p.y + h);
    }

}
