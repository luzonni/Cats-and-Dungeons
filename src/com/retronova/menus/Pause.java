package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Pause implements Activity {

    private Rectangle[] quadrados;
    private final Font fonteTitulo = FontHandler.font(FontHandler.Game, 15 * Configs.UiScale());
    private final Color corFundo = new Color(50, 50, 50, 150);
    private final Color corTexto = Color.WHITE;
    private final String[] quadradosNomes = {"Continue", "Restart", "Options", "Main Geral", "Quit"};
    private int quadradoSeta = -1;
    private int botaoPressionado = -1;

    private boolean confirmando = false;
    private ConfirmacaoTipo tipoConfirmacao = null;
    private final Rectangle botaoSim = new Rectangle();
    private final Rectangle botaoNao = new Rectangle();
    private final Font fonteConfirmacaoMensagem = FontHandler.font(FontHandler.Game, 8 * Configs.UiScale());
    private final Font fonteBotaoConfirmacao = FontHandler.font(FontHandler.Game, 9 * Configs.UiScale());
    private final Color corPopup = new Color(0x6B7A8F);

    private enum ConfirmacaoTipo {
        RESTART, MENU, QUIT
    }

    public Pause() {
        telacheia();
    }

    private void telacheia() {
        quadrados = new Rectangle[5];

        int x = Engine.window.getWidth() / 2;
        int yCentro = Engine.window.getHeight() / 2;
        int espacamento = 50;
        int alturaBotao = 50;
        int larguraBotao = 200;

        int yInicial = yCentro - ((alturaBotao + espacamento) * quadrados.length - espacamento) / 2;

        for (int i = 0; i < quadrados.length; i++) {
            int y = yInicial + i * (alturaBotao + espacamento);
            quadrados[i] = new Rectangle(x - larguraBotao / 2, y, larguraBotao, alturaBotao);
        }
    }

    @Override
    public void tick() {
        atualizarAnimacao();

        if (KeyBoard.KeyPressed("ESCAPE") && !confirmando) {
            System.out.println("Jogo pausado");
            Engine.pause(null);
            return;
        }

        if (confirmando) {
            if (Mouse.isPressed(Mouse_Button.LEFT, botaoSim)) {
                Sound.play(Sounds.Button);
                executarAcaoConfirmada();
                confirmando = false;
            } else if (Mouse.isPressed(Mouse_Button.LEFT, botaoNao)) {
                Sound.play(Sounds.Button);
                confirmando = false;
                tipoConfirmacao = null;
            }
            return;
        }

        for (int i = 0; i < quadrados.length; i++) {
            if (Mouse.isPressed(Mouse_Button.LEFT, quadrados[i])) {
                botaoPressionado = i;
            } else if (botaoPressionado == i && !Mouse.isPressed(Mouse_Button.LEFT, quadrados[i])) {
                botaoPressionado = -1;
                if (quadrados[i].contains(Mouse.getX(), Mouse.getY())) {
                    switch (i) {
                        case 0 -> {
                            Engine.pause(null);
                            Sound.play(Sounds.Button);
                        }
                        case 1 -> {
                            tipoConfirmacao = ConfirmacaoTipo.RESTART;
                            confirmando = true;
                        }
                        case 2 -> {
                            Sound.play(Sounds.Button);
                            Engine.pause(null);
                            Engine.heapActivity(new Options());
                        }
                        case 3 -> {
                            tipoConfirmacao = ConfirmacaoTipo.MENU;
                            confirmando = true;
                        }
                        case 4 -> {
                            tipoConfirmacao = ConfirmacaoTipo.QUIT;
                            confirmando = true;
                        }
                    }
                }
            }
        }
    }

    private void executarAcaoConfirmada() {
        switch (tipoConfirmacao) {
            case RESTART -> {
                Engine.pause(null);
                Game.restart();
            }
            case MENU -> {
                Engine.pause(null);
                Sound.stopAll();
                Engine.backActivity(2);
                Sound.play(Sounds.Button);
                Sound.play(Musics.Menu, true);
            }
            case QUIT -> Engine.CLOSE();
        }
        tipoConfirmacao = null;
    }

    private void atualizarAnimacao() {
        quadradoSeta = -1;
        if (confirmando) return;
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
        if (confirmando) {
            Composite originalComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
            g.setComposite(originalComposite);

            desenharPopupConfirmacao(g);
        }
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
            int tamanhoFonte = 8 * Configs.UiScale();
            Font fonteAtual = FontHandler.font(FontHandler.Game, tamanhoFonte);
            FontMetrics fmQuadrados = g.getFontMetrics(fonteAtual);

            while (fmQuadrados.stringWidth(quadradosNomes[i]) > quadrados[i].width - 20) {
                tamanhoFonte--;
                fonteAtual = FontHandler.font(FontHandler.Game, tamanhoFonte);
                fmQuadrados = g.getFontMetrics(fonteAtual);
            }

            int larguraBotao = quadrados[i].width;
            int alturaBotao = quadrados[i].height;
            int x = quadrados[i].x;
            int y = quadrados[i].y;

            boolean selecionado = quadradoSeta == i;

            if (selecionado) {
                larguraBotao = (int) (quadrados[i].width * 1.1);
                alturaBotao = (int) (quadrados[i].height * 1.1);
                x = quadrados[i].x - (larguraBotao - quadrados[i].width) / 2;
                y = quadrados[i].y - (alturaBotao - quadrados[i].height) / 2;
            }

            g.setColor(new Color(0x4A5364));
            g.fillRect(x, y + alturaBotao - 5, larguraBotao, 5);

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g.setColor(new Color(0x000000));
            RoundRectangle2D shadowRect = new RoundRectangle2D.Double(x + 2, y + 2, larguraBotao, alturaBotao, 20, 20);
            g.fill(shadowRect);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            RoundRectangle2D roundedRect = new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25);
            g.setColor(new Color(0x6B7A8F));
            g.fill(roundedRect);

            GradientPaint lightTop = new GradientPaint(
                    x, y, new Color(255, 255, 255, 60),
                    x, y + alturaBotao / 2, new Color(255, 255, 255, 0)
            );
            g.setPaint(lightTop);
            g.fill(new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25));

            g.setColor(Color.WHITE);
            g.setFont(fonteAtual);
            g.drawString(
                    quadradosNomes[i],
                    x + (larguraBotao - fmQuadrados.stringWidth(quadradosNomes[i])) / 2,
                    y + (alturaBotao - fmQuadrados.getHeight()) / 2 + fmQuadrados.getAscent()
            );
        }
    }

    private void desenharPopupConfirmacao(Graphics2D g) {
        int largura = 500;
        int altura = 180;
        int x = (Engine.window.getWidth() - largura) / 2;
        int y = (Engine.window.getHeight() - altura) / 2;
        int bordaRaio = 25;

        g.setColor(corPopup);
        g.fillRoundRect(x, y, largura, altura, bordaRaio, bordaRaio);

        g.setColor(corPopup.darker());
        g.drawRoundRect(x, y, largura, altura, bordaRaio, bordaRaio);

        String mensagem = switch (tipoConfirmacao) {
            case RESTART -> "Restart the game?";
            case MENU -> "Return to main menu?";
            case QUIT -> "Do you want to quit?";
        };

        g.setFont(fonteConfirmacaoMensagem);
        FontMetrics fm = g.getFontMetrics();
        int textoX = x + (largura - fm.stringWidth(mensagem)) / 2;
        int textoY = y + 60;
        g.setColor(Color.WHITE);
        g.drawString(mensagem, textoX, textoY);

        int larguraBotao = 80;
        int alturaBotao = 35;
        int espacamentoBotoes = 60;
        int yBotoes = y + altura - alturaBotao - 30;

        botaoSim.setBounds(x + largura / 2 - larguraBotao - espacamentoBotoes / 2, yBotoes, larguraBotao, alturaBotao);
        botaoNao.setBounds(x + largura / 2 + espacamentoBotoes / 2, yBotoes, larguraBotao, alturaBotao);

        desenharBotaoConfirmacao(g, botaoSim, "Yes");
        desenharBotaoConfirmacao(g, botaoNao, "No");
    }

    private void desenharBotaoConfirmacao(Graphics2D g, Rectangle botao, String texto) {
        g.setColor(corPopup.darker());
        g.fillRoundRect(botao.x, botao.y, botao.width, botao.height, 15, 15);

        g.setColor(Color.WHITE);
        g.setFont(fonteBotaoConfirmacao);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto,
                botao.x + (botao.width - fm.stringWidth(texto)) / 2,
                botao.y + (botao.height - fm.getHeight()) / 2 + fm.getAscent()
        );
    }

    @Override
    public void dispose() {}
}