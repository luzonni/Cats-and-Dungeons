package com.retronova.game.hud;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawString;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.game.interfaces.Slot;
import com.retronova.game.items.Consumable;
import com.retronova.game.objects.entities.Player;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;

import java.awt.*;
import java.awt.image.BufferedImage;

class Hotbar {

    private BufferedImage[] sprites;
    private Rectangle[] bounds;
    private final Player player;
    private int index;
    private char[] numbers = {'1', '2', '3', '4', '5', '6', '7', '8', '9'};

    private Font fontStack;

    public Hotbar(Player player) {
        this.player = player;
        SpriteHandler sheet = new SpriteHandler("ui", "hotbar", Configs.HudScale());
        int sheetSize = sheet.getWidth()/16;
        this.sprites = new BufferedImage[sheetSize];
        this.fontStack = FontHandler.font(FontHandler.Septem, Configs.HudScale() * 8);
        for(int i = 0; i < sheetSize; i++) {
            this.sprites[i] = sheet.getSpriteWithIndex(i, 0);
        }
    }

    private void refreshPositions() {
        int hotbarWidth = this.sprites[0].getWidth() * 5;
        int x = Engine.window.getWidth()/2 - hotbarWidth/2;
        int y = Engine.window.getHeight() - this.sprites[0].getHeight() - Configs.Margin();
        if(this.bounds == null) {
            this.bounds = new Rectangle[5];
            for(int i = 0; i < 5; i++) {
                this.bounds[i] = new Rectangle(x + i * sprites[0].getWidth(), y, sprites[0].getWidth(), sprites[0].getHeight());
            }
        }
        for(int i = 0; i < 5; i++) {
            this.bounds[i].setLocation(x + i * sprites[0].getWidth(), y);
        }
    }


    public void tick() {
        refreshPositions();
        Slot slot = player.getInventory().getHotbar()[getIndexHot()];
        player.getInventory().setItemHand(slot.item());
        if(!slot.isEmpty()) {
            if(KeyBoard.KeyPressed("F")) {
                if(slot.item() instanceof Consumable consumable) {
                    consumable.consume();
                    slot.take();
                }
            }
            if(KeyBoard.KeyPressed("Q")) {
                player.dropLoot(slot.take());
            }
        }
    }

    Rectangle[] getBounds() {
        return this.bounds;
    }

    private int getIndexHot() {
        int length = player.getInventory().getHotbarSize();
        int scroll = Mouse.Scroll();
        if(scroll > 0) {
            index++;
            if(index > length-1) {
                index = 0;
            }
        }else if(scroll < 0) {
            index--;
            if(index < 0) {
                index = length-1;
            }
        }
        try {
            char keyChar = KeyBoard.getKeyChar(numbers);
            int number = Integer.parseInt(String.valueOf(keyChar)) - 1;
            if(number < length)
                this.index = number;
        }catch (Exception ignore) {}
        return index;
    }

    public void render(Graphics2D g) {
        refreshPositions();
        Slot[] items = player.getInventory().getHotbar();
        int length = player.getInventory().getHotbarSize();
        int w = bounds[0].width * bounds.length;
        int ww = bounds[0].width * length;
        int difX = (w - ww)/2;
        for(int i = 0; i < length; i++) {
            BufferedImage sprite = index == i ? sprites[1] : sprites[0];
            g.drawImage(sprite, bounds[i].x + difX, bounds[i].y, null);
            if(!items[i].isEmpty()) {
                int x = bounds[i].x + difX;
                int y = bounds[i].y;
                g.drawImage(items[i].item().getSprite(), x + 2 * Configs.HudScale(), y + 2 * Configs.HudScale(),12 * Configs.HudScale(), 12 * Configs.HudScale(), null);
                if(items[i].item() instanceof Consumable consumable) {
                    if(consumable.getStack() <= 1)
                        continue;
                    String stack = String.valueOf(consumable.getStack());
                    int wf = FontHandler.getWidth(stack, fontStack);
                    int hf = FontHandler.getHeight(stack, fontStack);
                    DrawString.draw(stack, fontStack, x + bounds[i].width - wf - 2 * Configs.HudScale(), y + bounds[i].height - hf - 2 * Configs.HudScale(), g);
                }
            }
        }
    }

}
