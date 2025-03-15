package com.retronova.game.interfaces.status;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.game.items.Consumable;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Div {

    private final Player player;
    private final Rectangle bounds;
    private final BufferedImage frame;
    private final Font font;
    private int scroll;

    public Div(Player player, int width, int height) {
        this.player = player;
        this.bounds = new Rectangle(width, height);
        this.frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.font = FontG.font(FontG.Septem, Configs.HudScale() * 6);
    }

    public void setLocation(int x, int y) {
        this.bounds.setLocation(x, y);
    }

    public void tick() {
        renderOnFrame();
    }

    private void renderOnFrame() {
        Graphics2D g = (Graphics2D) this.frame.getGraphics();
        int WIDTH = this.frame.getWidth();
        int HEIGHT = this.frame.getHeight();
        g.setColor(new Color(0xe17564));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int x = 0;
        int y = scroll;
        int H = 0;
        List<Consumable> passives = player.getPassives();
        if(passives.isEmpty())
            return;
        for(int i = 0; i < passives.size(); i++) {
            Consumable passive = passives.get(i);
            String name = passive.getName();
            BufferedImage icon = passive.getSprite();
            int hr = icon.getHeight();
            g.drawImage(icon, x, y + hr*i, null);
            g.setFont(font);
            int hf = FontG.getHeight(name, font);
            g.setColor(Color.white);
            g.drawString(name, x + icon.getWidth() + Configs.HudScale()*2, y + (hr/2 + hf/2) + hr*i);
            H+=hr;
        }
        int s = Mouse.Scroll();
        int pref = passives.get(0).getSprite().getHeight()/2;
        if(s == -1) {
            System.out.println("up");
            if(scroll < 0)
                scroll+=pref;
        }else if(s == 1) {
            System.out.println("down");
            if(scroll*-1 < H - HEIGHT)
                scroll-=pref;
        }
    }

    public void render(Graphics2D g) {
        g.drawImage(this.frame, bounds.x, bounds.y, null);
    }




}
