package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.menus.Menu;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Pause implements Activity {

    private Rectangle[] quadrados;
    private final Font fonteTitulo = FontG.font(FontG.Game, 15 * Configs.UiScale());
    private final Color corFundo = new Color(50, 50, 50, 150);
    private final Color corTexto = Color.WHITE;
    private final String[] quadradosNomes = {"Continue", "Restart", "Options", "Lobby", "Main Menu", "Quit"};
    private int quadradoSeta = -1;
    private Game game;
    private int botaoPressionado = -1;

    public Pause(Game game) {
        this.game = game;
        telacheia();
    }

    private void telacheia() {
        quadrados = new Rectangle[6];

        int x = Engine.window.getWidth() / 2;
        int y = 300;
        int larguraBotao = 200;
        int alturaBotao = 50;

        quadrados[0] = new Rectangle(x - larguraBotao / 2, y - 150, larguraBotao, alturaBotao);
        quadrados[1] = new Rectangle(x - larguraBotao / 2, y - 50, larguraBotao, alturaBotao);
        quadrados[2] = new Rectangle(x - larguraBotao / 2, y + 50, larguraBotao, alturaBotao);
        quadrados[3] = new Rectangle(x - larguraBotao / 2, y + 150, larguraBotao, alturaBotao);
        quadrados[4] = new Rectangle(x - larguraBotao / 2, y + 250, larguraBotao, alturaBotao);
        quadrados[5] = new Rectangle(x - larguraBotao / 2, y + 350, larguraBotao, alturaBotao);
    }

    @Override
    public void tick() {
        atualizarAnimacao();
        if (KeyBoard.KeyPressed("ESCAPE")) {
            System.out.println("Jogo pausado");
            Engine.pause(null);
            return;
        }

        for (int i = 0; i < quadrados.length; i++) {
            if (Mouse.isPressed(Mouse_Button.LEFT, quadrados[i])) {
                botaoPressionado = i;
            } else if (botaoPressionado == i && !Mouse.isPressed(Mouse_Button.LEFT, quadrados[i])) {
                botaoPressionado = -1;
                if (quadrados[i].contains(Mouse.getX(), Mouse.getY())) {
                    switch (i) {
                        case 0:
                            System.out.println("Clicou em Resume");
                            Engine.pause(null);
                            break;
                        case 1:
                            System.out.println("Clicou em Restart");
                            Sound.stop(Musics.Music3);
                            Sound.play(Musics.Music2, true);
                            Engine.pause(null);
                            Game.restart();
                            break;
                        case 2:
                            System.out.println("Clicou em Options");
                            break;
                        case 3:
                            System.out.println("Clicou em Looby");
                            Sound.stop(Musics.Music3);
                            Sound.play(Musics.Music2, true);
                            game.changeMap(null);
                            Engine.pause(null);
                            break;
                        case 4:
                            System.out.println("Clicou em Main Menu");
                            Engine.pause(null);
                            Sound.stop(Musics.Music3);
                            Sound.stop(Musics.Music2);
                            Engine.setActivity(new Menu());
                            break;
                        case 5:
                            System.out.println("Clicou em Quit");
                            Engine.CLOSE();
                            break;
                        default:
                            System.out.println("Botão desconhecido");
                            break;
                    }
                }
            }
        }
    }

    private void atualizarAnimacao() {
        quadradoSeta = -1;
        for (int i = 0; i < quadrados.length; i++) {
            if (quadrados[i].contains(Mouse.getX(), Mouse.getY())) {
                quadradoSeta = i;
                break;
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        telacheia();
        desenharFundo(g);
        desenharTitulo(g);
        desenharBotoes(g);
    }

    private void desenharFundo(Graphics2D g) {
        g.setColor(corFundo);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
    }

    private void desenharTitulo(Graphics2D g) {
        g.setFont(fonteTitulo);
        g.setColor(corTexto);
        FontMetrics fmPause = g.getFontMetrics();
        int x = (Engine.window.getWidth() - fmPause.stringWidth("Game Paused")) / 2;
        int y = 50;
        g.drawString("Game Paused", x, y + fmPause.getAscent());
    }

    private void desenharBotoes(Graphics2D g) {
        Stroke defaultStroke = g.getStroke();

        for (int i = 0; i < quadrados.length; i++) {
            int tamanhoFonte = 8 * Configs.UiScale();
            Font fonteAtual = FontG.font(FontG.Game, tamanhoFonte);
            FontMetrics fmQuadrados = g.getFontMetrics(fonteAtual);

            while (fmQuadrados.stringWidth(quadradosNomes[i]) > quadrados[i].width - 20) {
                tamanhoFonte--;
                fonteAtual = FontG.font(FontG.Game, tamanhoFonte);
                fmQuadrados = g.getFontMetrics(fonteAtual);
            }

            int larguraBotao = quadrados[i].width;
            int alturaBotao = quadrados[i].height;
            int x = quadrados[i].x;
            int y = quadrados[i].y;

            if (quadradoSeta == i) {
                larguraBotao = (int) (quadrados[i].width * 1.1);
                alturaBotao = (int) (quadrados[i].height * 1.1);
                x = quadrados[i].x - (larguraBotao - quadrados[i].width) / 2;
                y = quadrados[i].y - (alturaBotao - quadrados[i].height) / 2;
            }

            g.setColor(new Color(0xF0A59B));
            g.fillRect(x, y, larguraBotao, 8);

            g.setColor(new Color(0x6A2838));
            g.fillRect(x, y + alturaBotao - 6, larguraBotao, 6);

            RoundRectangle2D roundedRect = new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25);

            g.setColor(new Color(0x6A2838));
            RoundRectangle2D shadowRect = new RoundRectangle2D.Double(x + 3, y + 3, larguraBotao, alturaBotao, 15, 15);
            g.fill(shadowRect);

            RoundRectangle2D shadowRectLeft = new RoundRectangle2D.Double(x - 3, y + 3, larguraBotao, alturaBotao, 15, 15);
            g.fill(shadowRectLeft);

            g.setColor(new Color(0xCC4154));
            g.fill(roundedRect);

            g.setStroke(defaultStroke);

            g.setColor(Color.WHITE);
            g.setFont(fonteAtual);
            g.drawString(
                    quadradosNomes[i],
                    x + (larguraBotao - fmQuadrados.stringWidth(quadradosNomes[i])) / 2,
                    y + (alturaBotao - fmQuadrados.getHeight()) / 2 + fmQuadrados.getAscent()
            );
        }
    }


    @Override
    public void dispose() {
    }
}