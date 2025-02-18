package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.FontG;
import com.retronova.graphics.SpriteSheet;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class Personagens implements Activity {

    private Rectangle[] selecao;
    private Rectangle[] dificuldade;
    private Rectangle[] botoes;
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
        quadrados();
    }

    private void quadrados(){

        selecao = new Rectangle[3];
        dificuldade = new Rectangle[3];
        botoes = new Rectangle[2];

        selecao[1] = new Rectangle((Engine.window.getWidth() - 200) / 2, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Magico
        selecao[0] = new Rectangle(selecao[1].x - Engine.SCALE-220, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Normal
        selecao[2] = new Rectangle(selecao[1].x + Engine.SCALE+220, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Sortudo

        dificuldade[1] = new Rectangle((Engine.window.getWidth() - 50) / 2, (Engine.window.getHeight() - 120), 50, 50); // Dificuldade 2
        dificuldade[0] = new Rectangle(dificuldade[1].x - Engine.SCALE*25, (Engine.window.getHeight() - 120) , 50, 50); // Dificuldade 1
        dificuldade[2] = new Rectangle(dificuldade[1].x + Engine.SCALE*25, (Engine.window.getHeight() - 120), 50, 50); // Dificuldade 3

        botoes[0] = new Rectangle(50, dificuldade[1].y, 150, 50); // Voltar
        botoes[1] = new Rectangle(Engine.window.getWidth() - 200, dificuldade[1].y, 150, 50); // Jogar

    }

    @Override
    public void tick() {
        if (Mouse.clickOn(Mouse_Button.LEFT, botoes[0])) {
            System.out.println("Menu Aberto");
            Engine.setActivity(new Menu());
        }
    }


    @Override
    public void render(Graphics2D g) {
        quadrados();

        g.setFont(FontG.font(22 * Engine.SCALE));
        g.setStroke(new BasicStroke(Engine.SCALE));
        FontMetrics fmTitulo = g.getFontMetrics();

        g.setColor(Color.white);
        int x = (Engine.window.getWidth() - fmTitulo.stringWidth("Choose your character")) / 2;
        int y = 50;
        g.drawString("Choose your character", x, y + fmTitulo.getAscent());

        String[] gatos = {"Normal Cat", "Magic Cat", "Luck Cat"};


        for (int i = 0; i < selecao.length; i++) {
            int[] cores_gatos = {0x555555, 0x222244, 0x663300};
            g.setFont(FontG.font(18));
            FontMetrics fmGatos = g.getFontMetrics();

            g.setColor(new Color(cores_gatos[i], false));
            g.fillRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);

            g.setColor(Color.WHITE);
            g.drawRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);

            g.drawString(gatos[i], selecao[i].x + (selecao[i].width - fmGatos.stringWidth(gatos[i])) / 2, selecao[i].y - 10);

            BufferedImage imagem = imagens.get(i);
            g.drawImage(imagem, selecao[i].x + (selecao[i].width - imagem.getWidth() * 3) / 2, selecao[i].y + (selecao[i].height - imagem.getHeight() * 3) / 2, imagem.getWidth() * 3, imagem.getHeight() * 3, null);
        }

        dificuldadequadrados(g);
        quadradobotoes(g);
    }

    private void dificuldadequadrados(Graphics2D g) {
        int[] cores_dificuldade = {0x90EE90, 0xFFA500, 0xFF0000};
        String[] numeros = {"1", "2", "3"};

        Font fonteDificuldade = FontG.font(10 * Engine.SCALE);
        FontMetrics fmDificuldade = g.getFontMetrics(fonteDificuldade);

        g.setFont(fonteDificuldade);

        FontMetrics dificuldade_titulo = g.getFontMetrics(FontG.font(10 * Engine.SCALE));
        g.setColor(Color.white);
        String titulo = "Difficulty";

        int x = (Engine.window.getWidth() - dificuldade_titulo.stringWidth(titulo)) / 2;
        int y = dificuldade[1].y - 50 + dificuldade_titulo.getAscent();

        g.drawString(titulo, x, y);

        for (int i = 0; i < dificuldade.length; i++) {
            g.setColor(new Color(cores_dificuldade[i], false));
            g.fillRect(dificuldade[i].x, dificuldade[i].y, dificuldade[i].width, dificuldade[i].height);

            g.setColor(Color.WHITE);
            g.drawRect(dificuldade[i].x, dificuldade[i].y, dificuldade[i].width, dificuldade[i].height);

            g.drawString(numeros[i], dificuldade[i].x + (dificuldade[i].width - fmDificuldade.stringWidth(numeros[i])) / 2, dificuldade[i].y + (dificuldade[i].height - fmDificuldade.getHeight()) / 2 + fmDificuldade.getAscent());
        }

    }

    private void quadradobotoes(Graphics2D g) {
        int[] cores_botoes = {0x4A4A4A, 0x00A878};
        String[] botoes_nomes = {"Back", "Play"};

        Font fonteBotoes = FontG.font(10 * Engine.SCALE);
        FontMetrics fmBotoes = g.getFontMetrics(fonteBotoes);

        g.setFont(fonteBotoes);

        for (int i = 0; i < botoes.length; i++) {
            g.setColor(new Color(cores_botoes[i], false));
            g.fillRect(botoes[i].x, botoes[i].y, botoes[i].width, botoes[i].height);


            g.setColor(Color.WHITE);
            g.drawRect(botoes[i].x, botoes[i].y, botoes[i].width, botoes[i].height);

            g.drawString(botoes_nomes[i], botoes[i].x + (botoes[i].width - fmBotoes.stringWidth(botoes_nomes[i])) / 2, botoes[i].y + (botoes[i].height - fmBotoes.getHeight()) / 2 + fmBotoes.getAscent());
        }

    }

    @Override
    public void dispose() {

    }
}
