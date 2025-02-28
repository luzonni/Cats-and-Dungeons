package com.retronova.game.objects.interfaces.inventory;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.items.Item;
import com.retronova.game.items.Sword;
import com.retronova.inputs.keyboard.KeyBoard;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;

public class Inventory implements Activity {

    private final Slot insurer;
    private final Slot[] hotbar;
    private final Slot[] slots;

    public Inventory(int width, int height) {
        this.insurer = new Slot(0, 0);
        this.slots = new Slot[width * height];
        this.hotbar = new Slot[3];
        int spaceBetween = Configs.UISCALE;
        int x = 100;
        int y = 100;
        for(int yy = 0; yy < height; yy++) {
            for(int xx = 0; xx < width; xx++) {
                int index = xx + yy * width;
                int w = 16 * Configs.UISCALE;
                int h = 16 * Configs.UISCALE;
                int xxx = x + (xx * w + xx * spaceBetween);
                int yyy = y + (yy * h + yy * spaceBetween);
                slots[index] = new Slot(xxx, yyy);
            }
        }
        slots[0].put(new Sword());
        slots[5].put(new Sword());
    }

    @Override
    public void tick() {
        if(KeyBoard.KeyPressed("E")) {
            Engine.pause(null);
        }
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
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

    @Override
    public void render(Graphics2D g) {
        int fw = Engine.window.getWidth();
        int fh = Engine.window.getHeight();
        g.setColor(new Color(0,0,0, 180));
        g.fillRect(0, 0, fw, fh);
        renderInventory(g);
        renderInsurer(g);
    }

    private void renderInventory(Graphics2D g) {
        for(int i = 0; i < slots.length; i++) {
            slots[i].render(g);
        }
    }

    private void renderInsurer(Graphics2D g) {
        if(!insurer.isEmpty()) {
            int x = Mouse.getX() + 16;
            int y = Mouse.getY() + 16;
            g.drawImage(insurer.item().getSprite(), x, y, 16 * 2, 16 * 2, null);
        }
    }

    @Override
    public void dispose() {

    }
}
