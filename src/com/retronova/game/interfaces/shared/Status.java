package com.retronova.game.interfaces.shared;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawString;
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

    private final Frame frame;

    public Status(Player player) {
        this.player = player;
        this.status = new SpriteSheet("ui","status", Configs.HudScale()).getSHEET();
        this.font = FontG.font(FontG.Septem,Configs.HudScale() * 8);
        this.frame = new Frame(player, 52 * Configs.HudScale(), 68 * Configs.HudScale());
        this.points = new HashMap<>();
        refreshPositions();
    }

    private void setLocation(String name, int x, int y) {
        if(!this.points.containsKey(name)) {
            this.points.put(name, new Point());
        }
        this.points.get(name).setLocation(x, y);
    }

    private void refreshPositions() {
        int x = Engine.window.getWidth()/2 - status.getWidth()/2;
        int y = Engine.window.getHeight()/2 - status.getHeight()/2;
        int s = Configs.HudScale();
        setLocation("main", x, y);
        setLocation("player", x + 8*s, y + 8*s);
        setLocation("life", x + 40*s, y + 7*s);
        setLocation("luck", x + 40*s, y + 20*s);
        setLocation("level", x + 91*s, y + 7*s);
        setLocation("money", x + 91*s, y + 20*s);
        setLocation("damage", x + 23*s, y + 49*s);
        setLocation("resistence", x + 23*s, y + 63*s);
        setLocation("range", x + 23*s, y + 76*s);
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
        renderString(String.valueOf(player.getLevel()), points.get("level"), g);
        renderString("$ "+player.getMoney(), points.get("money"), g);
        renderString(String.valueOf(player.getDamage()), points.get("damage"), g);
        renderString(String.valueOf(player.getDamage()), points.get("resistence"), g);
        renderString(String.valueOf(player.getRange()), points.get("range"), g);
        frame.render(g);
    }

    private void renderString(String value, Point p, Graphics2D g){
        DrawString.draw(value, font, p, g);
    }

    @Override
    public void dispose() {

    }
    
}
