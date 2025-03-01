package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.FontG;
import com.retronova.graphics.SpriteSheet;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Personagens implements Activity {

    private Rectangle[] selecao;
    private Rectangle[] dificuldade;
    private Rectangle[] botoes;
    private ArrayList<BufferedImage> imagens;
    private int personagemSelecionado = -1;
    private int dificuldadeSelecionada = -1;
    private Player[] players;
    private boolean[] quadradoClicadoDireito = {false, false, false};
    private String[][] infoPersonagens = {
            {"HP: 100", "Attack: 10", "Speed: Normal"},
            {"HP: 80", "Attack: 15", "Speed: Slow"},
            {"HP: 120", "Attack: 8", "Speed: Fast"}
    };

    // Variáveis para a animação de virada de carta
    private float[] rotacao = {0, 0, 0};
    private boolean[] animando = {false, false, false};
    private final float VELOCIDADE_ROTACAO = 0.1f;

    // Cores de todos os botões (quadrados)
    private final Color[] coresGatos = {new Color(0x555555), new Color(0x222244), new Color(0x663300)};
    private final Color[] coresPersonagensSelecionados = {new Color(0xA9A9A9), new Color(0x483D8B), new Color(0xFFA500)};
    private final Color[] coresDificuldade = {new Color(0x90EE90), new Color(0xFFA500), new Color(0xFF0000)};
    private final Color[] coresDificuldadeSelecionada = {new Color(0x6aa84f), new Color(0xf1c232), new Color(0xcc0000)};
    private final Color[] coresBotoes = {new Color(0x4A4A4A), new Color(0x00A878)};
    private final Color corTexto = Color.WHITE;

    // Fontes de todos os botões (quadrados)
    private final Font fonteTitulo = FontG.font(22 * Configs.UISCALE);
    private final Font fonteGatos = FontG.font(7 * Configs.UISCALE);
    private final Font fonteDificuldade = FontG.font(8 * Configs.UISCALE);
    private final Font fonteBotoes = FontG.font(8 * Configs.UISCALE);
    private final Font fonteDificuldadeTitulo = FontG.font(10 * Configs.UISCALE);
    private final Font fonteInfoPersonagens = FontG.font(5 * Configs.UISCALE);

    public Personagens() {
        imagens = new ArrayList<>();

        String[] gatos = {"cinzento", "mago", "sortudo"};
        for (String nome : gatos) {
            BufferedImage imagem = new SpriteSheet("objects/player", nome, Configs.UISCALE).getSHEET();
            imagens.add(imagem);
        }

        players = new Player[3];
        for (int i = 0; i < players.length; i++) {
            players[i] = Player.TEMPLATES[i];
        }

        for (int i = 0; i < rotacao.length; i++) {
            rotacao[i] = 1;
        }
    }

    @Override
    public void tick() {
        for (int i = 0; i < selecao.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, selecao[i])) {
                personagemSelecionado = (personagemSelecionado == i) ? -1 : i;
                if (personagemSelecionado != -1 && dificuldadeSelecionada == -1) {
                    System.out.println("Personagem selecionado. Escolha a dificuldade para prosseguir.");
                }
            }

            if (Mouse.clickOn(Mouse_Button.RIGHT, selecao[i])) {
                quadradoClicadoDireito[i] = !quadradoClicadoDireito[i];
                animando[i] = true;
                rotacao[i] = 0;
            }

            if (animando[i]) {
                rotacao[i] += VELOCIDADE_ROTACAO;
                if (rotacao[i] >= 1) {
                    rotacao[i] = 1;
                    animando[i] = false;
                }
            }
        }

        for (int i = 0; i < dificuldade.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, dificuldade[i])) {
                dificuldadeSelecionada = (dificuldadeSelecionada == i) ? -1 : i;
                if (personagemSelecionado == -1 && dificuldadeSelecionada != -1) {
                    System.out.println("Dificuldade selecionada. Escolha o personagem para prosseguir.");
                }
            }
        }

        for (int i = 0; i < players.length; i++) {
            // O personagem só será atualizado se ele estiver visível
            if (personagemSelecionado == i && !quadradoClicadoDireito[i]) {
                players[i].tick();
            }
        }

        if (Mouse.clickOn(Mouse_Button.LEFT, botoes[0])) {
            Engine.setActivity(new Menu());
        } else if (Mouse.clickOn(Mouse_Button.LEFT, botoes[1])) {
            if (personagemSelecionado != -1 && dificuldadeSelecionada != -1) {
                Engine.setActivity(new Game(personagemSelecionado, dificuldadeSelecionada, new GameMap()));
            } else {
                if (personagemSelecionado == -1 && dificuldadeSelecionada == -1) {
                    System.out.println("Selecione um personagem e dificuldade antes de jogar!");
                } else if (personagemSelecionado == -1) {
                    System.out.println("Selecione um personagem antes de jogar!");
                } else if (dificuldadeSelecionada == -1) {
                    System.out.println("Selecione a dificuldade antes de jogar!");
                }
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
        selecao[0] = new Rectangle(selecao[1].x - Configs.UISCALE - 220, (alturaTela - 300) / 2, 200, 300);
        selecao[2] = new Rectangle(selecao[1].x + Configs.UISCALE + 220, (alturaTela - 300) / 2, 200, 300);

        dificuldade[1] = new Rectangle((larguraTela - 50) / 2, (alturaTela - 120), 50, 50);
        dificuldade[0] = new Rectangle(dificuldade[1].x - Configs.UISCALE * 25, (alturaTela - 120), 50, 50);
        dificuldade[2] = new Rectangle(dificuldade[1].x + Configs.UISCALE * 25, (alturaTela - 120), 50, 50);

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
            RoundRectangle2D arredondar = new RoundRectangle2D.Double(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height, 15, 15);
            GradientPaint gradient = new GradientPaint(selecao[i].x, selecao[i].y, coresGatos[i].brighter(), selecao[i].x + selecao[i].width, selecao[i].y + selecao[i].height, coresGatos[i].darker());
            g.setPaint(gradient);
            g.fill(arredondar);

            g.setColor(corTexto);
            g.draw(arredondar);

            g.drawString(gatos[i], selecao[i].x + (selecao[i].width - fmGatos.stringWidth(gatos[i])) / 2, selecao[i].y - 10);

            AffineTransform originalTransform = g.getTransform();
            g.translate(selecao[i].x + selecao[i].width / 2, selecao[i].y + selecao[i].height / 2);

            float escala = quadradoClicadoDireito[i] ? (1 - rotacao[i]) : rotacao[i];
            g.scale(Math.max(escala, 0.1), 1);

            if (!quadradoClicadoDireito[i] || escala > 0.1) {
                BufferedImage imagem = players[i].getSprite();
                g.drawImage(imagem, (int) (-imagem.getWidth() * 2.5f / 2), (int) (-imagem.getHeight() * 2.5f / 2), (int) (imagem.getWidth() * 2.5f), (int) (imagem.getHeight() * 2.5f), null);
            }
            g.setTransform(originalTransform);

            if (quadradoClicadoDireito[i]) {
                originalTransform = g.getTransform();
                g.translate(selecao[i].x + selecao[i].width / 2, selecao[i].y + selecao[i].height / 2);
                g.scale(rotacao[i], 1);

                Font fonteOriginal = g.getFont();
                g.setFont(fonteInfoPersonagens);
                FontMetrics fmInfo = g.getFontMetrics();
                String[] info = infoPersonagens[i];
                int y = -selecao[i].height / 2 + 20;
                for (String line : info) {
                    g.drawString(line, -selecao[i].width / 2 + 10, y);
                    y += fmInfo.getHeight();
                }
                g.setFont(fonteOriginal);
                g.setTransform(originalTransform);
            }

            if (personagemSelecionado == i) {
                g.setColor(coresPersonagensSelecionados[i]);
                g.setStroke(new BasicStroke(2.0f));
                g.draw(arredondar);
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
            RoundRectangle2D arredondar = new RoundRectangle2D.Double(dificuldade[i].x, dificuldade[i].y, dificuldade[i].width, dificuldade[i].height, 15, 15);

            GradientPaint gradient = new GradientPaint(dificuldade[i].x, dificuldade[i].y, coresDificuldade[i].brighter(), dificuldade[i].x + dificuldade[i].width, dificuldade[i].y + dificuldade[i].height, coresDificuldade[i].darker());

            g.setPaint(gradient);
            g.fill(arredondar);

            g.setColor(corTexto);
            g.draw(arredondar);

            g.drawString(numeros[i], dificuldade[i].x + (dificuldade[i].width - fmDificuldade.stringWidth(numeros[i])) / 2, dificuldade[i].y + (dificuldade[i].height - fmDificuldade.getHeight()) / 2 + fmDificuldade.getAscent());

            if (dificuldadeSelecionada == i) {
                g.setColor(coresDificuldadeSelecionada[i]);
                g.setStroke(new BasicStroke(2.0f));
                g.draw(arredondar);
                g.setStroke(new BasicStroke(1.0f));
            }
        }
    }

    private void desenharBotoes(Graphics2D g) {
        String[] botoesNomes = {"Back", "Play"};
        g.setFont(fonteBotoes);
        FontMetrics fmBotoes = g.getFontMetrics();

        for (int i = 0; i < botoes.length; i++) {
            RoundRectangle2D arredondar = new RoundRectangle2D.Double(botoes[i].x, botoes[i].y, botoes[i].width, botoes[i].height, 15, 15);

            GradientPaint gradient = new GradientPaint(botoes[i].x, botoes[i].y, coresBotoes[i].brighter(), botoes[i].x + botoes[i].width, botoes[i].y + botoes[i].height, coresBotoes[i].darker());

            g.setPaint(gradient);
            g.fill(arredondar);

            g.setColor(coresBotoes[i].brighter());
            g.setStroke(new BasicStroke(2.0f));
            g.draw(arredondar);

            if (botoes[i].contains(Mouse.getX(), Mouse.getY())) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.0f));
                g.draw(arredondar);
            }

            g.setStroke(new BasicStroke(1.0f));

            g.setColor(Color.WHITE);
            g.drawString(botoesNomes[i], botoes[i].x + (botoes[i].width - fmBotoes.stringWidth(botoesNomes[i])) / 2, botoes[i].y + (botoes[i].height - fmBotoes.getHeight()) / 2 + fmBotoes.getAscent());
        }
    }

    @Override
    public void dispose() {

    }
}