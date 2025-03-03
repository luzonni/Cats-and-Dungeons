package com.retronova.game.interfaces.inventory;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.exceptions.InventoryOutsOfBounds;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.graphics.FontG;
import com.retronova.graphics.SpriteSheet;
import com.retronova.inputs.keyboard.KeyBoard;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Inventory implements Activity {

    private final Slot insurer;
    private final Slot[] hotbar;
    private int lengthHotbar;
    private final Slot[] bag;
    private int lengthBag;

    private Item itemHand;

    private Point inventoryPosition;
    private BufferedImage inventory;

    public Inventory(int lengthBag, int lengthHotbar) {
        if(lengthBag > 15 || lengthHotbar > 5) {
            throw new InventoryOutsOfBounds("Valor de slots acima do permitido");
        }
        this.lengthBag = lengthBag;
        this.lengthHotbar = lengthHotbar;
        this.insurer = new Slot(0, 0);
        this.bag = new Slot[15];
        this.hotbar = new Slot[5];
        this.inventory = new SpriteSheet("ui", "inventory", Configs.UISCALE).getSHEET();

        refreshPositions();
        this.bag[lengthBag-1].put(Item.build(ItemIDs.Silk));
    }

    public void refreshPositions() {
        int xh = Configs.UISCALE * 6;
        int yh = Configs.UISCALE * 70;
        int xi = Configs.UISCALE*6;
        int yi = Configs.UISCALE*6;
        if(this.inventoryPosition == null) {
            this.inventoryPosition = new Point(Configs.MARGIN, Configs.MARGIN);
            xh += inventoryPosition.x;
            yh += inventoryPosition.y;
            xi += inventoryPosition.x;
            yi += inventoryPosition.y;
            for (int i = 0; i < hotbar.length; i++) {
                int w = 16 * Configs.UISCALE;
                this.hotbar[i] = new Slot(xh + i * w, yh);
            }
            for(int yy = 0; yy < 3; yy++) {
                for(int xx = 0; xx < 5; xx++) {
                    int index = xx + yy * 5;
                    int w = 16 * Configs.UISCALE;
                    int h = 16 * Configs.UISCALE;
                    int xxx = xi + (xx * w);
                    int yyy = yi + (yy * h);
                    bag[index] = new Slot(xxx, yyy);
                }
            }
            return;
        }
        if(this.inventoryPosition.x == Configs.MARGIN && this.inventoryPosition.y == Configs.MARGIN)
            return;
        this.inventoryPosition.setLocation(Configs.MARGIN, Configs.MARGIN);
        xh += inventoryPosition.x;
        yh += inventoryPosition.y;
        xi += inventoryPosition.x;
        yi += inventoryPosition.y;
        for (int i = 0; i < hotbar.length; i++) {
            int w = 16 * Configs.UISCALE;
            this.hotbar[i].setPosition(xh + i * w, yh);
        }
        for(int yy = 0; yy < 3; yy++) {
            for(int xx = 0; xx < 5; xx++) {
                int index = xx + yy * 5;
                int w = 16 * Configs.UISCALE;
                int h = 16 * Configs.UISCALE;
                int xxx = xi + (xx * w);
                int yyy = yi + (yy * h);
                bag[index].setPosition(xxx, yyy);
            }
        }
    }

    public boolean give(Item item) {
        Slot[] slots = merge();
        for(int i = 0; i < slots.length; i++) {
            if(slots[i].isEmpty()) {
                if(slots[i].put(item))
                    return true;
            }
        }
        return false;
    }

    public boolean drop(Item item) {
        Slot[] slots = merge();
        for(int i = 0; i < slots.length; i++) {
            if(!slots[i].isEmpty() && slots[i].item().equals(item)) {
                slots[i].take();
                return true;
            }
        }
        return false;
    }

    public void setItemHand(Item item) {
        this.itemHand = item;
    }

    public Item getItemHand() {
        return this.itemHand;
    }

    public int getHotbarSize() {
        return this.lengthHotbar;
    }

    public int getBagSize() {
        return this.lengthBag;
    }

    public void plusBag(int amount) {
        if(this.lengthBag + amount <= 15) {
            this.lengthBag += amount;
            return;
        }
        throw new InventoryOutsOfBounds("Aumento da BAG fora do tamanho limite");
    }

    public void plusHotbar(int amount) {
        if(this.lengthHotbar + amount <= 5) {
            this.lengthHotbar += amount;
            return;
        }
        throw new InventoryOutsOfBounds("Aumento da HOTBAR fora do tamanho limite");
    }

    public Item[] getHotbar() {
        Item[] items = new Item[lengthHotbar];
        for(int i = 0; i < items.length; i++) {
            items[i] = hotbar[i].item();
        }
        return items;
    }

    @Override
    public void tick() {
        if(KeyBoard.KeyPressed("E")) {
            Engine.pause(null);
        }
        refreshPositions();
        interation();
    }

    private void interation() {
        Slot[] currentSlots = merge();
        for(int i = 0; i < currentSlots.length; i++) {
            Slot slot = currentSlots[i];
            if(Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
                if(!slot.isEmpty() && insurer.isEmpty()) {
                    insurer.put(slot.take());
                }else if(slot.isEmpty() && !insurer.isEmpty()) {
                    slot.put(insurer.take());
                }else if(!slot.isEmpty() && !insurer.isEmpty()) {
                    Item insureItem = insurer.take();
                    Item slotItem = slot.take();
                    insurer.put(slotItem);
                    slot.put(insureItem);
                    //TODO continuar a logica e extrair metodos
                }
            }
        }
    }

    private Slot[] merge() {
        Slot[] merged = new Slot[lengthBag + lengthHotbar];
        int index = 0;
        for(int i = 0; i < lengthBag; i++, index++) {
            merged[index] = bag[i];
        }
        for(int i = 0; i < lengthHotbar; i++, index++) {
            merged[index] = hotbar[i];
        }
        return merged;
    }

    @Override
    public void render(Graphics2D g) {
        int fw = Engine.window.getWidth();
        int fh = Engine.window.getHeight();
        g.setColor(new Color(0,0,0, 180));
        g.fillRect(0, 0, fw, fh);
        renderInventory(g);
        renderInsurer(g);
        renderItemTag(g);
    }

    private void renderInventory(Graphics2D g) {
        g.drawImage(this.inventory, inventoryPosition.x, inventoryPosition.y, null);
        for(int i = 0; i < lengthBag; i++) {
            bag[i].render(g);
        }
        for(int i = 0; i < lengthHotbar; i++) {
            hotbar[i].render(g);
        }
    }

    private void renderInsurer(Graphics2D g) {
        if(!insurer.isEmpty()) {
            int x = Mouse.getX() + 16;
            int y = Mouse.getY() + 16;
            g.drawImage(insurer.item().getSprite(), x, y, 16 * 2, 16 * 2, null);
        }
    }

    private void renderItemTag(Graphics2D g) {
        String value = "";
        Slot[] slots = merge();
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if(Mouse.on(slot.getBounds()) && !slot.isEmpty()) {
                value = slot.item().getName();
            }
        }
        if(value.isBlank()) {
            return;
        }
        int x = Mouse.getX() + 16;
        int y = Mouse.getY() + 16;
        Font font = FontG.font(Configs.UISCALE * 8);
        int wF = FontG.getWidth(value, font);
        int hF = FontG.getHeight(value, font);
        int padding = Configs.UISCALE * 3;
        g.setFont(font);
        g.setStroke(new BasicStroke(Configs.UISCALE));
        g.setColor(new Color(190, 49, 68));
        g.fillRect(x, y, wF + padding*2, hF + padding*2);
        g.setColor(new Color(135, 35, 65));
        g.drawRect(x, y, wF + padding*2, hF + padding*2);
        g.setColor(Color.white);
        g.drawString(value, x + padding, y + hF + padding);
    }

    @Override
    public void dispose() {

    }

}
