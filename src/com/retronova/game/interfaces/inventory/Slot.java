package com.retronova.game.interfaces.inventory;

import com.retronova.engine.Configs;
import com.retronova.game.items.Item;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

class Slot {

    private final BufferedImage sprite;
    private final Rectangle bounds;
    private Item item;

    Slot(int x, int y, Item item) {
        this.item = item;
        this.sprite = new SpriteSheet("ui", "slot", Configs.UISCALE).getSHEET();
        this.bounds = new Rectangle(x, y, this.sprite.getWidth(), this.sprite.getHeight());
    }

    Slot(int x, int y) {
        this.item = null;
        this.sprite = new SpriteSheet("ui", "slot", Configs.UISCALE).getSHEET();
        this.bounds = new Rectangle(x, y, this.sprite.getWidth(), this.sprite.getHeight());
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
        Item taked = this.item;
        this.item = null;
        return taked;
    }

    boolean isEmpty() {
        return this.item == null;
    }

    void render(Graphics2D g) {
        g.drawImage(sprite, bounds.x, bounds.y, null);
        if(!isEmpty())
            g.drawImage(item.getSprite(), bounds.x, bounds.y, sprite.getWidth(), sprite.getHeight(), null);
    }
}
