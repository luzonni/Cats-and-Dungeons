package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontG;
import com.retronova.game.map.GameMap;

import java.awt.*;

public class Plate extends  Furniture {

    private final String content;
    private final Font font;

    public Plate(int ID, double x, double y, String content) {
        super(ID, x, y, 1000);
        loadSprites("plate");
        this.content = content;
        this.font = FontG.font(FontG.Septem, Configs.GameScale()*6);
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        if(GameMap.mouseOnRect(this.getBounds())) {
            renderMessage(g);
        }
    }

    private void renderMessage(Graphics2D g) {
        int padding = Configs.GameScale() * 2;
        int wC = FontG.getWidth(content, font);
        int hC = FontG.getHeight(content, font);
        int width = wC + padding*2;
        int height = hC + padding*2;
        int x = (int)getX() + getWidth()/2 - width/2;
        int y = (int)getY() + Configs.GameScale()*3;

        g.setColor(new Color(0x663931));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(0x000000));
        g.setStroke(new BasicStroke(Configs.GameScale()));
        g.drawRect(x, y, width, height);
        int xT = x + padding;
        int yT = y + height - hC + padding;
        g.setFont(this.font);
        g.setColor(new Color(0x000000));
        g.drawString(content, xT + Configs.GameScale(), yT + Configs.GameScale());
        g.setColor(new Color(0xffffff));
        g.drawString(content, xT, yT);
    }

}
