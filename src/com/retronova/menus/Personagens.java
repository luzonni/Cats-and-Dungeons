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
    private int personagemSelecionado = -1;
    private int dificuldadeSelecionada = -1;
    private int personagemInformacoes = -1;




    public Personagens() {
        imagens = new ArrayList<>();

        String[] gatos = {"cinzento", "mago", "sortudo"};
        for (String nome : gatos) {
            BufferedImage imagem = new SpriteSheet("objects/player", nome, Engine.UISCALE).getSHEET();
            imagens.add(imagem);
        }
        quadrados();
    }

    private void quadrados(){

        selecao = new Rectangle[3];
        dificuldade = new Rectangle[3];
        botoes = new Rectangle[2];

        selecao[1] = new Rectangle((Engine.window.getWidth() - 200) / 2, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Magico
        selecao[0] = new Rectangle(selecao[1].x - Engine.UISCALE-220, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Normal
        selecao[2] = new Rectangle(selecao[1].x + Engine.UISCALE+220, (Engine.window.getHeight() - 300) / 2, 200, 300); // Gato Sortudo

        dificuldade[1] = new Rectangle((Engine.window.getWidth() - 50) / 2, (Engine.window.getHeight() - 120), 50, 50); // Dificuldade 2
        dificuldade[0] = new Rectangle(dificuldade[1].x - Engine.UISCALE*25, (Engine.window.getHeight() - 120) , 50, 50); // Dificuldade 1
        dificuldade[2] = new Rectangle(dificuldade[1].x + Engine.UISCALE*25, (Engine.window.getHeight() - 120), 50, 50); // Dificuldade 3

        botoes[0] = new Rectangle(50, dificuldade[1].y, 150, 50); // Voltar
        botoes[1] = new Rectangle(Engine.window.getWidth() - 200, dificuldade[1].y, 150, 50); // Jogar

    }

    @Override
    public void tick() {
        for (int i = 0; i < selecao.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, selecao[i])) {
                if (personagemSelecionado == i) {
                    personagemSelecionado = -1;
                    System.out.println("Personagem deselecionado");
                } else {
                    personagemSelecionado = i;
                    System.out.println("Personagem selecionado: " + i);
                }
                break;
            }
        }

        if (Mouse.clickOn(Mouse_Button.LEFT, botoes[0])) {
            Engine.setActivity(new Menu());
            System.out.println("Menu aberto");
        }

        for (int i = 0; i < dificuldade.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, dificuldade[i])) {
                if (dificuldadeSelecionada == i) {
                    dificuldadeSelecionada = -1;
                    System.out.println("Dificuldade deselecionada");
                } else {
                    dificuldadeSelecionada = i;
                    System.out.println("Dificuldade selecionada: " + i);
                }
                break;
            }
        }

        if (Mouse.clickOn(Mouse_Button.LEFT, botoes[0])) {
            Engine.setActivity(new Menu());
            System.out.println("Menu aberto");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, botoes[1])) {
            if (personagemSelecionado != -1 && dificuldadeSelecionada != -1) {
                Engine.setActivity(new Game(Game.PLAYERS[personagemSelecionado], new GameMap(new File("maps/playground"))));
                System.out.println("Jogo iniciado");
            } else {
                System.out.println("Selecione um personagem e/ou dificuldade antes de jogar!");
            }
        }

    }


    @Override
    public void render(Graphics2D g) {
        quadrados();

        g.setFont(FontG.font(22 * Engine.UISCALE));
        FontMetrics fmTitulo = g.getFontMetrics();

        g.setColor(Color.white);
        int x = (Engine.window.getWidth() - fmTitulo.stringWidth("Choose your character")) / 2;
        int y = 50;
        g.drawString("Choose your character", x, y + fmTitulo.getAscent());

        String[] gatos = {"Normal Cat", "Magic Cat", "Lucky Cat"};


        for (int i = 0; i < selecao.length; i++) {
            int[] cores_gatos = {0x555555, 0x222244, 0x663300};
            g.setFont(FontG.font(7 * Engine.UISCALE));
            FontMetrics fmGatos = g.getFontMetrics();

            g.setColor(new Color(cores_gatos[i], false));
            g.fillRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);

            g.setColor(Color.WHITE);
            g.drawRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);

            g.drawString(gatos[i], selecao[i].x + (selecao[i].width - fmGatos.stringWidth(gatos[i])) / 2, selecao[i].y - 10);

            BufferedImage imagem = Game.PLAYERS[i].getSprite();
            g.drawImage(imagem, selecao[i].x + (selecao[i].width - imagem.getWidth() * 3) / 2, selecao[i].y + (selecao[i].height - imagem.getHeight() * 3) / 2, imagem.getWidth() * 3, imagem.getHeight() * 3, null);


            if (personagemSelecionado == i) {
                int[] cores_personagens = {0xA9A9A9, 0x483D8B, 0xFFA500};

                g.setColor(new Color(cores_personagens[i], false));
                float thickness = 5.0f; // Espessura da borda - PFV NÃO APAGUE ESSA MENSAGEM EU NUNCA LEMBRO COMO ARRUMA A ESPESSURA
                g.setStroke(new BasicStroke(thickness));
                g.drawRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);
                g.setStroke(new BasicStroke(1.0f));
            }

            if (personagemInformacoes == i) {
                int[] cores_selecionado = {0xA9A9A9, 0x483D8B, 0xFFA500};
                String[] personagens = {"Testando", "Testando2", "Testando3"};
                Font fonteselecionado = FontG.font(10 * Engine.UISCALE);
                FontMetrics fmSelecionado = g.getFontMetrics(fonteselecionado);

                g.setColor(new Color(cores_selecionado[i], false));
                float thickness = 5.0f;
                g.setStroke(new BasicStroke(thickness));
                g.drawRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);
                g.setStroke(new BasicStroke(1.0f));

                g.setColor(Color.white);
                g.drawString(personagens[i], selecao[i].x + (selecao[i].width - fmSelecionado.stringWidth(personagens[i])) / 2, selecao[i].y + (selecao[i].height - fmSelecionado.getHeight()) / 2 + fmSelecionado.getAscent());
            }
        }

        dificuldadequadrados(g);
        quadradobotoes(g);
    }

    private void dificuldadequadrados(Graphics2D g) {
        int[] cores_dificuldade = {0x90EE90, 0xFFA500, 0xFF0000};
        String[] numeros = {"1", "2", "3"};

        Font fonteDificuldade = FontG.font(10 * Engine.UISCALE);
        FontMetrics fmDificuldade = g.getFontMetrics(fonteDificuldade);

        g.setFont(fonteDificuldade);

        FontMetrics dificuldade_titulo = g.getFontMetrics(FontG.font(10 * Engine.UISCALE));
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

            if (dificuldadeSelecionada == i) {
                int[] cores_dificuldade_att = {0x6aa84f, 0xf1c232, 0xcc0000};
                g.setColor(new Color(cores_dificuldade_att[i], false));
                float thickness = 5.0f;
                g.setStroke(new BasicStroke(thickness));
                g.drawRect(dificuldade[i].x, dificuldade[i].y, dificuldade[i].width, dificuldade[i].height);
                g.setStroke(new BasicStroke(1.0f));
            }
        }

    }

    private void quadradobotoes(Graphics2D g) {
        int[] cores_botoes = {0x4A4A4A, 0x00A878};
        String[] botoes_nomes = {"Back", "Play"};

        Font fonteBotoes = FontG.font(10 * Engine.UISCALE);
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
