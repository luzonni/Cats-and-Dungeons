package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.FontG;
import com.retronova.graphics.SpriteSheet;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Personagens implements Activity {

    private Rectangle[] selecao;
    private Rectangle[] dificuldade;
    private Rectangle[] botoes;
    private ArrayList<BufferedImage> imagens;
    private int personagemSelecionado = -1;
    private int dificuldadeSelecionada = -1;

    // Cores de todos os botões (quadrados)
    private final Color[] coresGatos = {new Color(0x555555), new Color(0x222244), new Color(0x663300)};
    private final Color[] coresPersonagensSelecionados = {new Color(0xA9A9A9), new Color(0x483D8B), new Color(0xFFA500)};
    private final Color[] coresDificuldade = {new Color(0x90EE90), new Color(0xFFA500), new Color(0xFF0000)};
    private final Color[] coresDificuldadeSelecionada = {new Color(0x6aa84f), new Color(0xf1c232), new Color(0xcc0000)};
    private final Color[] coresBotoes = {new Color(0x4A4A4A), new Color(0x00A878)};
    private final Color corTexto = Color.WHITE;

    // Fontes de todos os botões (quadrados)
    private final Font fonteTitulo = FontG.font(22 * Engine.UISCALE);
    private final Font fonteGatos = FontG.font(7 * Engine.UISCALE);
    private final Font fonteDificuldade = FontG.font(8 * Engine.UISCALE);
    private final Font fonteBotoes = FontG.font(8 * Engine.UISCALE);
    private final Font fonteDificuldadeTitulo = FontG.font(10 * Engine.UISCALE);

    public Personagens() {
        imagens = new ArrayList<>();

        String[] gatos = {"cinzento", "mago", "sortudo"};
        for (String nome : gatos) {
            BufferedImage imagem = new SpriteSheet("objects/player", nome, Engine.UISCALE).getSHEET();
            imagens.add(imagem);
        }
    }

    @Override
    public void tick() {
        for (int i = 0; i < selecao.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, selecao[i])) {
                personagemSelecionado = (personagemSelecionado == i) ? -1 : i;
            }
        }

        for (int i = 0; i < dificuldade.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, dificuldade[i])) {
                dificuldadeSelecionada = (dificuldadeSelecionada == i) ? -1 : i;
            }
        }

        if (Mouse.clickOn(Mouse_Button.LEFT, botoes[0])) {
            Engine.setActivity(new Menu());
        } else if (Mouse.clickOn(Mouse_Button.LEFT, botoes[1])) {
            if (personagemSelecionado != -1 && dificuldadeSelecionada != -1) {
                Engine.setActivity(new Game(personagemSelecionado, dificuldadeSelecionada, new GameMap()));
            } else {
                System.out.println("Selecione um personagem e/ou dificuldade antes de jogar!");
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        atualizarPosicoes();
        desenharTitulo(g);
        desenharSelecao(g);
        desenharDificuldade(g);
        desenharBotoes(g);
    }

    private void atualizarPosicoes() {
        selecao = new Rectangle[3];
        dificuldade = new Rectangle[3];
        botoes = new Rectangle[2];

        int larguraTela = Engine.window.getWidth();
        int alturaTela = Engine.window.getHeight();

        selecao[1] = new Rectangle((larguraTela - 200) / 2, (alturaTela - 300) / 2, 200, 300);
        selecao[0] = new Rectangle(selecao[1].x - Engine.UISCALE - 220, (alturaTela - 300) / 2, 200, 300);
        selecao[2] = new Rectangle(selecao[1].x + Engine.UISCALE + 220, (alturaTela - 300) / 2, 200, 300);

        dificuldade[1] = new Rectangle((larguraTela - 50) / 2, (alturaTela - 120), 50, 50);
        dificuldade[0] = new Rectangle(dificuldade[1].x - Engine.UISCALE * 25, (alturaTela - 120), 50, 50);
        dificuldade[2] = new Rectangle(dificuldade[1].x + Engine.UISCALE * 25, (alturaTela - 120), 50, 50);

        botoes[0] = new Rectangle(50, dificuldade[1].y, 150, 50);
        botoes[1] = new Rectangle(larguraTela - 200, dificuldade[1].y, 150, 50);
    }

    private void desenharTitulo(Graphics2D g) {
        g.setFont(fonteTitulo);
        g.setColor(corTexto);
        FontMetrics fmTitulo = g.getFontMetrics();
        int x = (Engine.window.getWidth() - fmTitulo.stringWidth("Choose your character")) / 2;
        int y = 50;
        g.drawString("Choose your character", x, y + fmTitulo.getAscent());
    }

    private void desenharSelecao(Graphics2D g) {
        String[] gatos = {"Normal Cat", "Magic Cat", "Lucky Cat"};
        g.setFont(fonteGatos);
        FontMetrics fmGatos = g.getFontMetrics();
        for (int i = 0; i < selecao.length; i++) {
            g.setColor(coresGatos[i]);
            g.fillRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);
            g.setColor(corTexto);
            g.drawRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);
            g.drawString(gatos[i], selecao[i].x + (selecao[i].width - fmGatos.stringWidth(gatos[i])) / 2, selecao[i].y - 10);
            BufferedImage imagem = Player.TEMPLATES[i].getSprite();
            g.drawImage(imagem, selecao[i].x + (selecao[i].width - imagem.getWidth() * 3) / 2, selecao[i].y + (selecao[i].height - imagem.getHeight() * 3) / 2, imagem.getWidth() * 3, imagem.getHeight() * 3, null);
            if (personagemSelecionado == i) {
                g.setColor(coresPersonagensSelecionados[i]);
                g.setStroke(new BasicStroke(5.0f));
                g.drawRect(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height);
                g.setStroke(new BasicStroke(1.0f));
            }
        }
    }

    private void desenharDificuldade(Graphics2D g) {
        String[] numeros = {"1", "2", "3"};
        g.setFont(fonteDificuldade);
        FontMetrics fmDificuldade = g.getFontMetrics();

        g.setColor(corTexto);
        FontMetrics fmDificuldadeTitulo = g.getFontMetrics(fonteDificuldadeTitulo);
        int xTitulo = (Engine.window.getWidth() - fmDificuldadeTitulo.stringWidth("   Difficulty")) / 2;
        int yTitulo = dificuldade[1].y - 50 + fmDificuldadeTitulo.getAscent();
        g.drawString("   Difficulty", xTitulo, yTitulo);

        for (int i = 0; i < dificuldade.length; i++) {
            g.setColor(coresDificuldade[i]);
            g.fillRect(dificuldade[i].x, dificuldade[i].y, dificuldade[i].width, dificuldade[i].height);
            g.setColor(corTexto);
            g.drawRect(dificuldade[i].x, dificuldade[i].y, dificuldade[i].width, dificuldade[i].height);
            g.drawString(numeros[i], dificuldade[i].x + (dificuldade[i].width - fmDificuldade.stringWidth(numeros[i])) / 2, dificuldade[i].y + (dificuldade[i].height - fmDificuldade.getHeight()) / 2 + fmDificuldade.getAscent());
            if (dificuldadeSelecionada == i) {
                g.setColor(coresDificuldadeSelecionada[i]);
                g.setStroke(new BasicStroke(5.0f));
                g.drawRect(dificuldade[i].x, dificuldade[i].y, dificuldade[i].width, dificuldade[i].height);
                g.setStroke(new BasicStroke(1.0f));
            }
        }
    }

    private void desenharBotoes(Graphics2D g) {
        String[] botoesNomes = {"Back", "Play"};
        g.setFont(fonteBotoes);
        FontMetrics fmBotoes = g.getFontMetrics();
        for (int i = 0; i < botoes.length; i++) {
            g.setColor(coresBotoes[i]);
            g.fillRect(botoes[i].x, botoes[i].y, botoes[i].width, botoes[i].height);
            g.setColor(corTexto);
            g.drawRect(botoes[i].x, botoes[i].y, botoes[i].width, botoes[i].height);
            g.drawString(botoesNomes[i], botoes[i].x + (botoes[i].width - fmBotoes.stringWidth(botoesNomes[i])) / 2, botoes[i].y + (botoes[i].height - fmBotoes.getHeight()) / 2 + fmBotoes.getAscent());
        }
    }

    @Override
    public void dispose() {

    }
}