package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.NineSlice;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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

    private static NineSlice framePrimary;
    private static NineSlice frameSecondary;

    /** Moldura 9-slice compartilhada: os cartoes de personagem reusam a mesma. */
    public static NineSlice frame(boolean primary) {
        if (framePrimary == null) {
            framePrimary = new NineSlice("button", CELL_W, 8, 8, 12, 6);
            frameSecondary = new NineSlice("button_dark", CELL_W, 8, 8, 12, 6);
        }
        return primary ? framePrimary : frameSecondary;
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
    private boolean meow;
    private boolean hoveredBefore;

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

    /** Faz o botão miar ao ser acionado. Reservado a um botão por tela. */
    public Button meow() {
        this.meow = true;
        return this;
    }

    public void tick() {
        hovered = bounds.contains(Mouse.getX(), Mouse.getY());
        pressed = Mouse.isPressed(Mouse_Button.LEFT, bounds);

        if (hovered && !hoveredBefore) {
            Sound.play(Sounds.Button);       // blip discreto ao passar por cima
        }
        hoveredBefore = hovered;

        if (Mouse.clickOn(Mouse_Button.LEFT, bounds)) {
            activate();
        }
    }

    /** Dispara a ação. Separado do clique para servir também ao teclado. */
    public void activate() {
        Sound.play(meow ? Sounds.Cat : Sounds.Button);
        if (onClick != null) {
            onClick.accept(this);
        }
    }

    public void render(Graphics2D g2) {
        int scale = Configs.UiScale();
        int state = pressed ? 2 : (hovered || focused ? 1 : 0);

        frame(primary).draw(g2, state, bounds.x, bounds.y, bounds.width, bounds.height, scale);

        if (focused && !pressed) {
            desenharAnelDeFoco(g2, scale);
        }
        desenharTexto(g2, scale, state == 2);
    }

    /** Contorno externo indicando o foco de teclado. */
    private void desenharAnelDeFoco(Graphics2D g2, int scale) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setColor(Palette.LIGHT);
        int topo = bounds.y + (EAR_H - 1) * scale;
        int altura = bounds.height - (EAR_H - 1) * scale;
        for (int i = 1; i <= scale; i++) {
            g.drawRect(bounds.x - i, topo - i, bounds.width + i * 2 - 1, altura + i * 2 - 1);
        }
        g.dispose();
    }

    private void desenharTexto(Graphics2D g2, int scale, boolean afundado) {
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

        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(text, x, y + scale);   // sombra dura, no estilo do resto da UI
        g.setColor(Palette.TEXT);
        g.drawString(text, x, y);
        g.dispose();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
