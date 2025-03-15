package com.retronova.game.interfaces.status;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class Status implements Activity {

    private final Player player;
    private final BufferedImage status;

    private final HashMap<String, Point> points;
    private final Font font;

    private Div frame;

    public Status(Player player) {
        this.player = player;
        this.status = new SpriteSheet("ui","status", Configs.HudScale()).getSHEET();
        this.font = FontG.font(FontG.Septem,Configs.HudScale() * 6);
        this.frame = new Div(player, 52 * Configs.HudScale(), 68 * Configs.HudScale());
        this.points = new HashMap<>();
        this.points.put("main", new Point());
        this.points.put("player", new Point());
        this.points.put("life", new Point());
        this.points.put("luck", new Point());
        refreshPositions();
    }

    private void refreshPositions() {
        int x = Engine.window.getWidth()/2 - status.getWidth()/2;
        int y = Engine.window.getHeight()/2 - status.getHeight()/2;
        int s = Configs.HudScale();
        this.points.get("main").setLocation(x, y);
        this.points.get("player").setLocation(x + 8*s, y + 9*s);
        this.points.get("life").setLocation(x + 40*s, y + 9*s);
        this.points.get("luck").setLocation(x + 40*s, y + 21*s);
        frame.setLocation(x + 78*s, y + 46*s);
    }

    @Override
    public void tick() {
        refreshPositions();
        frame.tick();
    }

    @Override
    public void render(Graphics2D g) {
        g.drawImage(status, points.get("main").x, points.get("main").y, null);
        g.drawImage(player.getSprite(0), points.get("player").x, points.get("player").y, 16 * Configs.HudScale(), 16 * Configs.HudScale(), Engine.window);
        renderString((int)player.getLife()+"/"+(int)player.getLifeSize(), points.get("life"), g);
        renderString((int)(player.getLuck()*100d)+"%", points.get("luck"), g);
        frame.render(g);
    }

    private void renderString(String value, Point p, Graphics2D g){
        g.setFont(font);
        int h = FontG.getHeight(value, font);
        int s = Configs.HudScale();
        g.setColor(Color.black);
        g.drawString(value, p.x + s, p.y + h + s);
        g.setColor(Color.white);
        g.drawString(value, p.x, p.y + h);
    }

    @Override
    public void dispose() {

    }
}
