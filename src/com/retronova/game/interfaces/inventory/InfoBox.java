package com.retronova.game.interfaces.inventory;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontG;

import java.awt.*;

class InfoBox {

    private final String[] values;
    private final Rectangle bounds;
    private final int padding;

    private final Font fontTitle, fontSpecs;

    InfoBox(String[] values) {
        this.values = values;
        this.padding = Configs.UiScale() * 5;
        this.fontTitle = FontG.font(Configs.UiScale() * 8);
        this.fontSpecs = FontG.font(Configs.UiScale() * 6);
        int height = padding*2 + FontG.getHeight(values[0], fontTitle);
        int lastWidth = padding*2 + FontG.getWidth(values[0], fontTitle);
        for(int i = 1; i < values.length; i++) {
            int currentWidth = padding*2 + FontG.getWidth(values[i], fontSpecs);
            if(currentWidth > lastWidth) {
                lastWidth = currentWidth;
            }
            height += FontG.getHeight(values[i], fontSpecs) + padding;
        }
        this.bounds = new Rectangle(lastWidth, height);
    }

    void render(int x, int y, Graphics2D g) {
        g.setColor(new Color(190, 49, 68));
        g.fillRect(x, y, this.bounds.width, this.bounds.height);
        g.setColor(new Color(135, 35, 65));
        g.setStroke(new BasicStroke(Configs.UiScale() *2));
        g.drawRect(x, y, this.bounds.width, this.bounds.height);
        g.setFont(fontTitle);
        g.setColor(Color.black);
        g.drawString(values[0], x + padding + Configs.UiScale(), y + FontG.getHeight(values[0], fontTitle)  + padding + Configs.UiScale());
        g.setColor(Color.white);
        g.drawString(values[0], x + padding, y + FontG.getHeight(values[0], fontTitle)  + padding);
        g.setFont(fontSpecs);
        int lastY = y + FontG.getHeight(values[0], fontTitle) + padding;
        for(int i = 1; i < values.length; i++) {
            lastY += FontG.getHeight(values[i-1], fontSpecs) + padding;
            g.setColor(Color.black);
            g.drawString(values[i], x + padding + Configs.UiScale(), lastY + Configs.UiScale());
            g.setColor(Color.white);
            g.drawString(values[i], x + padding, lastY);
        }
    }

}
