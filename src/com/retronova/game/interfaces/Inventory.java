package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.InventoryOutsOfBounds;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Inventory implements Activity {

    private char[] numbers = {'1', '2', '3', '4', '5', '6', '7', '8', '9'};
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
        this.inventory = new SpriteSheet("ui", "inventory", Configs.HudScale()).getSHEET();
        refreshPositions();
    }

    public void refreshPositions() {
        int X = Engine.window.getWidth()/2 - inventory.getWidth()/2;
        int Y = Engine.window.getHeight()/2 - inventory.getHeight()/2;
        int xh = Configs.HudScale() * 6;
        int yh = Configs.HudScale() * 70;
        int xi = Configs.HudScale() * 6;
        int yi = Configs.HudScale() * 6;
        if(this.inventoryPosition == null) {
            this.inventoryPosition = new Point(X, Y);
            xh += inventoryPosition.x;
            yh += inventoryPosition.y;
            xi += inventoryPosition.x;
            yi += inventoryPosition.y;
            for (int i = 0; i < hotbar.length; i++) {
                int w = 16 * Configs.HudScale();
                this.hotbar[i] = new Slot(xh + i * w, yh);
            }
            for(int yy = 0; yy < 3; yy++) {
                for(int xx = 0; xx < 5; xx++) {
                    int index = xx + yy * 5;
                    int w = 16 * Configs.HudScale();
                    int h = 16 * Configs.HudScale();
                    int xxx = xi + (xx * w);
                    int yyy = yi + (yy * h);
                    bag[index] = new Slot(xxx, yyy);
                }
            }
            return;
        }
        if(this.inventoryPosition.x == X && this.inventoryPosition.y == Y)
            return;
        this.inventoryPosition.setLocation(X, Y);
        xh += inventoryPosition.x;
        yh += inventoryPosition.y;
        xi += inventoryPosition.x;
        yi += inventoryPosition.y;
        for (int i = 0; i < hotbar.length; i++) {
            int w = 16 * Configs.HudScale();
            this.hotbar[i].setPosition(xh + i * w, yh);
        }
        for(int yy = 0; yy < 3; yy++) {
            for(int xx = 0; xx < 5; xx++) {
                int index = xx + yy * 5;
                int w = 16 * Configs.HudScale();
                int h = 16 * Configs.HudScale();
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
        refreshPositions();
        interation();
    }

    private void interation() {
        for(int i = 0; i < lengthBag; i++) {
            Slot slot = bag[i];
            if(Mouse.on(slot.getBounds())) {
                try {
                    char keyChar = KeyBoard.getKeyChar(numbers);
                    int index = Integer.parseInt(String.valueOf(keyChar)) - 1;
                    if (index < lengthHotbar)
                        slotPermutation(slot, hotbar[index]);
                }catch (Exception ignore) {}
            }
            if(KeyBoard.KeyPressing("SHIFT") && Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
                Slot toChange = null;
                for(int j = 0; j < lengthHotbar; j++) {
                    Slot slot2 = hotbar[j];
                    if(slot2.isEmpty()) {
                        toChange = slot2;
                        break;
                    }
                }
                if(toChange != null) {
                    slotPermutation(slot, toChange);
                    return;
                }
            }
        }
        for(int i = 0; i < lengthHotbar; i++) {
            Slot slot = hotbar[i];
            if(KeyBoard.KeyPressing("SHIFT") && Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
                Slot toChange = null;
                for(int j = 0; j < lengthBag; j++) {
                    Slot slot2 = bag[j];
                    if(slot2.isEmpty()) {
                        toChange = slot2;
                        break;
                    }
                }
                if(toChange != null) {
                    slotPermutation(slot, toChange);
                    return;
                }
            }
        }
        Slot[] currentSlots = merge();
        for(int i = 0; i < currentSlots.length; i++) {
            Slot slot = currentSlots[i];
            if(Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
               slotPermutation(slot, this.insurer);
            }
            if(slot.item() instanceof Consumable consumable) {
                if(Mouse.clickOn(Mouse_Button.RIGHT, slot.getBounds())) {
                    consumable.consume();
                    slot.take();
                    //TODO adicionar som de item sendo consumido
                }
            }
        }
    }

    private void slotPermutation(Slot slot1, Slot slot2) {
        if(!slot1.isEmpty() && slot2.isEmpty()) {
            slot2.put(slot1.take());
        }else if(slot1.isEmpty() && !slot2.isEmpty()) {
            slot1.put(slot2.take());
        }else if(!slot1.isEmpty() && !slot2.isEmpty()) {
            Item insureItem = slot2.take();
            Item slotItem = slot1.take();
            slot2.put(slotItem);
            slot1.put(insureItem);
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
        renderInventory(g);
        renderInsurer(g);
        renderInfo(g);
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

    private void renderInfo(Graphics2D g) {
        Slot[] slots = merge();
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
