package com.retronova.menus;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.game.Corrida;
import com.retronova.game.Game;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.TelaDeMenu;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu de pausa, por cima do jogo congelado.
 *
 * Não desenha fundo próprio: o jogo continua visível atrás do véu, porque a pausa
 * é um estado do jogo e não uma tela separada (ver TelaDeMenu). O jogo para de
 * receber ticks, as animações de corpo param junto (ver Game.tempoVisual) e a
 * música abaixa enquanto ela está aberta.
 */
public class Pause extends TelaDeMenu {

    private final List<Button> botoes = new ArrayList<>();
    private int foco = -1;

    public Pause() {
        super("Paused");
        botoes.add(new Button(0, 0, 0, 0, "Continue", b -> voltar()).primary());
        botoes.add(new Button(0, 0, 0, 0, "Restart",
                b -> confirmacao.perguntar("Restart the game?", this::reiniciar)));
        botoes.add(new Button(0, 0, 0, 0, "Options", b -> abrir(new Options())));
        botoes.add(new Button(0, 0, 0, 0, "Main Menu",
                b -> confirmacao.perguntar("Return to main menu?", this::voltarAoMenu)));
        botoes.add(new Button(0, 0, 0, 0, "Quit",
                b -> confirmacao.perguntar("Do you want to quit?", this::sair)));
    }

    private void reiniciar() {
        Engine.pause(null);
        Game.restart();
    }

    /** Fecha o jogo, gravando antes. */
    private void sair() {
        Corrida.gravar();
        Engine.CLOSE();
    }

    private void voltarAoMenu() {
        // Grava ANTES de desempilhar: depois disso a partida ja nao e a corrente,
        // e a gravacao nao teria de quem fotografar o estado.
        Corrida.gravar();
        voltarAoMenuPrincipal();
    }

    @Override
    protected void posicionar() {
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int espaco = altura + 4 * Configs.UiScale();
        Rectangle area = areaDoConteudo();
        // A coluna e o aviso embaixo dela, centralizados juntos na área.
        int total = botoes.size() * espaco + 10 * Configs.UiScale();
        int x = Engine.window.getWidth() / 2 - largura / 2;
        int y = area.y + Math.max(0, (area.height - total) / 2);
        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(x, y + i * espaco, largura, altura));
        }
    }

    @Override
    protected void atualizar() {
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
    protected void desenharConteudo(Graphics2D g) {
        for (Button botao : botoes) {
            botao.render(g);
        }
        desenharAvisoDoSave(g);
    }

    /**
     * Diz onde a corrida está gravada, embaixo dos botões.
     *
     * NÃO HÁ BOTÃO DE "SAVE AND QUIT", e a ausência é proposital: sair pelo "Main
     * Menu" ou pelo "Quit" já grava. Um botão de salvar sugeriria que os outros
     * caminhos perdem a corrida, o que é falso.
     *
     * O que faltava não era o mecanismo, era o jogador SABER dele. Sem esta linha,
     * sair da partida parece perder tudo, e quem acha que vai perder tudo não sai —
     * fica jogando cansado, que é o problema que um suspend save existe para
     * resolver. A frase diz também o que fica de fora: os inimigos da tela não são
     * gravados, e voltam como a horda daquele minuto.
     */
    private void desenharAvisoDoSave(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        String aviso = "Leaving saves your run. Enemies on screen are not kept";
        int largura = FontHandler.getWidth(aviso, fonte);
        int x = Engine.window.getWidth() / 2 - largura / 2;
        Rectangle ultimo = botoes.get(botoes.size() - 1).getBounds();
        int y = ultimo.y + ultimo.height + 9 * s;
        g.setFont(fonte);
        TextFx.draw(g, aviso, x, y, Palette.LIGHT, s);
    }
}
