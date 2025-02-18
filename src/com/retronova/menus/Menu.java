package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.IDs;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.FontG;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.io.File;

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

            GameMap map = new GameMap(new File("maps/playground"));
            double x = (map.getBounds().getWidth() / GameObject.SIZE()) / 2;
            double y = (map.getBounds().getHeight() / GameObject.SIZE()) / 2;

            Player[] player = new Player[] {
                    new Player(100, 100, "cinzento", 0.8, 12, 5),
                    new Player(100, 100, "mago", 0.8, 12, 10),
            };

            Player player1 = player[1];

            System.out.println("Seleção de Personagens aberta");
            Engine.setActivity(new Personagens());
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[1])) {
            System.out.println("Opções");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[2])) {
            Engine.CLOSE();
            System.out.println("Jogo Fechado");
        }
    }

    @Override
    public void render(Graphics2D g) {
        int[] cores_quadrados = {0x00A878, 0x4169E1, 0x708090};
        String[] quadrados_nomes = {"Play", "Options", "Exit"};

        Font fonteQuadrados = FontG.font(10 * Engine.UISCALE);
        FontMetrics fmQuadrados = g.getFontMetrics(fonteQuadrados);
        g.setFont(fonteQuadrados);



        for (int i = 0; i < quadrados.length; i++) {
            g.setColor(new Color(cores_quadrados[i], false));

            g.fillRect(quadrados[i].x, quadrados[i].y, quadrados[i].width, quadrados[i].height);
            g.setColor(Color.WHITE);
            g.drawRect(quadrados[i].x, quadrados[i].y, quadrados[i].width, quadrados[i].height);

            g.drawString(quadrados_nomes[i], quadrados[i].x + (quadrados[i].width - fmQuadrados.stringWidth(quadrados_nomes[i])) / 2, quadrados[i].y + (quadrados[i].height - fmQuadrados.getHeight()) / 2 + fmQuadrados.getAscent());

            // A formula é X + (largura do retangulo - largura da font) /2 , Y + (altura do retangulo - altura da fonte) /2 + Ascent (não sei muito bem explicar o ascent)
        }

    }

    @Override
    public void dispose() {

    }
}
