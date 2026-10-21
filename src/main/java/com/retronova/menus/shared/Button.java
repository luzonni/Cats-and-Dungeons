package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.NineSlice;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Botão de menu em 9-slice, com orelhas de gato assadas no sprite.
 *
 * Quatro estados visuais (normal, hover, foco, pressionado). O feedback de
 * pressão desloca o conteúdo para baixo pela espessura do contorno, em vez de
 * escalar o botão: escala não-inteira em pixel art produz borda borrada.
 */
public class Button {

    /** Altura da faixa de orelhas no sprite, em pixels nativos. */
    private static final int EAR_H = 6;
    /** Dimensões nativas de uma célula do sprite. */
    private static final int CELL_W = 24, CELL_H = 22;

    /** Molduras ja carregadas, por nome de sprite. */
    private static final Map<String, NineSlice> MOLDURAS = new HashMap<>();

    /** Moldura 9-slice compartilhada: os cartoes de personagem reusam a mesma. */
    public static NineSlice frame(boolean primary) {
        String nome = primary ? "button" : "button_dark";
        return MOLDURAS.computeIfAbsent(nome, n -> new NineSlice(n, CELL_W, 8, 8, 12, 6));
    }

    /** Altura recomendada para que os cantos caiam em escala inteira. */
    public static int preferredHeight() {
        return CELL_H * Configs.UiScale();
    }

    /** Largura recomendada para um botão de menu. */
    public static int preferredWidth() {
        return 78 * Configs.UiScale();
    }

    private Rectangle bounds;
    private final String text;
    private final Consumer<Button> onClick;

    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean primary;
    /** Som proprio ao acionar. Nulo usa o clique padrao. */
    private Sounds vozDoClique;
    private boolean hoveredBefore;
    /** O realce de hover e de foco, no estilo único do jogo. Ver Realce. */
    private final Realce.Estado realce = new Realce.Estado();

    public Button(int x, int y, int width, int height, String text, Consumer<Button> onClick) {
        this.bounds = new Rectangle(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
    }

    /** Marca o botão como ação principal da tela. Encadeável. */
    public Button primary() {
        this.primary = true;
        return this;
    }

    /**
     * Troca o som de acionamento por uma voz. Reservado a um botão por tela.
     *
     * Recebe qual voz em vez de assumir uma: na seleção de personagem o botão de
     * embarcar mia com o gato ESCOLHIDO, e isso muda a cada clique na fileira.
     */
    public Button meow(Sounds voz) {
        this.vozDoClique = voz;
        return this;
    }

    public void tick() {
        hovered = Entrada.porMouse() && bounds.contains(Mouse.getX(), Mouse.getY());
        if (hovered) {
            // A PATINHA TAMBEM NOS MENUS.
            //
            // O cursor de mao so era pedido pelas interfaces de dentro do jogo —
            // inventario, loja, vendedor. Nos menus, que sao feitos inteiros de
            // botoes, ele nunca aparecia: a unica parte do jogo em que passar o
            // mouse por cima de algo clicavel nao dizia nada.
            Engine.window.pointing();
        }
        pressed = Mouse.isPressed(Mouse_Button.LEFT, bounds);

        if (hovered && !hoveredBefore) {
            Sound.play(Sounds.Hover);        // tique discreto ao passar por cima
        }
        hoveredBefore = hovered;

        if (Mouse.clickOn(Mouse_Button.LEFT, bounds)) {
            activate();
        }

        realce.tick(hovered || focused);
    }

    /** Dispara a ação. Separado do clique para servir também ao teclado. */
    public void activate() {
        Sound.play(vozDoClique != null ? vozDoClique : Sounds.Button);
        if (onClick != null) {
            onClick.accept(this);
        }
    }

    public void render(Graphics2D g2) {
        int scale = Configs.UiScale();
        int state = pressed ? 2 : (hovered || focused ? 1 : 0);
        float intensidade = realce.noQuadro();
        Rectangle r = Realce.crescer(bounds, intensidade, scale);

        Realce.brilho(g2, corpo(r, scale), intensidade, scale);
        frame(primary).draw(g2, state, r.x, r.y, r.width, r.height, scale);

        if (focused && !pressed) {
            Realce.marcadorDeFoco(g2, corpo(r, scale), scale);
        }
        desenharTexto(g2, r, scale, state == 2);
    }

    /** O corpo do botão, sem a faixa de orelhas: é ele que brilha e que o foco aponta. */
    private static Rectangle corpo(Rectangle r, int scale) {
        int topo = r.y + (EAR_H - 1) * scale;
        return new Rectangle(r.x, topo, r.width, r.height - (EAR_H - 1) * scale);
    }


    private void desenharTexto(Graphics2D g2, Rectangle bounds, int scale, boolean afundado) {
        Graphics2D g = (Graphics2D) g2.create();

        // O texto vive no corpo, abaixo da faixa de orelhas.
        int corpoY = bounds.y + EAR_H * scale;
        int corpoH = bounds.height - EAR_H * scale;

        int tamanho = 8 * scale;
        Font fonte = FontHandler.font(FontHandler.Game, tamanho);
        int largura = FontHandler.getWidth(text, fonte);
        int limite = bounds.width - 8 * scale;
        while (largura > limite && tamanho > 4) {
            tamanho--;
            fonte = FontHandler.font(FontHandler.Game, tamanho);
            largura = FontHandler.getWidth(text, fonte);
        }
        int altura = FontHandler.getHeight(text, fonte);

        int x = bounds.x + (bounds.width - largura) / 2;
        int y = corpoY + (corpoH + altura) / 2 - scale;
        if (afundado) {
            y += scale;
        }

        TextFx.draw(g, text, fonte, x, y, Palette.TEXT, scale);
        g.dispose();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
