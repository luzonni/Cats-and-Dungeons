package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;

public class Menu implements Activity {

    private Rectangle[] quadrados;

    public Menu() {
        quadrados = new Rectangle[3];
        telacheia();

    }

    private void telacheia() {
        int x = Engine.window.getWidth() / 2;
        int y = Engine.window.getHeight() / 2;
        int largura_botao = 200;
        int altura_botao = 50;

        quadrados[0] = new Rectangle(x - largura_botao / 2, y - 150, largura_botao, altura_botao); // Botão de Jogar
        quadrados[1] = new Rectangle(x - largura_botao / 2, y - 50, largura_botao, altura_botao); // Botão de Opções
        quadrados[2] = new Rectangle(x - largura_botao / 2, y + 50, largura_botao, altura_botao); // Botão de Sair
    }

    @Override
    public void tick() {
        telacheia();
        if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[0])) {
            System.out.println("Jogo Iniciado");
            Engine.setActivity(new Game());
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[1])) {
            System.out.println("Opções");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[2])) {
            System.out.println("Jogo Fechado");
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 25));
        FontMetrics fm = g.getFontMetrics();

        // Botão de Jogar
        g.setColor(Color.RED);
        g.fillRect(quadrados[0].x, quadrados[0].y, quadrados[0].width, quadrados[0].height);
        g.setColor(new Color(255, 255, 255));
        g.drawRect(quadrados[0].x, quadrados[0].y, quadrados[0].width, quadrados[0].height);
        g.setColor(Color.WHITE);
        g.drawString("Jogar", quadrados[0].x + (quadrados[0].width - fm.stringWidth("Jogar")) / 2, quadrados[0].y + (quadrados[0].height - fm.getHeight()) / 2 + fm.getAscent());

        // Botão de Opções
        g.setColor(Color.BLUE);
        g.fillRect(quadrados[1].x, quadrados[1].y, quadrados[1].width, quadrados[1].height);
        g.setColor(new Color(255, 255, 255));
        g.drawRect(quadrados[1].x, quadrados[1].y, quadrados[1].width, quadrados[1].height);
        g.setColor(Color.WHITE);
        g.drawString("Opções", quadrados[1].x + (quadrados[1].width - fm.stringWidth("Opções")) / 2, quadrados[1].y + (quadrados[1].height - fm.getHeight()) / 2 + fm.getAscent());

        // Botão de Sair
        g.setColor(Color.GREEN);
        g.fillRect(quadrados[2].x, quadrados[2].y, quadrados[2].width, quadrados[2].height);
        g.setColor(new Color(255, 255, 255));
        g.drawRect(quadrados[2].x, quadrados[2].y, quadrados[2].width, quadrados[2].height);
        g.setColor(Color.WHITE);
        g.drawString("Sair", quadrados[2].x + (quadrados[2].width - fm.stringWidth("Sair")) / 2, quadrados[2].y + (quadrados[2].height - fm.getHeight()) / 2 + fm.getAscent());

        // Ordem das cores - Retangulo, borda e fonte
        // A formula é X + (largura do retangulo - largura da font) /2 , Y + (altura do retangulo - altura da fonte) /2 + Ascent (não sei muito bem explicar o ascent)
    }

    @Override
    public void dispose() {

    }
}
