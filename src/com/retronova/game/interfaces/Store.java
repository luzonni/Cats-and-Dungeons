package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.StoreException;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Store implements Activity {

    private final BufferedImage store;
    private final Point positionStore;
    private final Slot[] slots;
    private int indexSelected = -1;
    private final int[] prices;

    private final Rectangle buttonBuy;


    public Store(Item[] items, int[] prices) {
        if(items.length != prices.length) {
            throw new StoreException("A quantidade de items não bate com a quantidade de preços");
        }
        this.prices = prices;
        this.store = new SpriteSheet("ui", "store", Configs.HudScale()).getSHEET();
        this.slots = new Slot[21];
        this.positionStore = new Point();
        this.buttonBuy = new Rectangle(23 * Configs.HudScale(), 14 * Configs.HudScale());
        for(int i = 0; i < slots.length; i++) {
            this.slots[i] = new Slot(0, 0);
            if(i > items.length-1 || items[i] == null)
                continue;
            if(items[i] instanceof Consumable consumable) {
                this.slots[i].put(Item.build(items[i].getID(), consumable.getStack()));
            }else {
                this.slots[i].put(Item.build(items[i].getID()));
            }
        }
        refreshPosition();
    }

    private void refreshPosition() {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        this.positionStore.setLocation(w/2 - store.getWidth()/2, h/2 - store.getHeight()/2);
        this.buttonBuy.setLocation(positionStore.x + 95 * Configs.HudScale(), positionStore.y + 72 * Configs.HudScale());
        for(int y = 0; y < 3; y++)
            for(int x = 0; x < 7; x++) {
                Slot slot = this.slots[x+y*7];
                int xx = positionStore.x + 6 * Configs.HudScale() + x * slot.getBounds().width;
                int yy = positionStore.y + 6 * Configs.HudScale() + y * slot.getBounds().height;
                slot.setPosition(xx, yy);
            }
    }

    @Override
    public void tick() {
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if(Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds()) && !slot.isEmpty()) {
                this.indexSelected = i;
                break;
            }
        }
        Player player = Game.getPlayer();
        if(indexSelected != -1 &&  Mouse.clickOn(Mouse_Button.LEFT, buttonBuy) && player.getMoney() >= prices[indexSelected]) {
            player.getInventory().give(slots[indexSelected].take());
            player.setMoney(player.getMoney() - prices[indexSelected]);
            if (slots[indexSelected].isEmpty()) {
                indexSelected = -1;
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        refreshPosition();
        renderStore(g);
        renderSlots(g);
        renderPriceSelected(g);
        renderButtonBuy(g);
        renderInfo(g);
    }

    private void renderStore(Graphics2D g) {
        g.drawImage(store, positionStore.x, positionStore.y, null);
    }

    private void renderSlots(Graphics2D g) {
        Rectangle rec = null;
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            slot.render(g);
            if(i == indexSelected) {
                rec = slot.getBounds();
            }
        }
        if(rec == null)
            return;
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(Configs.HudScale()));
        g.drawRect(rec.x, rec.y, rec.width, rec.height);
    }

    private void renderPriceSelected(Graphics2D g) {
        if(indexSelected == -1)
            return;
        int x = positionStore.x + 18 * Configs.HudScale();
        int y = positionStore.y + 82 * Configs.HudScale();
        Font font = FontG.font(FontG.Game,Configs.HudScale() * 8);
        String value = prices[indexSelected] + "/" + Game.getPlayer().getMoney();
        g.setFont(font);
        g.setColor(Color.black);
        g.drawString(value, x + Configs.HudScale(), y + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(value, x, y);
    }

    public void renderButtonBuy(Graphics2D g) {
        if(!Mouse.on(buttonBuy))
            return;
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(Configs.HudScale()));
        g.drawRect(buttonBuy.x, buttonBuy.y, buttonBuy.width, buttonBuy.height);
    }

    private void renderInfo(Graphics2D g) {
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if (Mouse.on(slot.getBounds()) && !slot.isEmpty()) {
                slot.renderInfo(g);
            }
        }
    }

    @Override
    public void dispose() {

    }
}
