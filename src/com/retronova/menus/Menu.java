package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.sound.Sounds;
import com.retronova.menus.shared.Button;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Menu implements Activity {

    private List<Button> botoes;
    private final String[] botoesNomes = {"Play", "Options", "Quit"};
    private BufferedImage imagemFundo;

    public Menu() {
        botoes = new ArrayList<>();
        inicializarBotoes();
        telacheia();
        Sound.stopAll();
        Sound.play(Musics.Menu, true);

        imagemFundo = SpriteSheet.getImage("icons.Gato");
    }

    private void inicializarBotoes() {
        int larguraBotao = 200;
        int alturaBotao = 50;
        int xInicial = Engine.window.getWidth() / 2 - larguraBotao / 2;
        int yInicial = Engine.window.getHeight() / 2 - 150;
        int espacamento = 100;

        for (int i = 0; i < botoesNomes.length; i++) {
            final int index = i;
            botoes.add(new Button(
                    xInicial,
                    yInicial + i * espacamento,
                    larguraBotao,
                    alturaBotao,
                    botoesNomes[i],
                    button -> {
                        Sound.play(Sounds.Button);
                        switch (index) {
                            case 0:
                                Engine.heapActivity(new Personagens());
                                break;
                            case 1:
                                Engine.heapActivity(new Options());
                                break;
                            case 2:
                                Engine.CLOSE();
                                break;
                        }
                    }
            ));
        }
    }

    private void telacheia() {
        int larguraBotao = 200;
        int alturaBotao = 50;
        int xInicial = Engine.window.getWidth() / 2 - larguraBotao / 2;
        int yInicial = Engine.window.getHeight() / 2 - 150;
        int espacamento = 100;

        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(xInicial, yInicial + i * espacamento, larguraBotao, alturaBotao));
        }
    }

    @Override
    public void tick() {
        telacheia();
        for (Button botao : botoes) {
            botao.tick();
            if (botao.getBounds().contains(Mouse.getX(), Mouse.getY())) {
                Engine.window.pointing();
            }
        }

        boolean botaoApontado = botoes.stream().anyMatch(botao -> botao.getBounds().contains(Mouse.getX(), Mouse.getY()));
        if (!botaoApontado) {
            Engine.window.normalCursor();
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (imagemFundo != null) {
            g.drawImage(imagemFundo, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }

        for (Button botao : botoes) {
            botao.render(g);
        }
    }

    @Override
    public void dispose() {
    }
}