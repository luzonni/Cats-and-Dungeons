package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class Personagens implements Activity {

    private Rectangle[] selecao;
    private ArrayList<BufferedImage> imagens;


    Player[] player = new Player[] {
            new Player(0, 0, "cinzento", 0.8, 12, 5),
            new Player(0, 0, "mago", 0.8, 12, 10),
            new Player(0,0, "sortudo", 0.8, 12, 20),
    };

    public Personagens() {
        imagens = new ArrayList<>();

        String[] gatos = {"cinzento", "mago", "sortudo"};
        for (String nome : gatos) {
            BufferedImage imagem = new SpriteSheet("objects/player", nome, Engine.SCALE).getSHEET();
            imagens.add(imagem);
        }

        selecao = new Rectangle[3];
        quadrados();
    }

    private void quadrados(){

        selecao[0] = new Rectangle(220, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Normal
        selecao[1] = new Rectangle((Engine.window.getWidth() - 200) / 2, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Magico
        selecao[2] = new Rectangle(Engine.window.getWidth() - 200 - 220, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Sortudo
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(Graphics2D g) {
        quadrados();
        g.setFont(new Font("Arial", Font.BOLD, 25));
        FontMetrics fm = g.getFontMetrics();

        g.setColor(Color.white);
        int x = (Engine.window.getWidth() - fm.stringWidth("Selecione o Personagem")) / 2;
        int y = 50;
        g.drawString("Selecione o Personagem", x, y + fm.getAscent());

        String[] nomes = {"Gato Normal", "Gato Mágico", "Gato Sortudo"};

        for (int i = 0; i < selecao.length; i++) { // Já que eu queria 3 quadrados da mesma forma e mesmo estilo, eu criei um loop baseado na quantidade de quadrados que defini antes
            g.setColor(new Color(96, 94, 94));
            g.fillRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);

            g.setColor(Color.WHITE);
            g.drawRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);

            g.drawString(nomes[i], selecao[i].x + (selecao[i].width - fm.stringWidth(nomes[i])) / 2, selecao[i].y - 10);

            BufferedImage imagem = imagens.get(i);
            g.drawImage(imagem, selecao[i].x + (selecao[i].width - imagem.getWidth()) / 2, selecao[i].y + (selecao[i].height - imagem.getHeight()) / 2, null);
        }
    }

    @Override
    public void dispose() {

    }
}
