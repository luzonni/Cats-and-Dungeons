package com.retronova.ui;

import com.retronova.engine.Engine;
import com.retronova.graphics.SpriteSheet;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;

public class UI {

    private Rectangle quadrado;
    private BufferedImage imagem;

    public UI (){
        quadrado = new Rectangle(Engine.window.getWidth()/2,Engine.window.getHeight()/2,500,50);
        imagem = new SpriteSheet("objects", "player", Engine.SCALE).getSHEET();
    }

    public void tick(){
        if(Mouse.clickOn(Mouse_Button.RIGHT,quadrado)){
            System.out.println("Sexo");
        }
    }
    public void render(Graphics g){
        g.setColor(Color.red);
        int x = quadrado.x;
        int y = quadrado.y;
        int width = quadrado.width;
        int height = quadrado.height;
        g.fillRect(x, y, width, height);
        g.drawImage(imagem, x+width/2-imagem.getWidth()/2, y,null);
    }
}

