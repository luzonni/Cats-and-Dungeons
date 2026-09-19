package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.Confirm;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu de pausa, sobreposto ao jogo.
 *
 * Diferente das outras telas, não desenha fundo próprio: o jogo continua visível
 * atrás de um véu, porque a pausa é um estado do jogo e não uma tela separada.
 */
public class Pause implements Activity {

    private final List<Button> botoes = new ArrayList<>();
    private final Confirm confirmacao = new Confirm();
    private int foco = -1;

    public Pause() {
        botoes.add(new Button(0, 0, 0, 0, "Continue", b -> Engine.pause(null)).primary());
        botoes.add(new Button(0, 0, 0, 0, "Restart",
                b -> confirmacao.perguntar("Restart the game?", this::reiniciar)));
        botoes.add(new Button(0, 0, 0, 0, "Options", b -> abrirOpcoes()));
        botoes.add(new Button(0, 0, 0, 0, "Main Menu",
                b -> confirmacao.perguntar("Return to main menu?", this::voltarAoMenu)));
        botoes.add(new Button(0, 0, 0, 0, "Quit",
                b -> confirmacao.perguntar("Do you want to quit?", Engine::CLOSE)));
        posicionar();
    }

    /**
     * Abre as opções por cima da pausa, e não como tela empilhada.
     *
     * Empilhar trocava a Activity e fazia as opções aparecerem com o fundo da
     * tela inicial, como se o jogador tivesse saído da partida.
     */
    private void abrirOpcoes() {
        Engine.pause(Options.sobreposta(() -> Engine.pause(this)));
    }

    private void reiniciar() {
        Engine.pause(null);
        Game.restart();
    }

    private void voltarAoMenu() {
        Engine.pause(null);
        Sound.stopAll();
        Engine.backActivity(2);
        Sound.play(Musics.Menu, true);
    }

    private void posicionar() {
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int espaco = altura + 4 * Configs.UiScale();

        int x = Engine.window.getWidth() / 2 - largura / 2;
        int y = Engine.window.getHeight() / 2 - (botoes.size() * espaco) / 2 + 4 * Configs.UiScale();

        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(x, y + i * espaco, largura, altura));
        }
    }

    @Override
    public void tick() {
        posicionar();

        // O diálogo tem prioridade: enquanto aberto, os botões de trás não reagem.
        if (confirmacao.tick()) {
            return;
        }
        if (KeyBoard.KeyPressed("Escape")) {
            Engine.pause(null);
            return;
        }
        navegarPeloTeclado();

        for (int i = 0; i < botoes.size(); i++) {
            Button botao = botoes.get(i);
            botao.tick();
            if (botao.isHovered()) {
                foco = -1;
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
        g.setColor(Palette.VEIL);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());

        desenharTitulo(g);
        for (Button botao : botoes) {
            botao.render(g);
        }
        desenharAvisoDoSave(g);
        confirmacao.render(g);
    }

    private void desenharTitulo(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 14f * s);
        String titulo = "Paused";
        int largura = FontHandler.getWidth(titulo, fonte);
        int x = Engine.window.getWidth() / 2 - largura / 2;
        int y = botoes.get(0).getBounds().y - 8 * s;
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(titulo, x + s, y + s);
        g.setColor(Palette.LIGHT);
        g.drawString(titulo, x, y);
    }

    /**
     * Diz onde a corrida está gravada, embaixo dos botões.
     *
     * NÃO HÁ BOTÃO DE "SAVE AND QUIT", e a ausência é proposital: o jogo já gravou
     * sozinho ao entrar nesta sala. Um botão de salvar sugeriria que sem ele nada
     * é gravado, o que é falso, e ainda prometeria gravar o instante atual — que é
     * o que este save justamente não faz.
     *
     * O que faltava não era o mecanismo, era o jogador SABER dele. Sem esta linha,
     * sair da partida parece perder tudo, e quem acha que vai perder tudo não sai —
     * fica jogando cansado, que é o problema que um suspend save existe para
     * resolver. Uma frase resolve, e ela diz a verdade inteira, inclusive a parte
     * inconveniente: o ponto é o COMEÇO desta sala, não este segundo.
     */
    private void desenharAvisoDoSave(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        String aviso = "Your run is saved at the start of this room";
        int largura = FontHandler.getWidth(aviso, fonte);
        int x = Engine.window.getWidth() / 2 - largura / 2;
        Rectangle ultimo = botoes.get(botoes.size() - 1).getBounds();
        int y = ultimo.y + ultimo.height + 10 * s;
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(aviso, x + s, y + s);
        g.setColor(Palette.LIGHT);
        g.drawString(aviso, x, y);
    }

    @Override
    public void dispose() {
    }
}
