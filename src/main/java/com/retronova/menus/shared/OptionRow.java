package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/**
 * Uma linha da tela de opções: rótulo à esquerda, controle à direita.
 *
 * O controle é sempre o mesmo agrupamento — {@code [-] valor [+]} — com os
 * botões colados ao valor que eles alteram, e com folga entre linhas. É o
 * arranjo que a pesquisa de usabilidade recomenda para steppers, e o oposto
 * do layout em grade anterior, onde o "+" de uma opção ficava a 10px do "-"
 * da opção vizinha.
 *
 * Todo valor trafega como inteiro; booleanos usam 0 e 1.
 */
public class OptionRow {

    public enum Kind {
        /** Barra de preenchimento com porcentagem. Faixa larga e contínua. */
        SLIDER,
        /** Número puro. Faixa curta. */
        STEPPER,
        /** Lista discreta de rótulos, circular. */
        CYCLE,
        /** Liga/desliga, sem setas. */
        TOGGLE,
        /**
         * Uma ação (Continue, Restart, Quit...): só o rótulo e uma seta no lugar
         * dos controles. Enter, A ou um clique na linha inteira a executam.
         */
        ACTION,
        /**
         * Um tamanho entre poucos, um bloco para cada (Menu size, HUD size): só os
         * que existem nesta tela, e cada um muda o desenho de verdade.
         */
        TAMANHO
    }

    // Geometria em pixels nativos; tudo é multiplicado pela escala da UI.
    private static final int ROW_H = 14;
    private static final int CLUSTER_W = 92;
    private static final int BTN_W = 12;
    private static final int CONTENT_X = 14, CONTENT_W = 64;
    private static final int PLUS_X = 80;
    /** Largura da trilha da barra arrastável; o número fica à direita dela, com ~28 livres. */
    private static final int TRILHA_W = 58;
    /** Altura da barra, com o contorno: fina, mas com miolo para o preenchimento. */
    private static final int BARRA_H = 6;
    /** Os blocos de tamanho: largura, altura e vão; seis cabem em 46, e o nome em ~40. */
    private static final int BLOCO_W = 6, BLOCO_H = 8, BLOCO_VAO = 2;
    /** Folga mínima entre o fim do rótulo e o começo dos controles. */
    private static final int LABEL_GAP = 4;

    private final String label;
    private final Kind kind;
    private final IntSupplier get;
    private final IntConsumer set;
    private final int min, max, step;
    private final String[] rotulos;
    private final String sufixo;

    private String descricao = "";

    private Rectangle bounds = new Rectangle();
    private Rectangle menos = new Rectangle();
    private Rectangle mais = new Rectangle();
    private Rectangle alvoToggle = new Rectangle();
    private Rectangle trilha = new Rectangle();
    /** O mouse segura a barra: o valor acompanha até soltar. */
    private boolean arrastando;
    /** A trilha como estava ao começar o arrasto: o arrasto mede contra ela. */
    private Rectangle trilhaDoArrasto = new Rectangle();
    /** Nos blocos de tamanho, o bloco sob o mouse enquanto segura; aplicado ao soltar. */
    private int pendente;
    /** O nome de cada valor dos blocos de tamanho. */
    private IntFunction<String> nome;

    private Runnable acao;
    /** O realce da linha, em rampa: aceso pelo foco ou pelo mouse (ver Realce). */
    private final Realce.Estado realce = new Realce.Estado();

    private boolean focado;
    private int seguraTicks;
    private int seguraDir;

    private OptionRow(String label, Kind kind, IntSupplier get, IntConsumer set,
                      int min, int max, int step, String[] rotulos, String sufixo) {
        this.label = label;
        this.kind = kind;
        this.get = get;
        this.set = set;
        this.min = min;
        this.max = max;
        this.step = step;
        this.rotulos = rotulos;
        this.sufixo = sufixo;
    }

    public static OptionRow slider(String label, IntSupplier get, IntConsumer set,
                                   int min, int max, int step, String sufixo) {
        return new OptionRow(label, Kind.SLIDER, get, set, min, max, step, null, sufixo);
    }

    public static OptionRow stepper(String label, IntSupplier get, IntConsumer set,
                                    int min, int max, int step) {
        return stepper(label, get, set, min, max, step, "");
    }

    /** Stepper com unidade ao lado do numero — "%", "px", e por ai. */
    public static OptionRow stepper(String label, IntSupplier get, IntConsumer set,
                                    int min, int max, int step, String sufixo) {
        return new OptionRow(label, Kind.STEPPER, get, set, min, max, step, null, sufixo);
    }

    public static OptionRow cycle(String label, IntSupplier get, IntConsumer set, String[] rotulos) {
        return new OptionRow(label, Kind.CYCLE, get, set, 0, rotulos.length - 1, 1, rotulos, "");
    }

    public static OptionRow toggle(String label, IntSupplier get, IntConsumer set) {
        return new OptionRow(label, Kind.TOGGLE, get, set, 0, 1, 1, new String[]{"Off", "On"}, "");
    }

    /** Uma linha que executa uma ação, no lugar de ajustar um valor. */
    public static OptionRow acao(String label, Runnable acao) {
        OptionRow linha = new OptionRow(label, Kind.ACTION, () -> 0, v -> { }, 0, 0, 0, null, "");
        linha.acao = acao;
        return linha;
    }

    /**
     * Um tamanho entre poucos: um bloco por valor, de {@code menor} a {@code maior}
     * (calculados na hora, pela tela), e o nome do valor escolhido à direita.
     */
    public static OptionRow tamanho(String label, IntSupplier get, IntConsumer set,
                                    IntSupplier menor, IntSupplier maior, IntFunction<String> nome) {
        OptionRow linha = new OptionRow(label, Kind.TAMANHO, get, set,
                Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, 1, null, "");
        linha.piso = menor;
        linha.teto = maior;
        linha.nome = nome;
        return linha;
    }

    public boolean ehAcao() {
        return kind == Kind.ACTION;
    }

    /** Enter e A: executa a ação, ou avança o valor como a seta da direita. */
    public void ativar() {
        if (kind == Kind.ACTION) {
            Sound.play(Sounds.Button);
            acao.run();
            return;
        }
        ajustar(1);
    }

    /** Um teto e um piso que mudam com a tela (o HUD que cabe na resolução atual), ou null. */
    private IntSupplier teto, piso;

    /**
     * Limita o máximo a um valor calculado na hora. Encadeável. Com ele o "+" para
     * no que cabe, em vez de aceitar um valor que não teria efeito.
     */
    public OptionRow ate(IntSupplier teto) {
        this.teto = teto;
        return this;
    }

    private int minimo() {
        return piso == null ? min : Math.max(min, Math.min(max, piso.getAsInt()));
    }

    private int maximo() {
        return teto == null ? max : Math.max(minimo(), Math.min(max, teto.getAsInt()));
    }

    /** Texto de ajuda exibido ao passar o mouse pela linha. Encadeável. */
    public OptionRow desc(String descricao) {
        this.descricao = descricao;
        return this;
    }

    public String getDescricao() {
        return this.descricao;
    }

    /** Mouse em qualquer ponto da linha, não só sobre os controles. */
    public boolean estaSobreLinha() {
        return bounds.contains(Mouse.getX(), Mouse.getY());
    }

    public static int height() {
        return ROW_H * Configs.UiScale();
    }

    public String getLabel() {
        return this.label;
    }

    public void setFocado(boolean focado) {
        this.focado = focado;
    }

    /** Define a área da linha e deriva a posição dos controles. */
    public void setBounds(int x, int y, int width) {
        int s = Configs.UiScale();
        this.bounds = new Rectangle(x, y, width, ROW_H * s);

        int clusterX = x + width - CLUSTER_W * s;
        int btnY = y + (ROW_H - BTN_W) / 2 * s;
        this.menos = new Rectangle(clusterX, btnY, BTN_W * s, BTN_W * s);
        this.mais = new Rectangle(clusterX + PLUS_X * s, btnY, BTN_W * s, BTN_W * s);
        // Alvo generoso: clicar em qualquer ponto da coluna de controles alterna.
        this.alvoToggle = new Rectangle(clusterX, btnY, CLUSTER_W * s, BTN_W * s);
        // A trilha da barra ocupa a altura toda da linha: fácil de pegar com o mouse.
        this.trilha = new Rectangle(clusterX + 2 * s, y, TRILHA_W * s, ROW_H * s);
    }

    /** Barra arrastável: os valores numéricos (volume, zoom, tamanhos, margem). */
    private boolean ehBarra() {
        return kind == Kind.SLIDER || kind == Kind.STEPPER;
    }

    public Rectangle getBounds() {
        return this.bounds;
    }

    public boolean estaSobreControles() {
        if (kind == Kind.ACTION) {
            return estaSobreLinha();
        }
        if (ehBarra() || kind == Kind.TAMANHO) {
            return arrastando || trilha.contains(Mouse.getX(), Mouse.getY());
        }
        return menos.contains(Mouse.getX(), Mouse.getY())
                || mais.contains(Mouse.getX(), Mouse.getY())
                || (kind == Kind.TOGGLE && alvoToggle.contains(Mouse.getX(), Mouse.getY()));
    }

    public void tick() {
        realce.tick(focado || (com.retronova.engine.inputs.Entrada.porMouse() && estaSobreLinha()));
        if (kind == Kind.ACTION) {
            if (Mouse.clickOn(Mouse_Button.LEFT, bounds)) {
                ativar();
            }
            return;
        }
        if (kind == Kind.TOGGLE) {
            if (Mouse.clickOn(Mouse_Button.LEFT, alvoToggle) || Mouse.clickOn(Mouse_Button.LEFT, menos)
                    || Mouse.clickOn(Mouse_Button.LEFT, mais)) {
                ajustar(1);
            }
            return;
        }
        if (ehBarra()) {
            arrastar();
            return;
        }
        if (kind == Kind.TAMANHO) {
            escolherBloco();
            return;
        }
        if (Mouse.clickOn(Mouse_Button.LEFT, menos)) {
            ajustar(-1);
        } else if (Mouse.clickOn(Mouse_Button.LEFT, mais)) {
            ajustar(1);
        }
        repetirEnquantoSegura();
    }

    /**
     * Clicar na trilha leva o valor até ali, e arrastar o acompanha.
     *
     * COMO NO FORZA HORIZON, e não mais de passo em passo nos botões − e +: num
     * intervalo de 50 a 200 em passos de 5, eram trinta cliques para ir de uma
     * ponta à outra. O arrasto continua até soltar, mesmo com o ponteiro fora da
     * linha. O valor anda no passo da opção, nunca passa do teto dela (ver ate).
     *
     * MEDE CONTRA A TRILHA DO COMEÇO DO ARRASTO. Medir contra a trilha do quadro
     * fazia a tela piscar: um valor que muda o tamanho dos menus muda a trilha, o
     * mouse parado cai noutro valor, que muda a trilha de volta, a cada quadro.
     */
    private void arrastar() {
        if (!arrastando && Mouse.clickOn(Mouse_Button.LEFT, trilha)) {
            arrastando = true;
            trilhaDoArrasto = new Rectangle(trilha);
        }
        if (!arrastando) {
            return;
        }
        if (!Mouse.pressing(Mouse_Button.LEFT)) {
            arrastando = false;
            Sound.play(Sounds.Button);      // o "assentou" do valor, uma vez, ao soltar
            return;
        }
        Rectangle t = trilhaDoArrasto;
        double frac = Math.clamp((Mouse.getX() - t.x) / (double) Math.max(1, t.width), 0, 1);
        double bruto = min + frac * (max - min);
        int v = min + (int) Math.round((bruto - min) / step) * step;
        v = Math.max(minimo(), Math.min(maximo(), v));
        if (v != get.getAsInt()) {
            set.accept(v);
        }
    }

    /**
     * Clicar num bloco de tamanho o escolhe; segurar e arrastar passa pelos
     * blocos, e o tamanho só se aplica AO SOLTAR. Aplicar no meio do arrasto
     * redimensionaria a própria linha debaixo do mouse (o mesmo pisca de arrastar).
     */
    private void escolherBloco() {
        if (!arrastando && Mouse.clickOn(Mouse_Button.LEFT, trilha)) {
            arrastando = true;
            trilhaDoArrasto = new Rectangle(trilha);
        }
        if (!arrastando) {
            return;
        }
        int passo = (BLOCO_W + BLOCO_VAO) * Configs.UiScale();
        int indice = (Mouse.getX() - trilhaDoArrasto.x) / Math.max(1, passo);
        pendente = Math.max(minimo(), Math.min(maximo(), minimo() + indice));
        if (!Mouse.pressing(Mouse_Button.LEFT)) {
            arrastando = false;
            if (pendente != get.getAsInt()) {
                set.accept(pendente);
                Sound.play(Sounds.Button);
            }
        }
    }

    /** Segurar a seta continua ajustando, após uma pausa inicial. */
    private void repetirEnquantoSegura() {
        int dir = 0;
        if (Mouse.isPressed(Mouse_Button.LEFT, menos)) {
            dir = -1;
        } else if (Mouse.isPressed(Mouse_Button.LEFT, mais)) {
            dir = 1;
        }
        if (dir != seguraDir) {
            seguraDir = dir;
            seguraTicks = 0;
            return;
        }
        if (dir == 0) {
            return;
        }
        seguraTicks++;
        if (seguraTicks > 30 && seguraTicks % 4 == 0) {
            ajustar(dir);
        }
    }

    /** Ajuste por teclado (setas esquerda/direita). */
    public void ajustarPeloTeclado(int dir) {
        if (kind != Kind.ACTION) {
            ajustar(dir);
        }
    }

    private void ajustar(int dir) {
        int atual = get.getAsInt();
        int novo;
        if (kind == Kind.CYCLE || kind == Kind.TOGGLE) {
            int n = max - min + 1;
            novo = min + ((atual - min + dir) % n + n) % n;   // circular
        } else {
            novo = Math.max(minimo(), Math.min(maximo(), atual + dir * step));
        }
        if (novo == atual) {
            return;      // já no limite: sem som, sem escrita
        }
        set.accept(novo);
        Sound.play(Sounds.Button);
    }

    private boolean noLimite(int dir) {
        if (kind == Kind.CYCLE || kind == Kind.TOGGLE) {
            return false;
        }
        int atual = get.getAsInt();
        return dir < 0 ? atual <= minimo() : atual >= maximo();
    }

    public void render(Graphics2D g2) {
        int s = Configs.UiScale();
        Graphics2D g = (Graphics2D) g2.create();

        // O FOCO E O HOVER NO ESTILO ÚNICO (ver Realce): a faixa da linha clareia,
        // e no foco do teclado e do controle as setas apontam para ela dos dois
        // lados. Antes era um friso de um pixel na lateral, uma terceira linguagem
        // de foco só desta tela, e o mouse não acendia nada.
        Rectangle faixa = new Rectangle(bounds.x - 2 * s, bounds.y, bounds.width + 4 * s, bounds.height);
        Realce.clarear(g, faixa, 0.8f * realce.noQuadro());
        if (focado && com.retronova.engine.inputs.Entrada.porFoco()) {
            Realce.marcadorDeFoco(g, faixa, s);
        }
        boolean aceso = focado || realce.noQuadro() > 0.5f;

        Font fonte = FontHandler.font(FontHandler.Game, 7f * s);
        int alturaTexto = FontHandler.getHeight(label, fonte);
        int baseTexto = bounds.y + (bounds.height + alturaTexto) / 2 - s;

        // O rótulo encolhe até caber antes dos controles, em vez de passar por baixo deles.
        Font fonteLabel = fonte;
        int espacoLabel = menos.x - bounds.x - LABEL_GAP * s;
        float tam = fonteLabel.getSize2D();
        while (FontHandler.getWidth(label, fonteLabel) > espacoLabel && tam > 4f) {
            tam -= 1f;
            fonteLabel = FontHandler.font(FontHandler.Game, tam);
        }

        g.setFont(fonteLabel);
        TextFx.draw(g, label, bounds.x, baseTexto, aceso ? Palette.TEXT : Palette.LIGHT, s);

        if (kind == Kind.ACTION) {
            desenharChevron(g, aceso, s);
        } else if (kind == Kind.TOGGLE) {
            desenharToggle(g, s);
        } else if (ehBarra()) {
            desenharBarra(g, s, fonte, baseTexto, aceso);
        } else if (kind == Kind.TAMANHO) {
            desenharBlocos(g, s, fonte, baseTexto, aceso);
        } else {
            desenharSeta(g, menos, "-", noLimite(-1), s);
            desenharSeta(g, mais, "+", noLimite(1), s);
            desenharConteudo(g, s, fonte, baseTexto);
        }
        g.dispose();
    }

    private void desenharConteudo(Graphics2D g, int s, Font fonte, int baseTexto) {
        int cx = menos.x + CONTENT_X * s;
        int cw = CONTENT_W * s;

        if (kind == Kind.SLIDER) {
            int barraW = cw - 24 * s;
            int barraH = 7 * s;
            int barraY = bounds.y + (bounds.height - barraH) / 2;

            g.setColor(Palette.OUTLINE);
            g.fillRect(cx, barraY, barraW, barraH);
            double frac = (get.getAsInt() - min) / (double) Math.max(1, max - min);
            int preenchido = (int) ((barraW - 2 * s) * frac);
            g.setColor(Palette.DEEP);
            g.fillRect(cx + s, barraY + s, barraW - 2 * s, barraH - 2 * s);
            if (preenchido > 0) {
                g.setColor(Palette.MAIN);
                g.fillRect(cx + s, barraY + s, preenchido, barraH - 2 * s);
                g.setColor(Palette.LIGHT);
                g.fillRect(cx + s, barraY + s, preenchido, s);
            }
            desenharValor(g, fonte, get.getAsInt() + sufixo, cx + barraW + 4 * s, cw - barraW - 4 * s, baseTexto);
        } else {
            String texto = (kind == Kind.CYCLE)
                    ? rotulos[get.getAsInt()]
                    : get.getAsInt() + sufixo;
            desenharValor(g, fonte, texto, cx, cw, baseTexto);
        }
    }

    /**
     * A barra dos valores contínuos (volume, zoom, margem), no idioma dos toggles:
     * contorno de um pixel, miolo fundo e o trecho escolhido cheio, com o número
     * alinhado à direita. A primeira versão era uma linha de um pixel com um
     * quadrado solto por cima, e não conversava com o resto da tela. Com o foco,
     * o preenchimento clareia, como o Realce faz no resto.
     */
    private void desenharBarra(Graphics2D g, int s, Font fonte, int baseTexto, boolean aceso) {
        int x0 = trilha.x, w = trilha.width, h = BARRA_H * s;
        int y = bounds.y + (bounds.height - h) / 2;
        double faixa = Math.max(1, max - min);
        int miolo = w - 2 * s;
        int cheio = (int) Math.round(miolo * (get.getAsInt() - min) / faixa);
        int xTeto = x0 + s + (int) Math.round(miolo * (maximo() - min) / faixa);

        g.setColor(Palette.OUTLINE);
        g.fillRect(x0, y, w, h);
        g.setColor(Palette.DEEP);
        g.fillRect(x0 + s, y + s, xTeto - x0 - s, h - 2 * s);   // além do teto (ver ate), fica o contorno
        g.setColor(aceso || arrastando ? Palette.TEXT : Palette.ACCENT);
        g.fillRect(x0 + s, y + s, cheio, h - 2 * s);

        desenharValorADireita(g, s, fonte, baseTexto, get.getAsInt() + sufixo, x0 + w);
    }

    /**
     * Os blocos de tamanho: um por tamanho que esta tela comporta, cheios até o
     * escolhido, como barras de sinal. Não há bloco para o que não cabe, então não
     * há o que explicar; o nome do tamanho fica à direita.
     */
    private void desenharBlocos(Graphics2D g, int s, Font fonte, int baseTexto, boolean aceso) {
        int de = minimo(), ate = maximo();
        int valor = arrastando ? pendente : get.getAsInt();
        int bw = BLOCO_W * s, bh = BLOCO_H * s, passo = (BLOCO_W + BLOCO_VAO) * s;
        int y = bounds.y + (bounds.height - bh) / 2;
        int x = trilha.x;
        for (int v = de; v <= ate; v++, x += passo) {
            g.setColor(Palette.OUTLINE);
            g.fillRect(x, y, bw, bh);
            g.setColor(v <= valor ? (aceso || arrastando ? Palette.TEXT : Palette.ACCENT) : Palette.DEEP);
            g.fillRect(x + s, y + s, bw - 2 * s, bh - 2 * s);
        }
        desenharValorADireita(g, s, fonte, baseTexto, nome.apply(valor), x - BLOCO_VAO * s);
    }

    /** O valor alinhado à direita da coluna, encolhendo para não encostar no controle. */
    private void desenharValorADireita(Graphics2D g, int s, Font fonte, int baseTexto, String texto, int fimDoControle) {
        int direita = bounds.x + bounds.width;
        int largura = direita - (fimDoControle + 4 * s);
        Font f = fonte;
        float tam = f.getSize2D();
        while (FontHandler.getWidth(texto, f) > largura && tam > 4f) {
            tam -= 1f;
            f = FontHandler.font(FontHandler.Game, tam);
        }
        g.setFont(f);
        TextFx.draw(g, texto, direita - FontHandler.getWidth(texto, f), baseTexto, Palette.TEXT, s);
        g.setFont(fonte);
    }

    private void desenharValor(Graphics2D g, Font fonte, String texto, int x, int largura, int baseTexto) {
        int s = Configs.UiScale();
        Font f = fonte;
        int w = FontHandler.getWidth(texto, f);
        float tam = f.getSize2D();
        while (w > largura && tam > 4f) {          // encolhe em vez de vazar
            tam -= 1f;
            f = FontHandler.font(FontHandler.Game, tam);
            w = FontHandler.getWidth(texto, f);
        }
        g.setFont(f);
        int tx = x + (largura - w) / 2;
        TextFx.draw(g, texto, tx, baseTexto, Palette.TEXT, s);
        g.setFont(fonte);
    }

    /** Seta do stepper. No limite da faixa ela apaga, sinalizando o fim do curso. */
    private void desenharSeta(Graphics2D g, Rectangle r, String sinal, boolean desabilitada, int s) {
        boolean sobre = !desabilitada && r.contains(Mouse.getX(), Mouse.getY());
        boolean apertada = sobre && Mouse.isPressed(Mouse_Button.LEFT, r);

        g.setColor(Palette.OUTLINE);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(desabilitada ? Palette.DEEP : (sobre ? Palette.LIGHT : Palette.MAIN));
        int d = apertada ? s : 0;
        g.fillRect(r.x + s, r.y + s + d, r.width - 2 * s, r.height - 2 * s - d);

        // glifo desenhado com retângulos: nítido em qualquer escala
        g.setColor(desabilitada ? Palette.MAIN : Palette.OUTLINE);
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2 + d;
        int braco = 3 * s;
        int esp = Math.max(1, s);
        g.fillRect(cx - braco, cy - esp / 2, braco * 2, esp);
        if (sinal.equals("+")) {
            g.fillRect(cx - esp / 2, cy - braco, esp, braco * 2);
        }
    }

    /** A seta das linhas de ação, na borda direita: "isto abre ou executa algo". */
    private void desenharChevron(Graphics2D g, boolean aceso, int s) {
        int lado = 4 * s;
        int x = bounds.x + bounds.width - lado - 2 * s + (aceso ? s : 0);
        int cy = bounds.y + bounds.height / 2;
        g.setColor(aceso ? Palette.TEXT : Palette.LIGHT);
        for (int i = 0; i < lado; i += s) {
            int altura = (lado - i) * 2 - s;
            g.fillRect(x + i, cy - altura / 2, s, Math.max(s, altura));
        }
    }

    private void desenharToggle(Graphics2D g, int s) {
        boolean ligado = get.getAsInt() != 0;
        int largura = 26 * s;
        int altura = 11 * s;
        // Alinhado ao "-" das outras linhas: a coluna de controles precisa de
        // uma única borda esquerda, senão as linhas de toggle parecem tortas.
        int x = menos.x;
        int y = bounds.y + (bounds.height - altura) / 2;

        g.setColor(Palette.OUTLINE);
        g.fillRect(x, y, largura, altura);
        g.setColor(ligado ? Palette.MAIN : Palette.DEEP);
        g.fillRect(x + s, y + s, largura - 2 * s, altura - 2 * s);

        int knob = 9 * s;
        int kx = ligado ? x + largura - knob - s : x + s;
        g.setColor(Palette.OUTLINE);
        g.fillRect(kx, y + s, knob, altura - 2 * s);
        g.setColor(ligado ? Palette.LIGHT : Palette.MAIN);
        g.fillRect(kx + s, y + 2 * s, knob - 2 * s, altura - 4 * s);
    }
}
