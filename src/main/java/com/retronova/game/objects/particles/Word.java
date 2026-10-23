package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.TextFx;
import com.retronova.game.Game;

import java.awt.*;

/**
 * Uma palavra que sobe no mundo: "Dodge", "FURY!", o nome da arma transmutada,
 * os Zs do gato dormindo.
 *
 * NO TEXTO DA CASA (TextFx): a fonte do jogo com o contorno e a sombra da paleta.
 * Era a Septem em drawString com uma sombra preta crua, a única palavra da tela
 * fora do desenho do resto. Números de dano e cura não passam aqui: ver
 * NumeroDeDano, que desenha cada algarismo uma vez só.
 */
public class Word extends Particle {

    private final String value;
    private final Font font;
    private final Rectangle boundsFont;
    private final Color cor;
    private int count;

    public Word(String value, double x, double y, double seconds) {
        this(value, x, y, seconds, TextFx.CLARO);
    }

    /** Com cor: o nome da arma nova na cor do elemento, por exemplo (ver Transmutacao). */
    public Word(String value, double x, double y, double seconds, Color cor) {
        super(x, y, seconds);
        this.value = value;
        this.cor = cor;
        this.font = FontHandler.font(FontHandler.Game, Configs.GameScale() * 6);
        int wF = FontHandler.getWidth(value, font);
        int hF = FontHandler.getHeight(value, font);
        boundsFont = new Rectangle(wF, hF);
        setX(getX() - boundsFont.width/2d);
        setY(getY() - boundsFont.height/2d);
        double scaleW = (double) boundsFont.width / getWidth();
        double scaleH = (double) boundsFont.height / getHeight();
        setWidth(scaleW);
        setHeight(scaleH);
    }

    @Override
    public void tick() {
        count++;
        if (count >= getSeconds()) {
            Game.getMap().remove(this);
        }
        this.setY(getY() - 0.25d * Configs.GameScale());
    }

    @Override
    public void render(Graphics2D g) {
        TextFx.draw(g, value, font, (int) getX(), (int) getY() + boundsFont.height, cor, Configs.GameScale());
    }
}
