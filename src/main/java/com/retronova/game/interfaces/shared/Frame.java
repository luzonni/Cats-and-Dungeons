package com.retronova.game.interfaces.shared;

import com.retronova.engine.graphics.Palette;
import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.UiSprite;
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
    private BufferedImage frame;
    private int escalaDoQuadro;
    private int scroll;
    private final InfoBox info;

    /** Medidas logicas do quadro, em pixels de sprite. Multiplicam pela escala. */
    private static final int LARGURA = 52, ALTURA = 68;

    public Frame(Player player) {
        this.player = player;
        this.bounds = new Rectangle();
        this.info = new InfoBox();
        recriar();
    }

    /**
     * Refaz a tela interna quando a escala do HUD muda.
     *
     * O quadro dos passivos e desenhado fora, numa imagem propria, entao ele nao
     * pode ser criado uma vez so: ficava do tamanho antigo enquanto o painel em
     * volta ja tinha crescido.
     */
    private void recriar() {
        int s = Configs.HudScale();
        this.escalaDoQuadro = s;
        int w = LARGURA * s, h = ALTURA * s;
        this.bounds.setSize(w, h);
        this.frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    public void setLocation(int x, int y) {
        this.bounds.setLocation(x, y);
    }

    public void tick() {
        if (escalaDoQuadro != Configs.HudScale()) {
            recriar();
        }
        this.info.clean();
        renderOnFrame();
    }

    private void renderOnFrame() {
        Graphics2D g = (Graphics2D) this.frame.getGraphics();
        int WIDTH = this.frame.getWidth();
        int HEIGHT = this.frame.getHeight();
        g.setColor(Palette.LIGHT);
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
        Font fontName = UiSprite.fonte(FontHandler.Septem, 8f);
        g.setFont(fontName);
        int hf = FontHandler.getHeight(name, fontName);
        g.setColor(Color.black);
        g.drawString(name, rec.x + size + Configs.HudScale()*2 + Configs.HudScale()/2, rec.y + (rec.height/2 + hf/2) + Configs.HudScale()/2);
        g.setColor(Color.white);
        g.drawString(name, rec.x + size + Configs.HudScale()*2, rec.y + (rec.height/2 + hf/2));
        String stack = passive.getStack()+"x";
        Font fontStack = UiSprite.fonte(FontHandler.Septem, 6f);
        int hfs = FontHandler.getHeight(stack, fontStack);
        g.setFont(fontStack);
        g.setColor(Color.black);
        g.drawString(stack, rec.x + Configs.HudScale()/2, rec.y + rec.height - hfs/2 + Configs.HudScale()/2);
        g.setColor(Color.white);
        g.drawString(stack, rec.x, rec.y + rec.height - hfs/2);
        g.setColor(Palette.MAIN);
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
