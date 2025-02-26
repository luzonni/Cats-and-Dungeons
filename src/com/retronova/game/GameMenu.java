package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.graphics.FontG;
import com.retronova.inputs.keyboard.KeyBoard;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;
import com.retronova.menus.Menu;

import java.awt.*;

public class GameMenu implements Activity {
    private final Game game;
    private Rectangle[] quadrados;
    private final Font fonteTitulo = FontG.font(15 * Engine.UISCALE);
    private final Font fonteBotoes = FontG.font(8 * Engine.UISCALE);
    private final Color corFundo = new Color(50, 50, 50, 150);
    private final Color corBotao = Color.GRAY;
    private final Color corBorda = Color.WHITE;
    private final Color corTexto = Color.WHITE;
    private final String[] quadradosNomes = {"Continue", "Restart", "Options", "Main Menu", "Quit"};

    public GameMenu(Game game) {
        this.game = game;
        atualizarPosicoes(); // Serve para inicializar os quadrados
    }

    private void atualizarPosicoes() {
        quadrados = new Rectangle[5];

        int x = Engine.window.getWidth() / 2;
        int y = 350;
        int larguraBotao = 200;
        int alturaBotao = 50;

        quadrados[0] = new Rectangle(x - larguraBotao / 2, y - 150, larguraBotao, alturaBotao);
        quadrados[1] = new Rectangle(x - larguraBotao / 2, y - 50, larguraBotao, alturaBotao);
        quadrados[2] = new Rectangle(x - larguraBotao / 2, y + 50, larguraBotao, alturaBotao);
        quadrados[3] = new Rectangle(x - larguraBotao / 2, y + 150, larguraBotao, alturaBotao);
        quadrados[4] = new Rectangle(x - larguraBotao / 2, y + 250, larguraBotao, alturaBotao);
    }

    @Override
    public void tick() {
        if (KeyBoard.KeyPressed("ESCAPE")) {
            System.out.println("Jogo pausado");
            Engine.pause(null);
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[0])) {
            System.out.println("Jogo continuado");
            Engine.pause(null);
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[1])) {
            System.out.println("Jogo reiniciado");
            Engine.pause(null);
            game.reiniciarJogo();
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[2])) {
            System.out.println("Opções abertas");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[3])) {
            System.out.println("Menu aberto");
            Engine.pause(null);
            Engine.setActivity(new Menu());
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[4])) {
            System.out.println("Jogo fechado");
            Engine.CLOSE();
        }
    }

    @Override
    public void render(Graphics2D g) {
        atualizarPosicoes(); // Aqui serve para recalcular as posições dos quadrados na hora de redimensionar
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
        for (int i = 0; i < quadrados.length; i++) {
            g.setFont(fonteBotoes);
            g.setColor(corBotao);
            g.fillRect(quadrados[i].x, quadrados[i].y, quadrados[i].width, quadrados[i].height);
            g.setStroke(new BasicStroke(2.0f));
            g.setColor(corBorda);
            g.drawRect(quadrados[i].x, quadrados[i].y, quadrados[i].width, quadrados[i].height);
            g.setColor(corTexto);
            FontMetrics fmQuadrados = g.getFontMetrics();
            g.drawString(quadradosNomes[i], quadrados[i].x + (quadrados[i].width - fmQuadrados.stringWidth(quadradosNomes[i])) / 2, quadrados[i].y + (quadrados[i].height - fmQuadrados.getHeight()) / 2 + fmQuadrados.getAscent());
        }
    }

    @Override
    public void dispose() {
    }
}