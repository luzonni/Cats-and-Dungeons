package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
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
        TOGGLE
    }

    // Geometria em pixels nativos; tudo é multiplicado pela escala da UI.
    private static final int ROW_H = 14;
    private static final int CLUSTER_W = 92;
    private static final int BTN_W = 12;
    private static final int CONTENT_X = 14, CONTENT_W = 64;
    private static final int PLUS_X = 80;
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
        return new OptionRow(label, Kind.STEPPER, get, set, min, max, step, null, "");
    }

    public static OptionRow cycle(String label, IntSupplier get, IntConsumer set, String[] rotulos) {
        return new OptionRow(label, Kind.CYCLE, get, set, 0, rotulos.length - 1, 1, rotulos, "");
    }

    public static OptionRow toggle(String label, IntSupplier get, IntConsumer set) {
        return new OptionRow(label, Kind.TOGGLE, get, set, 0, 1, 1, new String[]{"Off", "On"}, "");
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
    }

    public Rectangle getBounds() {
        return this.bounds;
    }

    public boolean estaSobreControles() {
        return menos.contains(Mouse.getX(), Mouse.getY())
                || mais.contains(Mouse.getX(), Mouse.getY())
                || (kind == Kind.TOGGLE && alvoToggle.contains(Mouse.getX(), Mouse.getY()));
    }

    public void tick() {
        if (kind == Kind.TOGGLE) {
            if (Mouse.clickOn(Mouse_Button.LEFT, alvoToggle) || Mouse.clickOn(Mouse_Button.LEFT, menos)
                    || Mouse.clickOn(Mouse_Button.LEFT, mais)) {
                ajustar(1);
            }
            return;
        }
        if (Mouse.clickOn(Mouse_Button.LEFT, menos)) {
            ajustar(-1);
        } else if (Mouse.clickOn(Mouse_Button.LEFT, mais)) {
            ajustar(1);
        }
        repetirEnquantoSegura();
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
        ajustar(dir);
    }

    private void ajustar(int dir) {
        int atual = get.getAsInt();
        int novo;
        if (kind == Kind.CYCLE || kind == Kind.TOGGLE) {
            int n = max - min + 1;
            novo = min + ((atual - min + dir) % n + n) % n;   // circular
        } else {
            novo = Math.max(min, Math.min(max, atual + dir * step));
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
        return dir < 0 ? atual <= min : atual >= max;
    }

    public void render(Graphics2D g2) {
        int s = Configs.UiScale();
        Graphics2D g = (Graphics2D) g2.create();

        if (focado) {
            // Marcador na lateral em vez de fundo: o painel já é DEEP, um
            // preenchimento na mesma cor não apareceria.
            g.setColor(Palette.LIGHT);
            g.fillRect(bounds.x - 3 * s, bounds.y + 2 * s, s, bounds.height - 4 * s);
        }

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
        g.setColor(Palette.OUTLINE);
        g.drawString(label, bounds.x + s, baseTexto + s);
        g.setColor(focado ? Palette.TEXT : Palette.LIGHT);
        g.drawString(label, bounds.x, baseTexto);

        if (kind == Kind.TOGGLE) {
            desenharToggle(g, s);
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
            String texto = (kind == Kind.CYCLE) ? rotulos[get.getAsInt()] : String.valueOf(get.getAsInt());
            desenharValor(g, fonte, texto, cx, cw, baseTexto);
        }
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
        g.setColor(Palette.OUTLINE);
        g.drawString(texto, tx + s, baseTexto + s);
        g.setColor(Palette.TEXT);
        g.drawString(texto, tx, baseTexto);
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
