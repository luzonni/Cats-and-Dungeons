package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

public class Button {
    private Rectangle bounds;
    private String text;
    private boolean hovered;
    private Consumer<Button> onClick;

    public Button(int x, int y, int width, int height, String text, Consumer<Button> onClick) {
        this.bounds = new Rectangle(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
    }

    public void tick() {
        hovered = bounds.contains(Mouse.getX(), Mouse.getY());

        if (Mouse.clickOn(Mouse_Button.LEFT, bounds)) {
            if (onClick != null) onClick.accept(this);
        }
    }

    public void render(Graphics2D g) {
        int largura = bounds.width;
        int altura = bounds.height;
        int x = bounds.x;
        int y = bounds.y;

        if (hovered) {
            largura *= 1.1;
            altura *= 1.1;
            x -= (largura - bounds.width) / 2;
            y -= (altura - bounds.height) / 2;
        }

        g.setColor(new Color(0x4A5364));
        g.fillRect(x, y + altura - 5, largura, 5);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(x + 2, y + 2, largura, altura, 20, 20));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        g.setColor(new Color(0x6B7A8F));
        g.fill(new RoundRectangle2D.Double(x, y, largura, altura, 25, 25));

        g.setPaint(new GradientPaint(
                x, y, new Color(255, 255, 255, 60),
                x, y + altura / 2, new Color(255, 255, 255, 0)
        ));
        g.fill(new RoundRectangle2D.Double(x, y, largura, altura, 25, 25));

        int fontSize = 8 * Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, fontSize);
        FontMetrics fm = g.getFontMetrics(fonte);

        while (fm.stringWidth(text) > bounds.width - 20) {
            fontSize--;
            fonte = FontHandler.font(FontHandler.Game, fontSize);
            fm = g.getFontMetrics(fonte);
        }

        g.setFont(fonte);
        g.setColor(Color.WHITE);
        g.drawString(text,
                x + (largura - fm.stringWidth(text)) / 2,
                y + (altura - fm.getHeight()) / 2 + fm.getAscent());
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
