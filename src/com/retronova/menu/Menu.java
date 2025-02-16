package com.retronova.menu;

import com.retronova.engine.Activity;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Menu implements Activity {

    private Rectangle[] quadrados;

    public Menu(){
        quadrados = new Rectangle[3];

        quadrados[0] = new Rectangle (100, 150, 200, 50); // Botão de Jogar
        quadrados[1] = new Rectangle (100,250, 200, 50); // Botão de Opções
        quadrados[2] = new Rectangle (100, 350, 200, 50); // Botão de Sair

    }

    @Override
    public void tick() {
        if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[0])) {
            System.out.println("Jogo Iniciado");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[1])) {
            System.out.println("Opções");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[2])) {
            System.out.println("Jogo Fechado");
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 25));
        FontMetrics fm = g.getFontMetrics(); // Isso aqui serve basicamente para pra medir a largura e a altura da fonte.

        // Botão de Jogar
        g.setColor(Color.RED);
        g.fillRect(100, 150, 200, 50);
        g.setColor(Color.WHITE);
        g.drawString("Jogar", 100 + (200 - fm.stringWidth("Jogar")) / 2, 150 + (50 - fm.getHeight()) / 2 + fm.getAscent());

        // Botão de Opções
        g.setColor(Color.BLUE);
        g.fillRect(100, 250, 200, 50);
        g.setColor(Color.WHITE);
        g.drawString("Opções", 100 + (200 - fm.stringWidth("Opções")) / 2, 250 + (50 - fm.getHeight()) / 2 + fm.getAscent());

        // Botão de Sair
        g.setColor(Color.GREEN);
        g.fillRect(100, 350, 200, 50);
        g.setColor(Color.WHITE);
        g.drawString("Sair", 100 + (200 - fm.stringWidth("Sair")) / 2, 350 + (50 - fm.getHeight()) / 2 + fm.getAscent());

        // A formula é X + (largura do retangulo - largura da font) /2 , Y + (altura do retangulo - altura da fonte) /2 + Ascent (não sei muito bem explicar o ascent)
    }

    @Override
    public void dispose() {

    }
}
