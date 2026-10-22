package com.retronova.menus;

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
import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * A tela inicial: a raiz do sistema de menus.
 *
 * Ela usa a mesma moldura das outras (ver TelaDeMenu) com duas diferenças, as
 * duas de propósito: não tem título, porque o letreiro já está na ilustração, e
 * não escurece a ilustração, porque aqui ela é a protagonista. Saves, opções e
 * conquistas abrem por cima dela, com a mesma ilustração escurecida — a cena não
 * troca, só recua.
 */
public class Menu extends TelaDeMenu {

    private final List<Button> botoes;
    /** O troféu das conquistas: o último da lista, mas fora da coluna central. */
    private Button trofeu;

    /** Índice do botão sob foco de teclado; -1 quando o mouse está no comando. */
    private int foco = -1;

    // ----------------------------------------------------------- ajuste fino

    /** Ticks entre um elemento e o próximo na entrada em sequência, e quanto cada um leva para surgir. */
    public static final int ATRASO_ENTRE_ELEMENTOS = 7, SURGIR_DO_ELEMENTO = 14;
    /** Quanto cada elemento sobe ao surgir, em pixels nativos. */
    public static final int SUBIDA_DO_ELEMENTO = 6;

    /**
     * Ticks desde que a entrada em sequência começou, ou -1 sem ela. Com ela, os
     * botões, o troféu e a versão surgem um depois do outro (ver Abertura): a tela
     * inicial chega como continuação da abertura, e não toda de uma vez.
     */
    private int desdeAEntrada = -1;

    public Menu() {
        this(true);
    }

    /** @param tocarMusica false quando a abertura vai trazer a música por desvanecimento */
    public Menu(boolean tocarMusica) {
        super(null);
        this.botoes = new ArrayList<>();
        criarBotoes();
        if (tocarMusica) {
            tocarMusica();
        }
    }

    /** Faz os elementos surgirem um a um a partir de agora. */
    public void entrarEmSequencia() {
        desdeAEntrada = 0;
    }

    /** De 0 a 1: quanto o elemento de ordem i já surgiu. */
    private float surgimento(int i) {
        if (desdeAEntrada < 0) {
            return 1f;
        }
        float t = (desdeAEntrada + Engine.alpha() - i * ATRASO_ENTRE_ELEMENTOS) / SURGIR_DO_ELEMENTO;
        float f = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - f) * (1f - f) * (1f - f);   // ease-out cúbico
    }

    /** A trilha da tela inicial, do começo. Chamada ao abrir e ao voltar da partida. */
    public void tocarMusica() {
        Sound.stopAll();
        Sound.play(Musics.Menu, true);
        Sound.play(Musics.Ambiente, true);
    }

    private void criarBotoes() {
        // "PLAY" ABRE A TELA DE CORRIDAS, e nao a selecao de personagem.
        //
        // Com tres espacos de save, o que se escolhe aqui deixou de ser uma ACAO e
        // passou a ser uma CORRIDA, e corrida se escolhe vendo. Ver Saves.
        //
        // SEM MIADO AQUI. Quem mia e o botao "Embark" da selecao de personagem, com
        // a voz DAQUELE gato. La o som responde a uma escolha; aqui ele so acontecia.
        botoes.add(new Button(0, 0, 0, 0, "Play", b -> abrir(new Saves())).primary());
        botoes.add(new Button(0, 0, 0, 0, "Settings", b -> abrir(new Configuracoes())));
        //Sair pela pausa já perguntava; aqui fechava direto. Mesmo diálogo nos dois.
        botoes.add(new Button(0, 0, 0, 0, "Quit", b -> perguntarSeSai()));
        // O TROFEU E O ULTIMO DA LISTA, para o teclado alcancá-lo como aos outros,
        // mas nao entra na coluna central: posicionar o leva para o canto.
        trofeu = new Button(0, 0, 0, 0, "", b -> abrir(new TelaDeConquistas()));
        botoes.add(trofeu);
    }

    private void perguntarSeSai() {
        confirmacao.perguntar("Do you want to quit?", Engine::CLOSE);
    }

    /** Na raiz não há para onde voltar: Esc e B perguntam se é para sair. */
    @Override
    protected void voltar() {
        perguntarSeSai();
    }

    /** A ilustração aqui é a protagonista: sem véu. */
    @Override
    protected boolean veuSobreAIlustracao() {
        return false;
    }

    @Override
    protected List<Rodape.Dica> dicas() {
        return List.of(Rodape.SELECIONAR, new Rodape.Dica("Esc", "B", "Quit"));
    }

    /** Recalcula posições a partir da escala da UI, para os cantos do 9-slice caírem inteiros. */
    @Override
    protected void posicionar() {
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int espaco = altura + 6 * Configs.UiScale();

        int x = Engine.window.largura() / 2 - largura / 2;
        // Sem o troféu: ele não faz parte da coluna.
        int naColuna = botoes.size() - 1;
        int y = Engine.window.altura() / 2 - (naColuna * espaco) / 2;

        for (int i = 0; i < naColuna; i++) {
            botoes.get(i).setBounds(new Rectangle(x, y + i * espaco, largura, altura));
        }
        // No canto superior direito, pequeno e quadrado, com a margem da interface.
        int lado = 13 * Configs.UiScale();
        trofeu.setBounds(new Rectangle(Engine.window.largura() - Configs.Margin() - lado,
                Configs.Margin(), lado, lado));
    }

    @Override
    protected void atualizar() {
        if (desdeAEntrada >= 0) {
            desdeAEntrada++;
            if (desdeAEntrada > botoes.size() * ATRASO_ENTRE_ELEMENTOS + SURGIR_DO_ELEMENTO + 2) {
                desdeAEntrada = -1;     // todos chegaram: a tela segue sem custo extra
            }
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
    protected void desenharConteudo(Graphics2D g) {
        int i = 0;
        for (Button botao : botoes) {
            if (botao != trofeu) {
                // o troféu tem desenho próprio, logo abaixo
                emSequencia(g, i++, gg -> botao.render(gg));
            }
        }
        emSequencia(g, i++, this::desenharTrofeu);
        emSequencia(g, i, this::desenharVersao);
    }

    /** Desenha o elemento de ordem i já no ponto da entrada em sequência: subindo e aparecendo. */
    private void emSequencia(Graphics2D g, int i, java.util.function.Consumer<Graphics2D> desenho) {
        float f = surgimento(i);
        if (f >= 1f) {
            desenho.accept(g);
            return;
        }
        if (f <= 0f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, f));
        g2.translate(0, Math.round((1 - f) * SUBIDA_DO_ELEMENTO * Configs.UiScale()));
        desenho.accept(g2);
        g2.dispose();
    }

    /**
     * O troféu por cima do botão vazio: o ícone, um selo vermelho se há conquista
     * nova que o jogador não viu, e a dica no foco ou com o mouse em cima.
     */
    private void desenharTrofeu(Graphics2D g) {
        int s = Configs.UiScale();
        Rectangle r = trofeu.getBounds();

        // UM QUADRADO NEUTRO, e não um botão com orelhas. O ouro do troféu competia
        // com o resto do menu, que é todo em ardósia; aqui o ícone usa a cor de
        // texto secundário e só clareia quando o mouse chega perto.
        //
        // E ele REAGE: sobe até 2 pixels, a borda acende e o ícone vai para o
        // branco. A cor só troca em dois valores (e não num degradê) porque os
        // ícones são pintados uma vez e guardados por cor.
        double h = hoverDoTrofeu;
        boolean aceso = h > 0.5;
        int sobe = (int) Math.round(2 * s * h);
        int y = r.y - sobe;

        g.setColor(new Color(0, 0, 0, 70));                       // sombra no "chão"
        g.fillRoundRect(r.x + s, r.y + 2 * s, r.width, r.height, 5 * s, 5 * s);
        // O brilho comum (ver Realce), e não mais a borda acendendo.
        com.retronova.menus.shared.Realce.brilho(g, new Rectangle(r.x, y, r.width, r.height), (float) h, s);
        if (trofeu.isFocused()) {
            com.retronova.menus.shared.Realce.marcadorDeFoco(g, new Rectangle(r.x, y, r.width, r.height), s);
        }
        g.setColor(Palette.OUTLINE);                              // borda
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
            double pulso = 0.5 + 0.5 * Math.sin(System.nanoTime() / 2.5e8);
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
    }

    /**
     * Carimbo de versao no canto, onde alguem procura quando vai reportar um
     * problema. Fica na ESQUERDA: o direito e onde costuma ficar o que muda.
     */
    private void desenharVersao(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Septem, 5f * s);
        String texto = Engine.VERSION;
        int x = Configs.Margin();
        int y = Engine.window.altura() - Configs.Margin();
        g.setFont(fonte);
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(texto, x + s, y + s);
        g.setColor(new Color(Palette.TEXT.getRed(), Palette.TEXT.getGreen(), Palette.TEXT.getBlue(), 110));
        g.drawString(texto, x, y);
    }
}
