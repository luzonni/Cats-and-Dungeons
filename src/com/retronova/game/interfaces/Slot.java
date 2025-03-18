package com.retronova.game.interfaces;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.DrawString;
import com.retronova.engine.graphics.FontG;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.mouse.Mouse;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Slot {

    private final Font fontStack;
    private final InfoBox info;
    private final BufferedImage sprite;
    private final Rectangle bounds;
    private Item item;

    public Slot(int x, int y) {
        this.item = null;
        this.sprite = new SpriteSheet("ui", "slot", Configs.HudScale()).getSHEET();
        this.bounds = new Rectangle(x, y, this.sprite.getWidth(), this.sprite.getHeight());
        info = new InfoBox();
        this.fontStack = FontG.font(FontG.Septem, Configs.HudScale() * 8);
    }

    public void setPosition(int x, int y) {
        this.bounds.setLocation(x, y);
    }

    public Item item() {
        return this.item;
    }

    public Rectangle getBounds() {
        return this.bounds;
    }

    public boolean put(Item item) {
        if(isEmpty()) {
            this.item = item;
            return true;
        }else if(this.item.getID() == item.getID() && this.item instanceof Consumable consumable) {
            int otherItemStack = ((Consumable) item).getStack();
            consumable.setStack(consumable.getStack() + otherItemStack);
            return true;
        }
        return false;
    }

    public Item take() {
        Item caught = this.item;
        if(caught instanceof Consumable consumable) {
            int currentStack = consumable.getStack();
            consumable.setStack(currentStack-1);
            if(consumable.getStack() <= 0) {
                this.item = null;
            }
            return Item.build(consumable.getID(), 1);
        }
        this.item = null;
        return caught;
    }

    public Item takeAll() {
        Item caught = this.item;
        this.item = null;
        return caught;
    }

    public boolean isEmpty() {
        return this.item == null;
    }

    public void render(Graphics2D g) {
        g.drawImage(sprite, bounds.x, bounds.y, null);
        renderItem(g);
    }

    public void renderItem(Graphics2D g) {
        if(isEmpty())
            return;
        int overAnimPref;
        if(Mouse.on(getBounds())) {
            overAnimPref = 2;
        }else {
            overAnimPref = 0;
        }
        int x = bounds.x + overAnimPref;
        int y = bounds.y + overAnimPref;
        int width = bounds.width - overAnimPref*2;
        int height = bounds.height - overAnimPref*2;
        g.drawImage(item.getSprite(), x, y, width, height, null);
        if(item instanceof Consumable consumable) {
            String stack = String.valueOf(consumable.getStack());
            int wf = FontG.getWidth(stack, fontStack);
            int hf = FontG.getHeight(stack, fontStack);
            DrawString.draw(stack, fontStack, x + width - wf - Configs.HudScale(), y + height - hf - Configs.HudScale(), g);
        }
    }

    public void renderInfo(Graphics2D g) {
        if(Mouse.on(getBounds()) && !isEmpty()) {
            info.setValues(this.item);
            info.render(g);
        }
    }
}
