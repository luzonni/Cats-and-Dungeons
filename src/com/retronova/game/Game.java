package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Game implements Activity {

    public static Camera C;

    //Teste
    private Player p;

    private final BufferedImage testeISprite;

    public Game() {
        this.C = new Camera();
        //inicialização do game
        this.testeISprite = new SpriteSheet("non", "non", Engine.SCALE).getSHEET();
        p = new Player(0, 0, 0);
    }

    @Override
    public void tick() {
        //tick logic
        p.tick();
    }

    @Override
    public void render(Graphics2D g) {
        //Render logic
        int width = testeISprite.getWidth();
        int height = testeISprite.getHeight();
        int x = Engine.window.getWidth()/2  - width/2;
        int y = Engine.window.getHeight()/2 - height/2;
        g.drawImage(this.testeISprite, x - C.getX(), y - C.getY(), null);
        p.render(g);
    }

    @Override
    public void dispose() {
        //limpar memoria
        System.out.println("Dispose Game");
    }

}
