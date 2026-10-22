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
        ACTION
    }

    // Geometria em pixels nativos; tudo é multiplicado pela escala da UI.
    private static final int ROW_H = 14;
    private static final int CLUSTER_W = 92;
    private static final int BTN_W = 12;
    private static final int CONTENT_X = 14, CONTENT_W = 64;
    private static final int PLUS_X = 80;
    /**
     * Largura da trilha da barra arrastável; o número fica à direita dela. 58 e não
     * mais: o marcador aceso passa 4 da ponta, e "100%" precisa de ~24 sem encostar.
     */
    private static final int TRILHA_W = 58;
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

    /** Um teto que muda com a tela (o HUD que cabe na resolução atual), ou null. */
    private IntSupplier teto;

    /**
     * Limita o máximo a um valor calculado na hora. Encadeável. Com ele o "+" para
     * no que cabe, em vez de aceitar um valor que não teria efeito.
     */
    public OptionRow ate(IntSupplier teto) {
        this.teto = teto;
        return this;
    }

    private int maximo() {
        return teto == null ? max : Math.max(min, Math.min(max, teto.getAsInt()));
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
        if (ehBarra()) {
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
     */
    private void arrastar() {
        if (!arrastando && Mouse.clickOn(Mouse_Button.LEFT, trilha)) {
            arrastando = true;
        }
        if (!arrastando) {
            return;
        }
        if (!Mouse.pressing(Mouse_Button.LEFT)) {
            arrastando = false;
            Sound.play(Sounds.Button);      // o "assentou" do valor, uma vez, ao soltar
            return;
        }
        double frac = Math.clamp((Mouse.getX() - trilha.x) / (double) Math.max(1, trilha.width), 0, 1);
        double bruto = min + frac * (max - min);
        int v = min + (int) Math.round((bruto - min) / step) * step;
        v = Math.max(min, Math.min(maximo(), v));
        if (v != get.getAsInt()) {
            set.accept(v);
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
            novo = Math.max(min, Math.min(maximo(), atual + dir * step));
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
        return dir < 0 ? atual <= min : atual >= maximo();
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
     * A barra no estilo do Forza Horizon: trilha fina, o trecho escolhido aceso, um
     * marcador e o número alinhado à direita. Sem caixa nem botões — limpa, e o
     * tamanho dela já diz onde o valor está na faixa.
     *
     * O TETO APARECE: quando a tela não comporta o máximo da opção (ver ate), o
     * trecho além dele fica apagado, com um traço onde ele termina, para o
     * jogador entender por que o marcador para ali.
     */
    private void desenharBarra(Graphics2D g, int s, Font fonte, int baseTexto, boolean aceso) {
        int cy = bounds.y + bounds.height / 2;
        int x0 = trilha.x, w = trilha.width;
        int espessura = Math.max(1, s);
        double faixa = Math.max(1, max - min);
        int teto = maximo();
        int xTeto = x0 + (int) Math.round(w * (teto - min) / faixa);
        int xValor = x0 + (int) Math.round(w * (get.getAsInt() - min) / faixa);

        g.setColor(Palette.OUTLINE);                                   // trilha até o teto
        g.fillRect(x0, cy - espessura / 2, xTeto - x0, espessura);
        if (xTeto < x0 + w) {                                          // além do teto, apagada
            g.setColor(Palette.DEEP);
            for (int x = xTeto; x < x0 + w; x += 3 * s) {
                g.fillRect(x, cy - espessura / 2, s, espessura);
            }
            g.setColor(Palette.LIGHT);
            g.fillRect(xTeto - espessura / 2, cy - 3 * s, espessura, 6 * s);
        }
        g.setColor(aceso || arrastando ? Palette.TEXT : Palette.ACCENT);  // trecho escolhido
        g.fillRect(x0, cy - espessura / 2, Math.max(0, xValor - x0), espessura);

        // O marcador: um ponto quadrado de pixel art, maior com o foco ou no arrasto.
        int lado = (aceso || arrastando ? 7 : 5) * s;
        g.setColor(Palette.OUTLINE);
        g.fillRect(xValor - lado / 2 - s, cy - lado / 2 - s, lado + 2 * s, lado + 2 * s);
        g.setColor(aceso || arrastando ? Palette.TEXT : Palette.ACCENT);
        g.fillRect(xValor - lado / 2, cy - lado / 2, lado, lado);

        // O número, à direita da trilha, alinhado à direita da coluna.
        String texto = get.getAsInt() + sufixo;
        int direita = bounds.x + bounds.width;
        int largura = direita - (x0 + w + 4 * s);
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
