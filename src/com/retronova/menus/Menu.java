package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Menu implements Activity {

    private Rectangle[] quadrados;
    private final Color[] coresQuadrados = {new Color(0x00A878), new Color(0x4169E1), new Color(0x708090)};
    private final String[] quadradosNomes = {"Play", "Options", "Quit"};
    private final Font fonteQuadrados = FontG.font(FontG.Game,8 * Configs.UiScale());
    private FontMetrics fmQuadrados;
    private int quadradoSeta = -1;

    public Menu() {
        quadrados = new Rectangle[3];
        telacheia();
        Sound.play(Musics.Music1, true);
    }


    private void telacheia() {
        int x = Engine.window.getWidth() / 2;
        int y = Engine.window.getHeight() / 2;
        int larguraBotao = 200;
        int alturaBotao = 50;

        quadrados[0] = new Rectangle(x - larguraBotao / 2, y - 150, larguraBotao, alturaBotao);
        quadrados[1] = new Rectangle(x - larguraBotao / 2, y - 50, larguraBotao, alturaBotao);
        quadrados[2] = new Rectangle(x - larguraBotao / 2, y + 50, larguraBotao, alturaBotao);
    }

    @Override
    public void tick() {
        telacheia();
        atualizarAnimacao();

        for (int i = 0; i < quadrados.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, quadrados[i])) {
                switch (i) {
                    case 0:
                        System.out.println("Clicou em Play");
                        Engine.setActivity(new Personagens());
                        break;
                    case 1:
                        System.out.println("Clicou em Options");
                        Engine.setActivity(new Options());
                        break;
                    case 2:
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
        fmQuadrados = g.getFontMetrics(fonteQuadrados);
        g.setFont(fonteQuadrados);

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