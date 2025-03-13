package com.retronova.game.interfaces.store;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.StoreException;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Slot;
import com.retronova.game.items.Item;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Store implements Activity {

    private BufferedImage store;
    private Point positionStore;
    private Point[] slotsPositions = {
            new Point(36, 10),
            new Point(54, 10),
            new Point(72, 10)
    };
    private Slot[] slots;
    private Slot selected;
    private int indexSelected = -1;
    private int[] prices;

    private Rectangle buttonBuy;


    public Store(Item[] items, int[] prices) {
        if(items.length != prices.length) {
            throw new StoreException("A quantidade de items não bate com a quantidade de preços");
        }
        this.prices = prices;
        this.store = new SpriteSheet("ui", "store", Configs.UISCALE).getSHEET();
        this.slots = new Slot[3];
        this.positionStore = new Point();
        this.buttonBuy = new Rectangle(20 * Configs.UISCALE, 10 * Configs.UISCALE);
        for(int i = 0; i < slots.length; i++) {
            this.slots[i] = new Slot(0, 0);
            this.slots[i].put(items[i]);
        }
        this.selected = new Slot(0, 0);
        refreshPosition();
    }

    private void refreshPosition() {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        this.positionStore.setLocation(w/2 - store.getWidth()/2, h/2 - store.getHeight()/2);
        this.buttonBuy.setLocation(positionStore.x + 97 * Configs.UISCALE, positionStore.y + 76 * Configs.UISCALE);
        for(int i = 0; i < slots.length; i++) {
            int x = positionStore.x + slotsPositions[i].x * Configs.UISCALE;
            int y = positionStore.y + slotsPositions[i].y * Configs.UISCALE;
            this.slots[i].setPosition(x, y);
        }
        this.selected.setPosition(positionStore.x + 32 * Configs.UISCALE, positionStore.y + 37 * Configs.UISCALE);
    }

    @Override
    public void tick() {
        if(KeyBoard.KeyPressed("E")) {
            Engine.pause(null);
        }
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if(Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
                this.selected.take();
                this.selected.put(slot.item());
                indexSelected = i;
            }
        }
        Player player = Game.getPlayer();
        if(Mouse.clickOn(Mouse_Button.LEFT, buttonBuy) && player.getMoney() >= prices[indexSelected]) {
            if(player.getInventory().give(selected.item())) {
                player.setMoney(player.getMoney() - prices[indexSelected]);
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        refreshPosition();
        int fw = Engine.window.getWidth();
        int fh = Engine.window.getHeight();
        g.setColor(new Color(0,0,0, 180));
        g.fillRect(0, 0, fw, fh);
        renderStore(g);
        renderSlots(g);
        renderPriceSelected(g);
        renderMoney(g);
        renderButtonBuy(g);
    }

    private void renderStore(Graphics2D g) {
        g.drawImage(store, positionStore.x, positionStore.y, null);
    }

    private void renderSlots(Graphics2D g) {
        for(int i = 0; i < slots.length; i++) {
            slots[i].render(g);
        }
        selected.render(g);
    }

    private void renderPriceSelected(Graphics2D g) {
        if(indexSelected == -1)
            return;
        int x = positionStore.x + 62 * Configs.UISCALE;
        int y = positionStore.y + 48 * Configs.UISCALE;
        Font font = FontG.font(Configs.UISCALE * 8);
        g.setFont(font);
        g.setColor(Color.white);
        g.drawString("" + prices[indexSelected], x, y);
    }

    private void renderMoney(Graphics2D g) {
        int x = positionStore.x + 25 * Configs.UISCALE;
        int y = positionStore.y + 83 * Configs.UISCALE;
        Player player = Game.getPlayer();
        Font font = FontG.font(Configs.UISCALE * 8);
        g.setFont(font);
        g.setColor(Color.white);
        g.drawString("" + player.getMoney(), x, y);
    }

    public void renderButtonBuy(Graphics2D g) {
        if(!Mouse.on(buttonBuy))
            return;
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(Configs.UISCALE));
        g.drawRect(buttonBuy.x, buttonBuy.y, buttonBuy.width, buttonBuy.height);
    }

    @Override
    public void dispose() {

    }
}
