package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sounds;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;


public class Menu implements Activity {

    private Rectangle[] quadrados;
    private final String[] quadradosNomes = {"Play", "Options", "Quit"};
    private final Font fonteQuadrados = FontG.font(FontG.Game,8 * Configs.UiScale());
    private int quadradoSeta = -1;
    private BufferedImage imagemFundo;


    public Menu() {
        quadrados = new Rectangle[3];
        telacheia();
        Sound.stopAll();
        Sound.play(Musics.Menu, true);

        try {
            imagemFundo = ImageIO.read(getClass().getResource("/com/retronova/resources/icons/Gato.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                        Engine.heapActivity(new Personagens());
                        Sound.play(Sounds.Button);
                        break;
                    case 1:
                        Sound.play(Sounds.Button);
                        Engine.heapActivity(new Options());
                        break;
                    case 2:
                        Engine.CLOSE();
                        break;
                    default:
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
        if (imagemFundo != null) {
            g.drawImage(imagemFundo, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }

        g.setFont(fonteQuadrados);
        desenharBotoes(g);
    }

    private void desenharBotoes(Graphics2D g) {
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



    @Override
    public void dispose() {
    }
}