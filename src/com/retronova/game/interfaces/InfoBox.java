package com.retronova.game.interfaces;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.game.items.Item;

import java.awt.*;

public class InfoBox {

    private String[] values;
    private Rectangle bounds;
    private final int padding;

    private final Font fontTitle, fontSpecs;

    public InfoBox() {
        this.padding = Configs.HudScale() * 5;
        this.fontTitle = FontG.font(FontG.Septem,Configs.HudScale() * 10);
        this.fontSpecs = FontG.font(FontG.Septem,Configs.HudScale() * 8);
    }

    public void setValues(Item item) {
        if(item == null)
            return;
        String[] values = new String[item.getSpecifications().length + 1];
        values[0] = item.getName();
        if(this.values != null && this.values[0].equals(values[0])) {
            return;
        }
        for (int j = 1; j < values.length; j++) {
            values[j] = "- " + item.getSpecifications()[j - 1];
        }
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
        this.values = values;
    }

    public boolean isEmpty() {
        return this.values == null || this.values.length == 0;
    }

    public void clean() {
        this.values = null;
    }

    public void render(Graphics2D g) {
        if(isEmpty())
            return;
        int x = Mouse.getX() + 16;
        int y = Mouse.getY() + 16;
        g.setColor(new Color(190, 49, 68));
        g.fillRect(x, y, this.bounds.width, this.bounds.height);
        g.setColor(new Color(135, 35, 65));
        g.setStroke(new BasicStroke(Configs.HudScale() *2));
        g.drawRect(x, y, this.bounds.width, this.bounds.height);
        g.setFont(fontTitle);
        g.setColor(Color.black);
        g.drawString(values[0], x + padding + Configs.HudScale(), y + FontG.getHeight(values[0], fontTitle)  + padding + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(values[0], x + padding, y + FontG.getHeight(values[0], fontTitle)  + padding);
        g.setFont(fontSpecs);
        int lastY = y + FontG.getHeight(values[0], fontTitle) + padding;
        for(int i = 1; i < values.length; i++) {
            lastY += FontG.getHeight(values[i-1], fontSpecs) + padding;
            g.setColor(Color.black);
            g.drawString(values[i], x + padding + Configs.HudScale(), lastY + Configs.HudScale());
            g.setColor(Color.white);
            g.drawString(values[i], x + padding, lastY);
        }
    }

}
