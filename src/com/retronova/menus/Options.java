package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Options implements Activity {

    private Rectangle[] quadrados;
    //private final Color[] corBotao = {new Color(0x00A878), new Color(0x4169E1), new Color(0x708090), new Color(0x808080), new Color(0xA9A9A9)};
    private final Color corBotao = Color.GRAY;
    private final Color corTexto = Color.WHITE;
    private final String[] quadradosNomes = {"Resolution", "Full Screan", "Save", "Botão 4", "Back"};
    private final Font fonteQuadrados = FontG.font(8 * Configs.UISCALE);
    private FontMetrics fmQuadrados;
    private int quadradoSeta = -1;

    public Options() {
        telacheia();
    }

    private void telacheia() {
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
        atualizarSeta();

        if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[0])) {
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[1])) {
            System.out.println("Opções");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[2])) {
            System.out.println("Jogo fechado");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[3])) {
            System.out.println("Botão 4");
        } else if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[4])) {
            System.out.println("Botão 5");
        }
    }

    private void atualizarSeta() {
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
        desenharBotoes(g);
        desenharSeta(g);
    }

    private void desenharBotoes(Graphics2D g) {
        Stroke defaultStroke = g.getStroke();

        for (int i = 0; i < quadrados.length; i++) {
            int tamanhoFonte = 8 * Configs.UISCALE;
            Font fonteAtual = FontG.font(tamanhoFonte);
            FontMetrics fmQuadrados = g.getFontMetrics(fonteAtual);

            while (fmQuadrados.stringWidth(quadradosNomes[i]) > quadrados[i].width - 20) {
                tamanhoFonte--;
                fonteAtual = FontG.font(tamanhoFonte);
                fmQuadrados = g.getFontMetrics(fonteAtual);
            }

            RoundRectangle2D arredondar = new RoundRectangle2D.Double(quadrados[i].x, quadrados[i].y, quadrados[i].width, quadrados[i].height, 15, 15);

            GradientPaint gradient = new GradientPaint(quadrados[i].x, quadrados[i].y, corBotao.brighter(), quadrados[i].x, quadrados[i].y + quadrados[i].height, corBotao.darker());

            g.setPaint(gradient);
            g.fill(arredondar);

            g.setColor(corBotao.brighter().darker());
            g.setStroke(new BasicStroke(1.0f));
            g.draw(arredondar);

            if (quadrados[i].contains(Mouse.getX(), Mouse.getY())) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.0f));
                g.draw(arredondar);
            }

            g.setStroke(defaultStroke);

            g.setColor(corTexto);
            g.setFont(fonteAtual);
            g.drawString(quadradosNomes[i], quadrados[i].x + (quadrados[i].width - fmQuadrados.stringWidth(quadradosNomes[i])) / 2, quadrados[i].y + (quadrados[i].height - fmQuadrados.getHeight()) / 2 + fmQuadrados.getAscent());
        }
    }


    private void desenharSeta(Graphics2D g) {
        if (quadradoSeta != -1) {
            int setaX = quadrados[quadradoSeta].x - 20;
            int setaY = quadrados[quadradoSeta].y + quadrados[quadradoSeta].height / 2;
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3.0f));
            g.drawLine(setaX, setaY, setaX - 10, setaY - 8);
            g.drawLine(setaX, setaY, setaX - 10, setaY + 8);
        }
    }

    @Override
    public void dispose() {
    }
}