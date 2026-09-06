package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.menus.shared.Button;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Menu implements Activity {

    private final List<Button> botoes;
    private final BufferedImage imagemFundo;

    /** Índice do botão sob foco de teclado; -1 quando o mouse está no comando. */
    private int foco = -1;

    public Menu() {
        this.botoes = new ArrayList<>();
        criarBotoes();
        posicionarBotoes();
        Sound.stopAll();
        Sound.play(Musics.Menu, true);
        this.imagemFundo = SpriteSheet.getImage("icons.Gato");
    }

    private void criarBotoes() {
        // "Play" é a ação principal da tela e carrega o peso visual sozinha.
        botoes.add(new Button(0, 0, 0, 0, "Play",
                b -> Engine.heapActivity(new Personagens())).primary().meow());
        botoes.add(new Button(0, 0, 0, 0, "Options",
                b -> Engine.heapActivity(new Options())));
        botoes.add(new Button(0, 0, 0, 0, "Quit",
                b -> Engine.CLOSE()));
    }

    /** Recalcula posições a partir da escala da UI, para os cantos do 9-slice caírem inteiros. */
    private void posicionarBotoes() {
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int espaco = altura + 6 * Configs.UiScale();

        int x = Engine.window.getWidth() / 2 - largura / 2;
        int y = Engine.window.getHeight() / 2 - (botoes.size() * espaco) / 2;

        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(x, y + i * espaco, largura, altura));
        }
    }

    @Override
    public void tick() {
        posicionarBotoes();
        navegarPeloTeclado();

        for (int i = 0; i < botoes.size(); i++) {
            Button botao = botoes.get(i);
            botao.tick();
            if (botao.isHovered()) {
                Engine.window.pointing();
                foco = -1;          // o mouse assume o controle
            }
            botao.setFocused(foco == i);
        }
    }

    private void navegarPeloTeclado() {
        if (KeyBoard.KeyPressed("Down")) {
            foco = (foco + 1) % botoes.size();
        } else if (KeyBoard.KeyPressed("Up")) {
            foco = (foco <= 0 ? botoes.size() : foco) - 1;
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            botoes.get(foco).activate();
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
