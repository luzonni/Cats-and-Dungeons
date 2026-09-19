package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.Cenario;
import com.retronova.menus.shared.Confirm;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Menu implements Activity {

    private final List<Button> botoes;
    /**
     * A ilustracao do titulo, viva.
     *
     * A arte e a mesma; o que muda e que ela deixou de ser um PNG parado. Ver
     * Cenario para o que cada efeito faz e por que.
     */
    private final Cenario cenario;

    /** Índice do botão sob foco de teclado; -1 quando o mouse está no comando. */
    private int foco = -1;

    private final Confirm confirmacao = new Confirm();

    public Menu() {
        this.botoes = new ArrayList<>();
        criarBotoes();
        posicionarBotoes();
        Sound.stopAll();
        Sound.play(Musics.Menu, true);
        Sound.play(Musics.Ambiente, true);
        this.cenario = new Cenario("icons.Gato");
    }

    private void criarBotoes() {
        // "PLAY" ABRE A TELA DE CORRIDAS, e nao a selecao de personagem.
        //
        // Com um save so, o menu dava conta com dois botoes: "Continue" quando havia
        // corrida guardada, "Play" quando nao. Com tres espacos essa saida acaba —
        // seriam tres botoes de continuar, todos com o mesmo rotulo, sem como dizer
        // qual e qual sem entrar. O que se escolhe aqui deixou de ser uma ACAO e
        // passou a ser uma CORRIDA, e corrida se escolhe vendo. Ver Saves.
        // SEM MIADO AQUI. Ele fazia sentido enquanto "Play" abria a selecao de gato:
        // o clique escolhia um bicho, e o bicho respondia. Agora ele abre a lista de
        // corridas, onde nao ha gato nenhum sendo escolhido — e era o miado
        // GENERICO ainda por cima, nao a voz de nenhum dos tres.
        //
        // Quem mia e o botao "Embark" da selecao de personagem, com a voz DAQUELE
        // gato. La o som responde a uma escolha; aqui ele so acontecia.
        botoes.add(new Button(0, 0, 0, 0, "Play",
                b -> Engine.heapActivity(new Saves())).primary());
        botoes.add(new Button(0, 0, 0, 0, "Options",
                b -> Engine.heapActivity(new Options())));
        //Sair pela pausa já perguntava; aqui fechava direto. Mesmo diálogo nos dois.
        botoes.add(new Button(0, 0, 0, 0, "Quit",
                b -> confirmacao.perguntar("Do you want to quit?", Engine::CLOSE)));
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
        // O cenario anima mesmo com o dialogo aberto: e fundo, nao interface.
        cenario.tick();

        // O diálogo tem prioridade: enquanto aberto, os botões de trás não reagem.
        if (confirmacao.tick()) {
            return;
        }
        navegarPeloTeclado();

        for (int i = 0; i < botoes.size(); i++) {
            Button botao = botoes.get(i);
            botao.tick();
            if (botao.isHovered()) {
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
        cenario.render(g);
        for (Button botao : botoes) {
            botao.render(g);
        }
        desenharVersao(g);
        confirmacao.render(g);
    }

    /**
     * Carimbo de versao no rodape.
     *
     * A versao vivia por cima de toda a tela, em corpo de botao, o tempo inteiro.
     * Ela nao muda durante a partida: nao tem por que disputar espaco com o jogo.
     * O canto da tela inicial e onde a maioria dos jogos a coloca, e e la que
     * alguem procura quando vai reportar um problema. Fica na ESQUERDA: e o lado
     * por onde a leitura comeca, e o direito e onde costuma ficar o que muda —
     * aviso, contador, atalho —, entao um carimbo fixo ali disputa atencao a toa.
     */
    private void desenharVersao(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Septem, 5f * s);
        String texto = Engine.VERSION;
        int x = Configs.Margin();
        int y = Engine.window.getHeight() - Configs.Margin();
        g.setFont(fonte);
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(texto, x + s, y + s);
        g.setColor(new Color(Palette.TEXT.getRed(), Palette.TEXT.getGreen(), Palette.TEXT.getBlue(), 110));
        g.drawString(texto, x, y);
    }

    @Override
    public void dispose() {
    }
}
