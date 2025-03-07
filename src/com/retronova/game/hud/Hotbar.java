package com.retronova.game.hud;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.items.Item;
import com.retronova.game.objects.entities.Player;
import com.retronova.engine.graphics.SpriteSheet;
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

    public Hotbar(Player player) {
        this.player = player;
        SpriteSheet sheet = new SpriteSheet("ui", "hotbar", Configs.HUDSCALE);
        int sheetSize = sheet.getWidth()/16;
        this.sprites = new BufferedImage[sheetSize];
        for(int i = 0; i < sheetSize; i++) {
            this.sprites[i] = sheet.getSpriteWithIndex(i, 0);
        }
        refreshPositions();
    }

    private void refreshPositions() {
        int hotbarWidth = this.sprites[0].getWidth() * 5;
        int x = Engine.window.getWidth()/2 - hotbarWidth/2;
        int y = Engine.window.getHeight() - this.sprites[0].getHeight() - Configs.MARGIN;
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
        Item itemHand = player.getInventory().getHotbar()[getIndexHot()];
        player.getInventory().setItemHand(itemHand);
        if(KeyBoard.KeyPressed("Q") && itemHand != null) {
            if(player.getInventory().drop(itemHand)) {
                player.dropLoot(itemHand);
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
        }catch (Exception e) {}
        return index;
    }

    public void render(Graphics2D g) {
        Item[] items = player.getInventory().getHotbar();
        int length = player.getInventory().getHotbarSize();
        int w = bounds[0].width * bounds.length;
        int ww = bounds[0].width * length;
        int difX = (w - ww)/2;
        for(int i = 0; i < length; i++) {
            BufferedImage sprite = index == i ? sprites[1] : sprites[0];
            g.drawImage(sprite, bounds[i].x + difX, bounds[i].y, null);
            if(items[i] != null) {
                int x = bounds[i].x + difX + 2 * Configs.HUDSCALE;
                int y = bounds[i].y + 2 * Configs.HUDSCALE;
                g.drawImage(items[i].getSprite(), x, y,12 * Configs.HUDSCALE, 12 * Configs.HUDSCALE, null);
            }
        }
    }

}
