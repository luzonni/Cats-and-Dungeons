package com.retronova.ui;

import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;

public class UI {

    private Rectangle quadrado;
    private BufferedImage imagem;

    public UI (){
        quadrado = new Rectangle(10,10,50,50);

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
        g.drawImage(imagem, x, y, width, height, null);
    }
}

