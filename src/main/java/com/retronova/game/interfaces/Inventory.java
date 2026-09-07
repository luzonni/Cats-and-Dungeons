package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.InventoryOutsOfBounds;
import com.retronova.game.Game;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.UiSprite;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

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
    /** Escala do HUD com que as posicoes atuais foram calculadas. */
    private int escalaAplicada = -1;
    private final UiSprite inventory;

    public Inventory(int lengthBag, int lengthHotbar) {
        if(lengthBag > 15 || lengthHotbar > 5) {
            throw new InventoryOutsOfBounds("Valor de slots acima do permitido");
        }
        this.lengthBag = lengthBag;
        this.lengthHotbar = lengthHotbar;
        this.insurer = new Slot(0, 0);
        this.bag = new Slot[15];
        this.hotbar = new Slot[5];
        this.inventory = new UiSprite("ui", "inventory");
        refreshPositions();
    }

    /**
     * Recoloca a moldura e os slots.
     *
     * Reage a DOIS gatilhos, e nao so a um: a janela mudou de tamanho, ou a
     * escala do HUD mudou. Antes so o primeiro era observado, entao mexer no
     * "HUD size" em Options deixava a moldura no tamanho velho e os slots nas
     * coordenadas novas — o inventario inteiro fora do lugar.
     */
    public void refreshPositions() {
        int s = Configs.HudScale();
        int X = Engine.window.getWidth() / 2 - inventory.largura() / 2;
        int Y = Engine.window.getHeight() / 2 - inventory.altura() / 2;

        boolean primeiraVez = this.inventoryPosition == null;
        boolean mudouPosicao = primeiraVez
                || this.inventoryPosition.x != X || this.inventoryPosition.y != Y;
        if (!mudouPosicao && s == this.escalaAplicada) {
            return;
        }
        this.escalaAplicada = s;
        if (primeiraVez) {
            this.inventoryPosition = new Point(X, Y);
        } else {
            this.inventoryPosition.setLocation(X, Y);
        }

        int w = 16 * s;
        int xh = X + 6 * s, yh = Y + 70 * s;
        int xi = X + 6 * s, yi = Y + 6 * s;
        for (int i = 0; i < hotbar.length; i++) {
            if (hotbar[i] == null) {
                hotbar[i] = new Slot(xh + i * w, yh);
            } else {
                hotbar[i].setPosition(xh + i * w, yh);
            }
        }
        for (int yy = 0; yy < 3; yy++) {
            for (int xx = 0; xx < 5; xx++) {
                int index = xx + yy * 5;
                int px = xi + xx * w;
                int py = yi + yy * w;
                if (bag[index] == null) {
                    bag[index] = new Slot(px, py);
                } else {
                    bag[index].setPosition(px, py);
                }
            }
        }
    }

    public void give(Item item) {
        Slot candidate = null;
        for(int i = 0; i < getHotbarSize(); i++) {
            Slot slot = hotbar[i];
            if(slot.isEmpty() || (slot.item().stackable() && slot.item().getID() == item.getID())) {
                candidate = slot;
                break;
            }
        }
        if(candidate == null)
            for(int i = 0; i < getBagSize(); i++) {
                Slot slot = bag[i];
                if(slot.isEmpty() || (slot.item().stackable() && slot.item().getID() == item.getID())) {
                    candidate = slot;
                    break;
                }
            }
        if(candidate == null)
            Game.getPlayer().dropLoot(item);
        else
            candidate.put(item);
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

    public boolean plusBag(int amount) {
        if(this.lengthBag + amount <= 15) {
            this.lengthBag += amount;
            return true;
        }
        return false;
    }

    public boolean plusHotbar(int amount) {
        if(this.lengthHotbar + amount <= 5) {
            this.lengthHotbar += amount;
            return true;
        }
        return false;
    }

    public Slot[] getHotbar() {
        return this.hotbar;
    }

    @Override
    public void tick() {
        refreshPositions();
        Slot[] slots = merge();
        for (Slot slot : slots) {
            interation(slot);
            if(slot.item() instanceof Consumable consumable) {
                if(Mouse.on(slot.getBounds()) && KeyBoard.KeyPressed("F")) {
                    consumable.consume();
                    slot.take();
                }
            }
        }
    }

    private void interation(Slot slot) {
        if(Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
            if(slot.isEmpty()) {
                slot.put(this.insurer.takeAll());
            }else {
                if(this.insurer.isEmpty()) {
                    this.insurer.put(slot.takeAll());
                }else {
                    replace(slot, this.insurer);
                }
            }
        }
        if(Mouse.clickOn(Mouse_Button.RIGHT, slot.getBounds())) {
            if(slot.isEmpty()) {
                slot.put(this.insurer.take());
            }else {
                if(this.insurer.isEmpty() || this.insurer.item().getID() == slot.item().getID()) {
                    this.insurer.put(slot.take());
                }else {
                    replace(slot, this.insurer);
                }
            }
        }
    }

    private void replace(Slot slot1, Slot slot2) {
        Item item = slot2.takeAll();
        if(!slot1.put(item)) {
            Item currentItem = slot1.takeAll();
            slot2.put(currentItem);
            slot1.put(item);
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
        g.drawImage(this.inventory.imagem(), inventoryPosition.x, inventoryPosition.y, null);
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
        if(!this.insurer.isEmpty())
            Game.getPlayer().dropLoot(this.insurer.takeAll());
    }

}
