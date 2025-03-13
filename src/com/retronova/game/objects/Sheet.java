package com.retronova.game.objects;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Sheet<T extends GameObject> {

    public static Map<String, BufferedImage[]> SHEETS;

    static {
        SHEETS = new HashMap<>();
    }

    private final BufferedImage[][] sheet;
    private int type;
    private int index;

    private final Class<T> gameObject;

    public Sheet(Class<T> type, String... sprites) {
        gameObject = type;
        this.sheet = new BufferedImage[sprites.length][];
        for(int i = 0; i < sprites.length; i++) {
            if(SHEETS.containsKey(sprites[i])) {
                this.sheet[i] = SHEETS.get(sprites[i]);
            }else {
                this.sheet[i] = loadSprite(sprites[i]);
                SHEETS.put(sprites[i], this.sheet[i]);
            }
        }
    }

    BufferedImage getSprite() {
        return this.sheet[type][index];
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setType(int type) {
        this.type = type;
        if(this.type > this.sheet.length-1) {
            this.type = 0;
        }
    }

    public void plusIndex() {
        this.index++;
        if(this.index > this.sheet[type].length-1) {
            this.index = 0;
        }
    }

    private BufferedImage[] loadSprite(String spriteName) {
        String modulo = "objects/" + gameObject.getSimpleName().toLowerCase();
        SpriteSheet sheet = new SpriteSheet(modulo, spriteName, Configs.getSCALE());
        int length = sheet.getWidth() / 16;
        BufferedImage[] sprites = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            sprites[i] = sheet.getSpriteWithIndex(i, 0);
        }
        return sprites;
    }

    public void dispose() {
        SHEETS.clear();
    }

}
