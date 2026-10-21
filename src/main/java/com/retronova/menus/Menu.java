package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Conquistas;
import com.retronova.game.map.arena.Icones;
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
    /** O troféu das conquistas: o último da lista, mas fora da coluna central. */
    private Button trofeu;
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

    /** Duracao, em ticks, da saida dos botoes antes de abrir Options. */
    private static final int DURACAO_TRANSICAO = 30;
    /** Atraso entre um botao e o seguinte comecar a sair. */
    private static final int ATRASO_POR_BOTAO = 5;
    /** Tick atual da transicao para Options; -1 quando nao ha transicao. */
    private int transicao = -1;

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
                b -> transicao = 0));
        //Sair pela pausa já perguntava; aqui fechava direto. Mesmo diálogo nos dois.
        botoes.add(new Button(0, 0, 0, 0, "Quit",
                b -> confirmacao.perguntar("Do you want to quit?", Engine::CLOSE)));
        // O TROFEU E O ULTIMO DA LISTA, para o teclado alcancá-lo como aos outros,
        // mas nao entra na coluna central: posicionarBotoes o leva para o canto.
        trofeu = new Button(0, 0, 0, 0, "", b -> Engine.heapActivity(new TelaDeConquistas()));
        botoes.add(trofeu);
    }

    /** Recalcula posições a partir da escala da UI, para os cantos do 9-slice caírem inteiros. */
    private void posicionarBotoes() {
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int espaco = altura + 6 * Configs.UiScale();

        int x = Engine.window.getWidth() / 2 - largura / 2;
        // Sem o troféu: ele não faz parte da coluna.
        int naColuna = botoes.size() - 1;
        int y = Engine.window.getHeight() / 2 - (naColuna * espaco) / 2;

        for (int i = 0; i < naColuna; i++) {
            botoes.get(i).setBounds(new Rectangle(x, y + i * espaco, largura, altura));
        }
        // No canto superior direito, pequeno e quadrado, com a margem da interface.
        // Era 1,3x a altura de um botão: grande demais para um atalho secundário.
        int lado = 13 * Configs.UiScale();
        trofeu.setBounds(new Rectangle(Engine.window.getWidth() - Configs.Margin() - lado,
                Configs.Margin(), lado, lado));
    }

    @Override
    public void tick() {
        posicionarBotoes();
        // O cenario anima mesmo com o dialogo aberto: e fundo, nao interface.
        cenario.tick();

        // Durante a saida, nada reage: so a animacao anda ate abrir Options.
        if (transicao >= 0) {
            if (++transicao >= DURACAO_TRANSICAO) {
                transicao = -1;
                Engine.heapActivity(new Options());
            }
            return;
        }

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
        // O troféu sobe e acende ao passar o mouse (ou ao focar), e desce ao sair.
        boolean sobre = trofeu.isHovered() || trofeu.isFocused();
        hoverDoTrofeu += ((sobre ? 1 : 0) - hoverDoTrofeu) * 0.35;
        if (sobre) {
            Engine.window.pointing();
        }
    }

    /** 0 parado, 1 com o mouse em cima; suavizado a cada tick. */
    private double hoverDoTrofeu;

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
        for (int i = 0; i < botoes.size(); i++) {
            if (botoes.get(i) == trofeu) {
                continue;           // o troféu tem desenho próprio, logo abaixo
            }
            int dx = deslocamentoDeSaida(i);
            g.translate(dx, 0);
            botoes.get(i).render(g);
            g.translate(-dx, 0);
        }
        desenharTrofeu(g);
        desenharVersao(g);
        desenharEscurecimento(g);
        confirmacao.render(g);
    }

    /**
     * O troféu por cima do botão vazio: o ícone, um selo vermelho se há conquista
     * nova que o jogador não viu, e a dica no foco ou com o mouse em cima.
     *
     * Acompanha a saída dos botões (a mesma translação), para não ficar parado
     * enquanto o resto da tela vai embora.
     */
    private void desenharTrofeu(Graphics2D g) {
        int s = Configs.UiScale();
        Rectangle r = trofeu.getBounds();
        int dx = deslocamentoDeSaida(botoes.indexOf(trofeu));
        g.translate(dx, 0);

        // UM QUADRADO NEUTRO, e não um botão com orelhas. O ouro do troféu competia
        // com o resto do menu, que é todo em ardósia; aqui o ícone usa a cor de
        // texto secundário e só clareia quando o mouse chega perto.
        //
        // E ele REAGE: sobe até 2 pixels, a borda acende e o ícone vai para o
        // branco. A subida é suavizada em tick(), então não pula. A cor só troca em
        // dois valores (e não num degradê) porque os ícones são pintados uma vez e
        // guardados por cor — um degradê criaria uma imagem nova a cada quadro.
        double h = hoverDoTrofeu;
        boolean aceso = h > 0.5;
        int sobe = (int) Math.round(2 * s * h);
        int y = r.y - sobe;

        g.setColor(new Color(0, 0, 0, 70));                       // sombra no "chão"
        g.fillRoundRect(r.x + s, r.y + 2 * s, r.width, r.height, 5 * s, 5 * s);
        g.setColor(aceso ? Palette.ACCENT : Palette.OUTLINE);     // borda
        g.fillRoundRect(r.x - s, y - s, r.width + 2 * s, r.height + 2 * s, 6 * s, 6 * s);
        g.setColor(aceso ? Palette.MAIN : Palette.DEEP);          // corpo
        g.fillRoundRect(r.x, y, r.width, r.height, 5 * s, 5 * s);
        g.setColor(new Color(255, 255, 255, (int) (22 + 28 * h))); // brilho no topo
        g.fillRect(r.x + 2 * s, y + s, r.width - 4 * s, s);

        int celula = Math.max(1, (int) Math.round(r.width * 0.6 / 9));
        int lado = Icones.lado(celula);
        Icones.desenhar(g, "TROFEU", aceso ? Palette.TEXT : Palette.LIGHT,
                r.x + (r.width - lado) / 2, y + (r.height - lado) / 2, celula);

        if (Conquistas.temNovas()) {
            // Pulsa devagar: chama atenção sem gritar.
            double pulso = 0.5 + 0.5 * Math.sin((transicao < 0 ? Engine.alpha() : 0) + System.nanoTime() / 2.5e8);
            int d = 5 * s;
            int bx = r.x + r.width - d + 2 * s, by = y - 2 * s;
            g.setColor(new Color(0xE0475F));
            g.fillOval(bx - (int) (pulso * s), by - (int) (pulso * s), d + (int) (2 * pulso * s), d + (int) (2 * pulso * s));
            g.setColor(new Color(0xFFD0D6));                        // brilho do ponto
            g.fillRect(bx + s, by + s, Math.max(1, s), Math.max(1, s));
        }
        if (trofeu.isHovered() || trofeu.isFocused()) {
            Font f = FontHandler.font(FontHandler.Septem, 6f * s);
            String dica = "Achievements";
            TextFx.draw(g, dica, f, r.x + r.width - FontHandler.getWidth(dica, f), r.y + r.height + 9 * s,
                    Palette.TEXT, s);
        }
        g.translate(-dx, 0);
    }

    /**
     * Quanto o botao i ja andou para fora da tela. Eles saem em cascata, alternando
     * os lados, com aceleracao (ease-in) para parecer que sao puxados.
     */
    private int deslocamentoDeSaida(int i) {
        if (transicao < 0) {
            return 0;
        }
        int duracao = DURACAO_TRANSICAO - ATRASO_POR_BOTAO * (botoes.size() - 1);
        double t = Math.clamp((transicaoNoQuadro() - i * ATRASO_POR_BOTAO) / duracao, 0, 1);
        double distancia = Engine.window.getWidth() / 2.0 + Button.preferredWidth();
        int lado = i % 2 == 0 ? -1 : 1;
        return (int) (lado * distancia * t * t * t);
    }

    /** Escurece a tela junto com a saida, para Options entrar sem corte seco. */
    private void desenharEscurecimento(Graphics2D g) {
        if (transicao < 0) {
            return;
        }
        int alfa = (int) Math.min(200, 200 * transicaoNoQuadro() / DURACAO_TRANSICAO);
        g.setColor(new Color(0, 0, 0, alfa));
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
    }

    /** Progresso da transicao interpolado entre ticks, para fluir acima de 60 FPS. */
    private double transicaoNoQuadro() {
        return transicao + Engine.alpha();
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
