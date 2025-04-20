package com.retronova.game.interfaces.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.game.interfaces.InfoBox;
import com.retronova.game.items.Consumable;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

class Frame {

    private final Player player;
    private final Rectangle bounds;
    private final BufferedImage frame;
    private final Font fontName;
    private final Font fontStack;
    private int scroll;
    private final InfoBox info;

    public Frame(Player player, int width, int height) {
        this.player = player;
        this.bounds = new Rectangle(width, height);
        this.frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.fontName = FontG.font(FontG.Septem, Configs.HudScale() * 8);
        this.fontStack = FontG.font(FontG.Septem, Configs.HudScale() * 6);
        this.info = new InfoBox();
    }

    public void setLocation(int x, int y) {
        this.bounds.setLocation(x, y);
    }

    public void tick() {
        this.info.clean();
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
        Rectangle rec = new Rectangle(x, y, WIDTH, 16 * Configs.HudScale());
        int H = 0;
        List<Consumable> passives = player.getPassives();
        if(passives.isEmpty())
            return;
        for(int i = 0; i < passives.size(); i++) {
            Consumable passive = passives.get(i);
            rec.setLocation(x, y + rec.height*i);
            renderPassive(passive, rec, g);
            H+=rec.height;
        }
        int s = Mouse.Scroll();
        int pref = rec.height/2;
        if(s == -1) {
            if(scroll < 0)
                scroll+=pref;
        }else if(s == 1) {
            if(scroll*-1 < H - HEIGHT)
                scroll-=pref;
        }
    }

    private void renderPassive(Consumable passive, Rectangle rec, Graphics2D g) {
        String name = passive.getName();
        BufferedImage icon = passive.getSprite();
        int size = rec.height - 4;
        g.drawImage(icon, rec.x, rec.y, size, size, null);
        g.setFont(fontName);
        int hf = FontG.getHeight(name, fontName);
        g.setColor(Color.black);
        g.drawString(name, rec.x + size + Configs.HudScale()*2 + Configs.HudScale()/2, rec.y + (rec.height/2 + hf/2) + Configs.HudScale()/2);
        g.setColor(Color.white);
        g.drawString(name, rec.x + size + Configs.HudScale()*2, rec.y + (rec.height/2 + hf/2));
        String stack = passive.getStack()+"x";
        int hfs = FontG.getHeight(stack, fontStack);
        g.setFont(fontStack);
        g.setColor(Color.black);
        g.drawString(stack, rec.x + Configs.HudScale()/2, rec.y + rec.height - hfs/2 + Configs.HudScale()/2);
        g.setColor(Color.white);
        g.drawString(stack, rec.x, rec.y + rec.height - hfs/2);
        g.setColor(new Color(0xbe3144));
        g.setStroke(new BasicStroke(Configs.HudScale()));
        g.drawLine(rec.x, rec.y + rec.height, rec.x + rec.width, rec.y + rec.height);
        if(Mouse.on(bounds.x + rec.x, bounds.y + rec.y, rec.width, rec.height)) {
            info.setValues(passive);
        }
    }

    public void render(Graphics2D g) {
        g.drawImage(this.frame, bounds.x, bounds.y, null);
        info.render(g);
    }

}
