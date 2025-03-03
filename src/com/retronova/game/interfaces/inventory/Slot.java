package com.retronova.game.interfaces.inventory;

import com.retronova.engine.Configs;
import com.retronova.game.items.Item;
import com.retronova.graphics.SpriteSheet;
import com.retronova.inputs.mouse.Mouse;

import java.awt.*;
import java.awt.image.BufferedImage;

class Slot {

    private final BufferedImage sprite;
    private final Rectangle bounds;
    private Item item;


    Slot(int x, int y) {
        this.item = null;
        this.sprite = new SpriteSheet("ui", "slot", Configs.UISCALE).getSHEET();
        this.bounds = new Rectangle(x, y, this.sprite.getWidth(), this.sprite.getHeight());
    }

    void setPosition(int x, int y) {
        this.bounds.setLocation(x, y);
    }

    Item item() {
        return this.item;
    }

    Rectangle getBounds() {
        return this.bounds;
    }

    boolean put(Item item) {
        if(isEmpty()) {
            this.item = item;
            return true;
        }
        return false;
    }

    public Item take() {
        Item caught = this.item;
        this.item = null;
        return caught;
    }

    boolean isEmpty() {
        return this.item == null;
    }

    void render(Graphics2D g) {
        g.drawImage(sprite, bounds.x, bounds.y, null);
        if(!isEmpty())
            renderItem(g);
    }

    private void renderItem(Graphics2D g) {
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

    }
}
