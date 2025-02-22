package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class UI implements Activity {

    private int padding;
    private Player player;

    UI(Player player) {
        this.padding = Engine.UISCALE * 3;
        this.player = player;
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {
        drawPlayerLife(g);
    }
    //TODO Apenas para teste... melhorar isso!
    private void drawPlayerLife(Graphics2D g) {
        g.setStroke(new BasicStroke(Engine.UISCALE));
        int width = (int)player.getLifeSize();
        int height = 5 * Engine.UISCALE;
        int x = Engine.window.getWidth() - padding - width;
        int y = padding;
        g.setColor(Color.red);
        g.fillRect(x, y, (int)player.getLife(), height);
        g.setColor(Color.black);
        g.drawRect(x, y, width, height);


        g.setStroke(new BasicStroke(1));
    }

    @Override
    public void dispose() {

    }
}
