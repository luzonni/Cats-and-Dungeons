package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.OptionRow;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela de opções.
 *
 * Organizada como lista vertical dentro de abas, e não mais como grade 4x3.
 * Na grade, o "+" de uma opção acabava a 10px do "-" da opção vizinha e a 200px
 * do próprio rótulo, o que fazia os controles parecerem pertencer à coluna
 * errada.
 *
 * Quase tudo se aplica na hora. As exceções são resolução e tela cheia, que
 * exigem recriar a janela; essas ficam pendentes até o botão Aplicar.
 */
public class Options implements Activity {

    private static final int[] FPS_VALORES = {30, 60, 120, 144};
    /** 0 = automática: a escala acompanha o tamanho da janela. */
    private static final int[] ESCALAS_UI = {0, 2, 3, 4, 5, 6};
    private static final String[] ROTULOS_ESCALA = {"Auto", "2", "3", "4", "5", "6"};

    private final String[] abas = {"Video", "Audio", "Interface"};
    private final List<List<OptionRow>> linhas = new ArrayList<>();
    private Rectangle[] abasRect;
    private int abaAtual;
    private int foco;
    private int yTitulo;
    private Rectangle painel = new Rectangle();

    // Mudanças que só podem ser aplicadas recriando a janela.
    private int pendResolucao = Configs.getIndexResolution();
    private int pendTelaCheia = Configs.Fullscreen() ? 1 : 0;

    private final Button aplicar;
    private final Button voltar;

    private BufferedImage imagemFundo;
    /** Quando aberta por cima do jogo, não desenha fundo próprio. */
    private final boolean sobreposta;
    private final Runnable aoFechar;

    /**
     * Abre as opções por cima da cena atual, sem trocar de Activity.
     *
     * Empilhar as opções a partir da pausa trocava a tela e trazia junto o fundo
     * da tela inicial, dando a impressão de que o jogador tinha saído da partida.
     *
     * @param aoFechar o que fazer no Back, no lugar de desempilhar a Activity
     */
    public static Options sobreposta(Runnable aoFechar) {
        return new Options(true, aoFechar);
    }

    public Options() {
        this(false, null);
    }

    private Options(boolean sobreposta, Runnable aoFechar) {
        this.sobreposta = sobreposta;
        this.aoFechar = aoFechar;
        montarLinhas();
        this.abasRect = new Rectangle[abas.length];
        for (int i = 0; i < abasRect.length; i++) {
            abasRect[i] = new Rectangle();
        }
        this.aplicar = new Button(0, 0, 0, 0, "Apply", b -> aplicarPendentes()).primary();
        this.voltar = new Button(0, 0, 0, 0, "Back", b -> fechar());

        try {
            imagemFundo = ImageIO.read(getClass().getResource(
                    "/com/retronova/resources/icons/Gato_fundo.png"));
        } catch (Exception e) {
            System.err.println("Options: fundo não carregado: " + e);
        }

        //Sem isto, o primeiro render pode acontecer antes do primeiro tick e
        //desenhar tudo em (0,0) — a piscada no canto ao abrir a tela.
        posicionar();
    }

    private void montarLinhas() {
        String[] resolucoes = new String[Engine.resolutions.length];
        for (int i = 0; i < resolucoes.length; i++) {
            resolucoes[i] = Engine.resolutions[i][0] + " x " + Engine.resolutions[i][1];
        }
        String[] fps = new String[FPS_VALORES.length];
        for (int i = 0; i < fps.length; i++) {
            fps[i] = String.valueOf(FPS_VALORES[i]);
        }

        List<OptionRow> video = new ArrayList<>();
        video.add(OptionRow.cycle("Resolution", () -> pendResolucao, v -> pendResolucao = v, resolucoes)
                .desc("Size of the game window. Takes effect only after Apply, which recreates the window."));
        video.add(OptionRow.toggle("Fullscreen", () -> pendTelaCheia, v -> pendTelaCheia = v)
                .desc("Fills the whole screen with a borderless window. Needs Apply. F11 also toggles it."));
        video.add(OptionRow.cycle("Frame rate", Options::indiceFps,
                v -> Configs.setMaxFrames(FPS_VALORES[v]), fps)
                .desc("Maximum frames drawn per second. Higher values look smoother but cost more CPU."));
        video.add(OptionRow.toggle("Smooth graphics",
                () -> Configs.isNeatGraphics() ? 1 : 0, v -> Configs.setNeatGraphics(v != 0))
                .desc("Antialiasing and smooth filtering. Softens the pixel art; off keeps the sharp look."));
        video.add(OptionRow.stepper("Camera zoom", Configs::Zoom, Configs::setZoom, 100, 160, 10, "%")
                .desc("Quao perto a camera fica do gato. Mais perto, mais imersao; mais longe, mais campo de visao."));
        video.add(OptionRow.toggle("Vignette",
                () -> Configs.Vignette() ? 1 : 0, v -> Configs.setVignette(v != 0))
                .desc("Darkens the corners of the screen during gameplay, to focus the eye on the center."));

        List<OptionRow> audio = new ArrayList<>();
        audio.add(OptionRow.slider("Music", Configs::Music, v -> {
            Configs.setMusic(v);
            Sound.updateVolumes();     // aplica ao vivo: é preciso ouvir para ajustar
        }, 0, 100, 5, "%").desc("Volume of the background music. Applies immediately."));
        audio.add(OptionRow.slider("Sound effects", Configs::Volum, Configs::setVolum, 0, 100, 5, "%")
                .desc("Volume of hits, footsteps, items and enemies. Menu sounds included."));

        List<OptionRow> ui = new ArrayList<>();
        ui.add(OptionRow.cycle("Text size", Options::indiceEscala,
                v -> Configs.setUiScale(ESCALAS_UI[v]), ROTULOS_ESCALA)
                .desc("Scale of menus, buttons and text. Auto follows the window size."));
        ui.add(OptionRow.stepper("HUD size", Configs::HudScale, Configs::setHudScale, 2, 6, 1)
                .desc("Scale of the in-game interface: life bar, hotbar, inventory and shop."));
        ui.add(OptionRow.stepper("Margin", Configs::Margin, Configs::setMargin, 0, 40, 5)
                .desc("Distance between the interface and the edges of the screen."));

        linhas.add(video);
        linhas.add(audio);
        linhas.add(ui);
    }

    private static int indiceEscala() {
        int atual = Configs.UiScaleSetting();
        for (int i = 0; i < ESCALAS_UI.length; i++) {
            if (ESCALAS_UI[i] == atual) {
                return i;
            }
        }
        return 0;   // valor desconhecido cai no automático
    }

    private static int indiceFps() {
        int atual = Configs.MaxFrames();
        for (int i = 0; i < FPS_VALORES.length; i++) {
            if (FPS_VALORES[i] == atual) {
                return i;
            }
        }
        return 1;
    }

    private boolean temPendencia() {
        return pendResolucao != Configs.getIndexResolution()
                || (pendTelaCheia != 0) != Configs.Fullscreen();
    }

    private void aplicarPendentes() {
        if (!temPendencia()) {
            return;
        }
        Configs.setIndexResolution(pendResolucao);
        Configs.setFullscreen(pendTelaCheia != 0);
        Engine.window.resetWindow();
    }

    /** Volta para quem abriu: a pausa, quando sobreposta; a pilha, quando não. */
    private void fechar() {
        if (aoFechar != null) {
            aoFechar.run();
        } else {
            Engine.backActivity();
        }
    }

    private List<OptionRow> atuais() {
        return linhas.get(abaAtual);
    }

    // ---------------------------------------------------------------- layout

    /** Maior número de linhas entre as abas. Define uma altura fixa para o painel. */
    private int maxLinhas() {
        int maior = 0;
        for (List<OptionRow> aba : linhas) {
            maior = Math.max(maior, aba.size());
        }
        return maior;
    }

    /**
     * O bloco inteiro — título, abas, painel e botões — é medido primeiro e
     * então centralizado na janela. Antes ficava ancorado no topo, o que em
     * telas grandes deixava a tela toda amontoada num canto.
     */
    private void posicionar() {
        int s = Configs.UiScale();
        int larguraPainel = Math.min(Engine.window.getWidth() - 16 * s, 220 * s);
        int x = Engine.window.getWidth() / 2 - larguraPainel / 2;

        int alturaTitulo = 18 * s;
        int alturaAbas = 13 * s;
        //Altura calculada pela aba MAIS LONGA, não pela aba atual: assim o painel
        //e os botões ficam parados ao alternar entre Video, Audio e Interface.
        int alturaLinhas = maxLinhas() * (OptionRow.height() + 2 * s) + 8 * s;
        int alturaBotao = Button.preferredHeight();
        int total = alturaTitulo + 6 * s + alturaAbas + alturaLinhas + 6 * s + alturaBotao;

        int topo = Math.max(4 * s, (Engine.window.getHeight() - total) / 2);
        this.yTitulo = topo + alturaTitulo;

        int yAbas = topo + alturaTitulo + 6 * s;
        int larguraAba = larguraPainel / abas.length;
        for (int i = 0; i < abas.length; i++) {
            abasRect[i] = new Rectangle(x + i * larguraAba, yAbas, larguraAba, alturaAbas);
        }
        this.painel = new Rectangle(x, yAbas + alturaAbas, larguraPainel, alturaLinhas);

        //Posiciona as linhas de TODAS as abas, não só a visível. Só a atual era
        //posicionada, então a aba recém-aberta desenhava um quadro com as
        //coordenadas zeradas — a piscada no canto da tela.
        int y = yAbas + alturaAbas + 4 * s;
        for (List<OptionRow> aba : linhas) {
            for (int i = 0; i < aba.size(); i++) {
                aba.get(i).setBounds(x + 5 * s, y + i * (OptionRow.height() + 2 * s),
                        larguraPainel - 10 * s);
            }
        }

        int larguraBotao = Button.preferredWidth();
        int yBotoes = yAbas + alturaAbas + alturaLinhas + 6 * s;
        aplicar.setBounds(new Rectangle(x, yBotoes, larguraBotao, alturaBotao));
        voltar.setBounds(new Rectangle(x + larguraPainel - larguraBotao, yBotoes,
                larguraBotao, alturaBotao));
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        posicionar();

        if (KeyBoard.KeyPressed("Escape")) {
            fechar();
            return;
        }
        trocarAbaPeloMouse();
        navegarPeloTeclado();

        List<OptionRow> rows = atuais();
        for (int i = 0; i < rows.size(); i++) {
            OptionRow linha = rows.get(i);
            linha.setFocado(foco == i);
            linha.tick();
            if (linha.estaSobreControles()) {
                Engine.window.pointing();
            }
        }

        if (temPendencia()) {
            aplicar.tick();
            if (aplicar.isHovered()) {
                Engine.window.pointing();
            }
        }
        voltar.tick();
        if (voltar.isHovered()) {
            Engine.window.pointing();
        }
    }

    private void trocarAbaPeloMouse() {
        for (int i = 0; i < abasRect.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, abasRect[i]) && i != abaAtual) {
                abaAtual = i;
                foco = 0;
                Sound.play(Sounds.Button);
                return;
            }
            if (abasRect[i].contains(Mouse.getX(), Mouse.getY())) {
                Engine.window.pointing();
            }
        }
    }

    private void navegarPeloTeclado() {
        List<OptionRow> rows = atuais();
        if (KeyBoard.KeyPressed("Down")) {
            foco = (foco + 1) % rows.size();
        } else if (KeyBoard.KeyPressed("Up")) {
            foco = (foco <= 0 ? rows.size() : foco) - 1;
        } else if (KeyBoard.KeyPressed("Right")) {
            rows.get(Math.min(foco, rows.size() - 1)).ajustarPeloTeclado(1);
        } else if (KeyBoard.KeyPressed("Left")) {
            rows.get(Math.min(foco, rows.size() - 1)).ajustarPeloTeclado(-1);
        }
        if (foco >= rows.size()) {
            foco = rows.size() - 1;
        }
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(Graphics2D g) {
        //Sobreposta, o cenário do jogo é o fundo; só o véu escurece atrás.
        if (!sobreposta && imagemFundo != null) {
            g.drawImage(imagemFundo, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }
        g.setColor(Palette.VEIL);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());

        desenharTitulo(g);
        desenharPainel(g);
        desenharAbas(g);

        for (OptionRow linha : atuais()) {
            linha.render(g);
        }
        if (temPendencia()) {
            aplicar.render(g);
            desenharAvisoPendente(g);
        }
        voltar.render(g);
        desenharTooltip(g);
    }

    private void desenharTitulo(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 14f * s);
        String titulo = "Options";
        int w = FontHandler.getWidth(titulo, fonte);
        int x = Engine.window.getWidth() / 2 - w / 2;
        int y = yTitulo;
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(titulo, x + s, y + s);
        g.setColor(Palette.LIGHT);
        g.drawString(titulo, x, y);
    }

    /** Painel de fundo atrás das linhas. Altura fixa, igual em todas as abas. */
    private void desenharPainel(Graphics2D g) {
        int s = Configs.UiScale();
        g.setColor(Palette.OUTLINE);
        g.fillRect(painel.x, painel.y, painel.width, painel.height);
        g.setColor(Palette.DEEP);
        g.fillRect(painel.x + s, painel.y + s, painel.width - 2 * s, painel.height - 2 * s);
        g.setColor(Palette.OUTLINE);
        g.fillRect(painel.x + 3 * s, painel.y + 3 * s, painel.width - 6 * s, s);
    }

    private void desenharAbas(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 7f * s);
        g.setFont(fonte);

        for (int i = 0; i < abas.length; i++) {
            Rectangle r = abasRect[i];
            boolean ativa = i == abaAtual;
            boolean sobre = r.contains(Mouse.getX(), Mouse.getY());

            g.setColor(Palette.OUTLINE);
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(ativa ? Palette.DEEP : (sobre ? Palette.MAIN : Palette.OUTLINE));
            g.fillRect(r.x + s, r.y + s, r.width - 2 * s, r.height - s);
            if (ativa) {
                g.setColor(Palette.LIGHT);
                g.fillRect(r.x + s, r.y + s, r.width - 2 * s, s);
            }

            int w = FontHandler.getWidth(abas[i], fonte);
            int h = FontHandler.getHeight(abas[i], fonte);
            int tx = r.x + (r.width - w) / 2;
            int ty = r.y + (r.height + h) / 2;
            g.setColor(Palette.OUTLINE);
            g.drawString(abas[i], tx + s, ty + s);
            g.setColor(ativa ? Palette.TEXT : Palette.LIGHT);
            g.drawString(abas[i], tx, ty);
        }
    }

    private void desenharAvisoPendente(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        String aviso = "Apply restarts the window";
        int x = aplicar.getBounds().x;          // alinhado à esquerda, sob o botão
        int y = aplicar.getBounds().y - 2 * s;
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(aviso, x + s, y + s);
        g.setColor(Palette.LIGHT);
        g.drawString(aviso, x, y);
    }

    /**
     * Balão de ajuda da linha sob o mouse. Desenhado por último, acima de tudo,
     * e preso dentro da janela para não vazar pelas bordas.
     */
    private void desenharTooltip(Graphics2D g) {
        OptionRow alvo = null;
        for (OptionRow linha : atuais()) {
            if (linha.estaSobreLinha() && !linha.getDescricao().isEmpty()) {
                alvo = linha;
                break;
            }
        }
        if (alvo == null) {
            return;
        }
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        int larguraMax = 96 * s;
        List<String> linhasTexto = quebrar(alvo.getDescricao(), fonte, larguraMax);

        int alturaLinha = 7 * s;
        int pad = 4 * s;
        int largura = 0;
        for (String t : linhasTexto) {
            largura = Math.max(largura, FontHandler.getWidth(t, fonte));
        }
        int caixaW = largura + pad * 2;
        int caixaH = linhasTexto.size() * alturaLinha + pad * 2;

        int x = Mouse.getX() + 4 * s;
        int y = Mouse.getY() + 6 * s;
        x = Math.min(x, Engine.window.getWidth() - caixaW - 2 * s);
        y = Math.min(y, Engine.window.getHeight() - caixaH - 2 * s);
        x = Math.max(2 * s, x);
        y = Math.max(2 * s, y);

        g.setColor(Palette.OUTLINE);
        g.fillRect(x, y, caixaW, caixaH);
        g.setColor(Palette.DEEP);
        g.fillRect(x + s, y + s, caixaW - 2 * s, caixaH - 2 * s);
        g.setColor(Palette.MAIN);
        g.fillRect(x + s, y + s, caixaW - 2 * s, s);

        g.setFont(fonte);
        for (int i = 0; i < linhasTexto.size(); i++) {
            int ty = y + pad + (i + 1) * alturaLinha - 2 * s;
            g.setColor(Palette.OUTLINE);
            g.drawString(linhasTexto.get(i), x + pad + s, ty + s);
            g.setColor(Palette.TEXT);
            g.drawString(linhasTexto.get(i), x + pad, ty);
        }
    }

    /** Quebra o texto em linhas que caibam na largura dada. */
    private List<String> quebrar(String texto, Font fonte, int largura) {
        List<String> saida = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String teste = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (FontHandler.getWidth(teste, fonte) > largura && !atual.isEmpty()) {
                saida.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(teste);
            }
        }
        if (!atual.isEmpty()) {
            saida.add(atual.toString());
        }
        return saida;
    }

    @Override
    public void dispose() {
    }
}
