package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.interfaces.shared.GumPanel;
import com.retronova.game.items.Item;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.furniture.GumMachine;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GumInterface implements Activity {

    private final BufferedImage gumInterface;
    private final BufferedImage[] buttonSprite;
    private final Point ui_position;
    private final Font font;
    private final Point pricePosition;

    private final Slot slot;
    private final Rectangle button;
    private final GumMachine gum;
    private int indexButton;

    private final Item[] awards;
    private final GumPanel gumPanel;

    private Color priceColor;
    private int countPriceColor;

    public GumInterface(GumMachine gum, Item[] awards) {
        this.gum = gum;
        int s = Configs.HudScale();
        this.gumInterface = SpriteSheet.getSprite("ui","bum_ui", s);
        SpriteSheet sheet = new SpriteSheet("ui", "button_bum", s);
        this.buttonSprite = new BufferedImage[]{sheet.getSpriteWithIndex(0, 0), sheet.getSpriteWithIndex(1, 0)};
        int x = Engine.window.getWidth()/2 - this.gumInterface.getWidth()/2;
        int y = Engine.window.getHeight()/2 - this.gumInterface.getHeight()/2;
        this.pricePosition = new Point(x + 16*s, y + 77*s);
        this.font = FontG.font(FontG.Septem, Configs.HudScale()*8);
        this.ui_position = new Point(x, y);
        this.slot = new Slot(x + 50*s, y + 62*s);
        this.button = new Rectangle(x + 76*s, y + 70*s, 16*s, 16*s);
        this.awards = awards;
        this.gumPanel = loadPanel(s);
        this.priceColor = Color.white;
    }

    private GumPanel loadPanel(int s) {
        BufferedImage[] sprites = new BufferedImage[awards.length];
        for(int i = 0; i < sprites.length; i++) {
            sprites[i] = awards[i].getSprite();
        }
        Rectangle bounds = new Rectangle(ui_position.x + 7*s, ui_position.y + 10*s, 103*s, 40*s);
        return new GumPanel(bounds, sprites);
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        gumPanel.tick();
        if(this.priceColor != Color.white) {
            countPriceColor++;
            if (countPriceColor > 30) {
                countPriceColor = 0;
                this.priceColor = Color.white;
            }
        }
        if(Mouse.on(this.button)) {
            this.indexButton = 1;
            if(Mouse.click(Mouse_Button.LEFT) && !gumPanel.opened()) {
                if(player.getMoney() >= gum.getPrice()) {
                    gumPanel.openAward();
                    player.setMoney(player.getMoney() - gum.getPrice());
                }else {
                    this.priceColor = new Color(0xe17564);
                }
            }
        }else {
            this.indexButton = 0;
        }
        if(gumPanel.animFinish() && this.slot.isEmpty() && awards != null) {
            this.slot.put(awards[Engine.RAND.nextInt(awards.length)]);
        }
        if(Mouse.clickOn(Mouse_Button.LEFT, this.slot.getBounds()) && !this.slot.isEmpty()) {
            Item award = this.slot.takeAll();
            Game.getPlayer().getInventory().give(award);
            Game.getInter().close();
            gum.disappear();
        }
    }

    @Override
    public void render(Graphics2D g) {
        gumPanel.render(g);
        g.drawImage(this.gumInterface, this.ui_position.x, this.ui_position.y, null);
        slot.render(g);
        BufferedImage buttonSprite = SpriteSheet.flip(this.buttonSprite[indexButton], this.gumPanel.opened() ? -1 : 1, 1);
        g.drawImage(buttonSprite, this.button.x, this.button.y, null);
        renderPrice(g);
    }

    private void renderPrice(Graphics2D g) {
        int x = this.pricePosition.x;
        int y = this.pricePosition.y;
        g.setFont(font);
        g.setColor(new Color(0x872341));
        g.drawString("$"+gum.getPrice(), x + Configs.HudScale(), y + Configs.HudScale());
        g.setColor(priceColor);
        g.drawString("$"+gum.getPrice(), x, y);
    }

    @Override
    public void dispose() {

    }

}
