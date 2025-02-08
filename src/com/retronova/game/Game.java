package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Game implements Activity {

    private final BufferedImage testeISprite;

    public Game() {
        //inicialização do game
        this.testeISprite = new SpriteSheet("non", "non", Engine.SCALE).getSHEET();
    }

    @Override
    public void tick() {
        //tick logic
    }

    @Override
    public void render(Graphics g) {
        //Render logic
        int width = testeISprite.getWidth();
        int height = testeISprite.getHeight();
        int x = Engine.window.getWidth()/2  - width/2;
        int y = Engine.window.getHeight()/2 - height/2;
        g.drawImage(this.testeISprite, x, y, null);
    }

    @Override
    public void dispose() {
        //limpar memoria
        System.out.println("Dispose Game");
    }

}
